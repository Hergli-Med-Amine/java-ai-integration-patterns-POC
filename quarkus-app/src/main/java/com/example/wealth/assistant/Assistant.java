package com.example.wealth.assistant;

import com.example.wealth.tools.PortfolioTools;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(tools = PortfolioTools.class, retrievalAugmentor = PolicyDocuments.class)
interface Assistant {

    @SystemMessage("""
            You are a wealth-management assistant for one signed-in client of a bank.
            Answer only about this client's own portfolio and the bank's policy documents.
            Use the tools for every figure about the portfolio; do not calculate or estimate figures yourself.
            To say whether the portfolio is within the investment policy, use the policyCheck tool;
            do not compare figures with limits yourself.
            For questions about policies, fees or risks, use only the document excerpts provided with the question,
            and name the document you rely on.
            The tools always return the signed-in client's data. You cannot access any other client;
            if asked to, say so.
            All amounts are in EUR. Be brief and factual. Do not give personal investment advice.
            """)
    String answer(@UserMessage String question, InvocationParameters parameters);
}
