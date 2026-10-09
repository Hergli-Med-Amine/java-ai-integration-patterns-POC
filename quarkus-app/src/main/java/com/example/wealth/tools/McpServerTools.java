package com.example.wealth.tools;

import com.example.wealth.portfolio.Exposure;
import com.example.wealth.portfolio.Holding;
import com.example.wealth.portfolio.PolicyCheck;
import com.example.wealth.portfolio.PortfolioRepository;
import dev.langchain4j.invocation.InvocationParameters;
import io.quarkiverse.mcp.server.FilterContext;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.Tool.Annotations;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolFilter;
import io.quarkiverse.mcp.server.ToolManager.ToolInfo;
import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// MCP requests carry no user identity in this PoC, so the MCP server acts for one configured client (ADR 0007).
// Without that setting the filter hides every tool: serving a client's data over MCP must be a deliberate choice.
// The MCP extension has its own @Tool annotation, so each tool is declared here again and delegates to PortfolioTools,
// which keeps the logic, the guardrail and the audit in one place.
@Singleton
@Startup
public class McpServerTools implements ToolFilter {

    private final PortfolioTools tools;
    private final Optional<String> clientId;

    McpServerTools(PortfolioTools tools, PortfolioRepository repository,
                   @ConfigProperty(name = "wealth.mcp.client-id") Optional<String> clientId) {
        this.tools = tools;
        this.clientId = clientId;
        // Fail at startup, not on the first tool call, if the configured client does not exist.
        clientId.ifPresent(repository::find);
    }

    @Override
    public boolean test(ToolInfo tool, FilterContext context) {
        return clientId.isPresent();
    }

    @Tool(description = PortfolioTools.PORTFOLIO_SUMMARY, annotations = @Annotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    PortfolioTools.PortfolioSummary portfolioSummary() {
        return tools.portfolioSummary(parameters());
    }

    @Tool(description = PortfolioTools.HOLDINGS, annotations = @Annotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    List<Holding> holdings() {
        return tools.holdings(parameters());
    }

    @Tool(description = PortfolioTools.EXPOSURE, annotations = @Annotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    Exposure exposure(@ToolArg(description = PortfolioTools.DIMENSION) Exposure.Dimension dimension) {
        return tools.exposure(dimension, parameters());
    }

    @Tool(description = PortfolioTools.POLICY_CHECK, annotations = @Annotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    PolicyCheck policyCheck() {
        return tools.policyCheck(parameters());
    }

    private InvocationParameters parameters() {
        return new InvocationParameters(Map.of(PortfolioTools.CLIENT_ID,
                clientId.orElseThrow(() -> new IllegalStateException("No MCP client configured"))));
    }
}
