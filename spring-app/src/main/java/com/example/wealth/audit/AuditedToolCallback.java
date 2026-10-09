package com.example.wealth.audit;

import com.example.wealth.tools.PortfolioTools;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

public final class AuditedToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger("audit");

    private final ToolCallback delegate;

    private AuditedToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    public static ToolCallback[] audited(ToolCallback... callbacks) {
        return Arrays.stream(callbacks).map(AuditedToolCallback::new).toArray(ToolCallback[]::new);
    }

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
        Object clientId = toolContext == null ? null : toolContext.getContext().get(PortfolioTools.CLIENT_ID);
        String tool = getToolDefinition().name();
        try {
            String result = delegate.call(toolInput, toolContext);
            log.info("tool_call client={} tool={} arguments={} outcome=success", clientId, tool, toolInput);
            return result;
        } catch (RuntimeException e) {
            // Rethrown so Spring AI's tool loop handles the error as usual; caught only to audit the failure.
            log.warn("tool_call client={} tool={} arguments={} outcome=error error={}",
                    clientId, tool, toolInput, e.getClass().getSimpleName());
            throw e;
        }
    }
}
