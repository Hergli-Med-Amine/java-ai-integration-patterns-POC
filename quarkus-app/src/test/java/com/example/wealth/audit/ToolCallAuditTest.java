package com.example.wealth.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wealth.AuditLog;
import com.example.wealth.FakeOllama;
import com.example.wealth.portfolio.Exposure;
import com.example.wealth.tools.PortfolioTools;
import dev.langchain4j.invocation.InvocationParameters;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(FakeOllama.Resource.class)
class ToolCallAuditTest {

    @Inject
    PortfolioTools tools;

    @BeforeEach
    void clearAuditLog() {
        AuditLog.clear();
    }

    @Test
    void recordsSuccessfulToolCall() {
        tools.exposure(Exposure.Dimension.SECTOR, new InvocationParameters(Map.of(PortfolioTools.CLIENT_ID, "C-1001")));

        assertThat(AuditLog.lines())
                .contains("tool_call client=C-1001 tool=exposure arguments={\"dimension\":\"SECTOR\"} outcome=success");
    }

    @Test
    void recordsFailedToolCall() {
        assertThatThrownBy(() -> tools.exposure(Exposure.Dimension.SECTOR,
                new InvocationParameters(Map.of(PortfolioTools.CLIENT_ID, "C-9999"))))
                .isInstanceOf(RuntimeException.class);

        assertThat(AuditLog.lines()).anySatisfy(line -> assertThat(line)
                .startsWith("tool_call client=C-9999 tool=exposure").contains("outcome=error"));
    }
}
