package com.example.wealth.tools;

import com.example.wealth.portfolio.Client;
import com.example.wealth.portfolio.Client.RiskProfile;
import com.example.wealth.portfolio.Exposure;
import com.example.wealth.portfolio.Holding;
import com.example.wealth.portfolio.PortfolioRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class PortfolioTools {

    public static final String CLIENT_ID = "clientId";

    private final PortfolioRepository repository;

    public PortfolioTools(PortfolioRepository repository) {
        this.repository = repository;
    }

    public record PortfolioSummary(String name, RiskProfile riskProfile, BigDecimal portfolioValue) {}

    @Tool(description = "Name, risk profile and total portfolio value in EUR of the signed-in client.")
    public PortfolioSummary portfolioSummary(ToolContext context) {
        Client client = currentClient(context);
        return new PortfolioSummary(client.name(), client.riskProfile(), client.portfolioValue());
    }

    @Tool(description = "All holdings of the signed-in client with asset class, sector, region, quantity, price and market value in EUR.")
    public List<Holding> holdings(ToolContext context) {
        return currentClient(context).holdings();
    }

    @Tool(description = "Exposure of the signed-in client's portfolio by one dimension: market value in EUR and percent of portfolio value per group, largest first.")
    public Exposure exposure(@ToolParam(description = "SECTOR, REGION or ASSET_CLASS") Exposure.Dimension dimension,
                             ToolContext context) {
        return currentClient(context).exposureBy(dimension);
    }

    // The client id is put into the tool context by the application, never by the model (ADR 0001).
    private Client currentClient(ToolContext context) {
        if (context == null || !(context.getContext().get(CLIENT_ID) instanceof String clientId)) {
            throw new IllegalStateException("No current client in tool context");
        }
        return repository.find(clientId);
    }
}
