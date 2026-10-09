package com.example.wealth.tools;

import com.example.wealth.audit.ToolCallAudit.Audited;
import com.example.wealth.portfolio.Client;
import com.example.wealth.portfolio.Client.RiskProfile;
import com.example.wealth.portfolio.Exposure;
import com.example.wealth.portfolio.Holding;
import com.example.wealth.portfolio.PolicyCheck;
import com.example.wealth.portfolio.PortfolioRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
@Audited
public class PortfolioTools {

    public static final String CLIENT_ID = "clientId";

    static final String PORTFOLIO_SUMMARY = "Name, risk profile and total portfolio value in EUR of the current client.";
    static final String HOLDINGS = "All holdings of the current client with asset class, sector, region, quantity, price and market value in EUR.";
    static final String EXPOSURE = "Exposure of the current client's portfolio by one dimension: market value in EUR and percent of portfolio value per group, largest first.";
    static final String DIMENSION = "SECTOR, REGION or ASSET_CLASS";
    static final String POLICY_CHECK = "Checks the current client's portfolio against the investment policy limits: the asset-class ranges of the client's risk profile and the crypto-asset rules. For each limit: current percent, allowed range, status (WITHIN, ABOVE_MAX, BELOW_MIN, NOT_PERMITTED) and the deviation in percentage points and EUR. Also lists the limits it does not check. Use it for any question about whether the portfolio is within policy.";

    private final PortfolioRepository repository;

    public PortfolioTools(PortfolioRepository repository) {
        this.repository = repository;
    }

    public record PortfolioSummary(String name, RiskProfile riskProfile, BigDecimal portfolioValue) {}

    @Tool(PORTFOLIO_SUMMARY)
    public PortfolioSummary portfolioSummary(InvocationParameters parameters) {
        Client client = currentClient(parameters);
        return new PortfolioSummary(client.name(), client.riskProfile(), client.portfolioValue());
    }

    @Tool(HOLDINGS)
    public List<Holding> holdings(InvocationParameters parameters) {
        return currentClient(parameters).holdings();
    }

    @Tool(EXPOSURE)
    public Exposure exposure(@P(DIMENSION) Exposure.Dimension dimension, InvocationParameters parameters) {
        return currentClient(parameters).exposureBy(dimension);
    }

    @Tool(POLICY_CHECK)
    public PolicyCheck policyCheck(InvocationParameters parameters) {
        return currentClient(parameters).policyCheck();
    }

    // The client id is put into the invocation parameters by the application, never by the model (ADR 0001).
    private Client currentClient(InvocationParameters parameters) {
        if (parameters == null || !(parameters.get(CLIENT_ID) instanceof String clientId)) {
            throw new IllegalStateException("No current client in invocation parameters");
        }
        return repository.find(clientId);
    }
}
