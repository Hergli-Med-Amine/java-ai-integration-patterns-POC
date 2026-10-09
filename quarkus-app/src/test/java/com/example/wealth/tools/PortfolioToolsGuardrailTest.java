package com.example.wealth.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wealth.portfolio.Exposure;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.wealth.portfolio.PortfolioRepository;
import dev.langchain4j.invocation.InvocationParameters;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

// The schema the model sees is checked end to end in AssistantEndToEndTest, from the request Quarkus sends to Ollama.
class PortfolioToolsGuardrailTest {

    private final PortfolioTools tools;

    PortfolioToolsGuardrailTest() throws IOException {
        tools = new PortfolioTools(new PortfolioRepository(new ObjectMapper(), "../data"));
    }

    @Test
    void toolsAnswerForTheClientInTheInvocationParameters() {
        Exposure exposure = tools.exposure(Exposure.Dimension.ASSET_CLASS,
                new InvocationParameters(Map.of(PortfolioTools.CLIENT_ID, "C-1003")));

        assertThat(exposure.lines().getFirst().key()).isEqualTo("BOND");
    }

    @Test
    void toolsRefuseToRunWithoutACurrentClient() {
        assertThatThrownBy(() -> tools.holdings(new InvocationParameters()))
                .isInstanceOf(IllegalStateException.class);
    }
}
