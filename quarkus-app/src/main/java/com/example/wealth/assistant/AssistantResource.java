package com.example.wealth.assistant;

import com.example.wealth.portfolio.PortfolioRepository;
import com.example.wealth.portfolio.PortfolioRepository.UnknownClientException;
import com.example.wealth.tools.PortfolioTools;
import dev.langchain4j.invocation.InvocationParameters;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Path("/assistant")
public class AssistantResource {

    private final Assistant assistant;
    private final PortfolioRepository repository;

    AssistantResource(Assistant assistant, PortfolioRepository repository) {
        this.assistant = assistant;
        this.repository = repository;
    }

    public record Question(@Schema(examples = "Is my crypto exposure within policy?") String question) {}

    public record Answer(String answer) {}

    @POST
    @Operation(summary = "Ask the assistant a question about the signed-in client's portfolio or the policy documents")
    public Answer ask(@Parameter(description = "Stands in for the authenticated client (ADR 0001)", example = "C-1002")
                      @HeaderParam("X-Client-Id") String clientId,
                      Question question) {
        if (clientId == null) {
            throw new BadRequestException("Missing X-Client-Id header");
        }
        // Reject unknown clients before spending a model call.
        repository.find(clientId);
        return new Answer(assistant.answer(question.question(),
                new InvocationParameters(Map.of(PortfolioTools.CLIENT_ID, clientId))));
    }

    @ServerExceptionMapper
    Response unknownClient(UnknownClientException e) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("status", 403, "title", "Forbidden", "detail", e.getMessage()))
                .build();
    }
}
