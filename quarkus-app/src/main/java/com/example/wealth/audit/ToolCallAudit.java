package com.example.wealth.audit;

import com.example.wealth.tools.PortfolioTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.invocation.InvocationParameters;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jboss.logging.Logger;

// Intercepts every method of the tools bean, so calls from the assistant and from MCP are audited alike.
@ToolCallAudit.Audited
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class ToolCallAudit {

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Audited {}

    private static final Logger log = Logger.getLogger("audit");

    private final ObjectMapper objectMapper;

    public ToolCallAudit(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @AroundInvoke
    Object audit(InvocationContext invocation) throws Exception {
        Object clientId = null;
        Map<String, Object> arguments = new LinkedHashMap<>();
        Parameter[] parameters = invocation.getMethod().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Object value = invocation.getParameters()[i];
            if (value instanceof InvocationParameters invocationParameters) {
                clientId = invocationParameters.get(PortfolioTools.CLIENT_ID);
            } else {
                arguments.put(parameters[i].getName(), value);
            }
        }
        String tool = invocation.getMethod().getName();
        String argumentsJson = objectMapper.writeValueAsString(arguments);
        try {
            Object result = invocation.proceed();
            log.infof("tool_call client=%s tool=%s arguments=%s outcome=success", clientId, tool, argumentsJson);
            return result;
        } catch (Exception e) {
            // Rethrown so the caller handles the error as usual; caught only to audit the failure.
            log.warnf("tool_call client=%s tool=%s arguments=%s outcome=error error=%s",
                    clientId, tool, argumentsJson, e.getClass().getSimpleName());
            throw e;
        }
    }
}
