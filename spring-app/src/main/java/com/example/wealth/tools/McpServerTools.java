package com.example.wealth.tools;

import static com.example.wealth.audit.AuditedToolCallback.audited;

import com.example.wealth.portfolio.PortfolioRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// MCP requests carry no user identity in this PoC, so the MCP server acts for one configured client (ADR 0007).
// Without that setting no tools are registered: serving a client's data over MCP must be a deliberate choice.
@Configuration
@ConditionalOnProperty("wealth.mcp.client-id")
class McpServerTools {

    @Bean
    ToolCallbackProvider mcpPortfolioTools(PortfolioTools tools, PortfolioRepository repository,
                                           @Value("${wealth.mcp.client-id}") String clientId) {
        // Fail at startup, not on the first tool call, if the configured client does not exist.
        repository.find(clientId);
        return ToolCallbackProvider.from(Arrays.stream(audited(ToolCallbacks.from(tools)))
                .map(callback -> new BoundToClient(callback, clientId))
                .toArray(ToolCallback[]::new));
    }

    private record BoundToClient(ToolCallback delegate, String clientId) implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            Map<String, Object> context = new HashMap<>(toolContext == null ? Map.of() : toolContext.getContext());
            context.put(PortfolioTools.CLIENT_ID, clientId);
            return delegate.call(toolInput, new ToolContext(context));
        }
    }
}
