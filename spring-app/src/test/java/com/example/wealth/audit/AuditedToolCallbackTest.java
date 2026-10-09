package com.example.wealth.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wealth.portfolio.PortfolioRepository;
import com.example.wealth.tools.PortfolioTools;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class AuditedToolCallbackTest {

    private final ToolCallback exposure = Arrays.stream(AuditedToolCallback.audited(ToolCallbacks.from(
                    new PortfolioTools(new PortfolioRepository(JsonMapper.builder().build(), "../data")))))
            .filter(callback -> callback.getToolDefinition().name().equals("exposure"))
            .findFirst()
            .orElseThrow();

    @Test
    void recordsSuccessfulToolCall(CapturedOutput output) {
        exposure.call("{\"dimension\":\"SECTOR\"}", new ToolContext(Map.of(PortfolioTools.CLIENT_ID, "C-1001")));

        assertThat(output).contains(
                "tool_call client=C-1001 tool=exposure arguments={\"dimension\":\"SECTOR\"} outcome=success");
    }

    @Test
    void recordsFailedToolCall(CapturedOutput output) {
        assertThatThrownBy(() -> exposure.call("{\"dimension\":\"SECTOR\"}",
                new ToolContext(Map.of(PortfolioTools.CLIENT_ID, "C-9999"))))
                .isInstanceOf(RuntimeException.class);

        assertThat(output).contains("tool_call client=C-9999 tool=exposure").contains("outcome=error");
    }
}
