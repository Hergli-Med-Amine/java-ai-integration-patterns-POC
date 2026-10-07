package com.example.wealth.assistant;

import static com.example.wealth.audit.AuditedToolCallback.audited;

import com.example.wealth.portfolio.PortfolioRepository;
import com.example.wealth.portfolio.PortfolioRepository.UnknownClientException;
import com.example.wealth.tools.PortfolioTools;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AssistantController {

    static final String SYSTEM_PROMPT = """
            You are a wealth-management assistant for one signed-in client of a bank.
            Answer only about this client's own portfolio and the bank's policy documents.
            Use the tools for every figure about the portfolio; do not calculate or estimate figures yourself.
            For questions about policies, fees or risks, use only the document excerpts provided with the question,
            and name the document you rely on.
            The tools always return the signed-in client's data. You cannot access any other client;
            if asked to, say so.
            All amounts are in EUR. Be brief and factual. Do not give personal investment advice.
            """;

    private final ChatClient chatClient;
    private final PortfolioRepository repository;

    AssistantController(ChatClient.Builder chatClientBuilder, PortfolioTools tools, VectorStore policyDocumentStore,
                        PortfolioRepository repository) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(audited(ToolCallbacks.from(tools)))
                .defaultAdvisors(QuestionAnswerAdvisor.builder(policyDocumentStore)
                        .searchRequest(SearchRequest.builder().topK(3).build())
                        .build())
                .build();
        this.repository = repository;
    }

    record Question(String question) {}

    record Answer(String answer) {}

    @PostMapping("/assistant")
    Answer ask(@RequestHeader("X-Client-Id") String clientId, @RequestBody Question question) {
        // Reject unknown clients before spending a model call.
        repository.find(clientId);
        String answer = chatClient.prompt()
                .user(question.question())
                .toolContext(Map.of(PortfolioTools.CLIENT_ID, clientId))
                .call()
                .content();
        return new Answer(answer);
    }

    @ExceptionHandler(UnknownClientException.class)
    ProblemDetail unknownClient(UnknownClientException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }
}
