package com.example.wealth.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wealth.portfolio.Exposure;
import com.example.wealth.portfolio.PortfolioRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.json.JsonMapper;

class PortfolioToolsGuardrailTest {

    private final PortfolioTools tools =
            new PortfolioTools(new PortfolioRepository(JsonMapper.builder().build(), "../data"));

    @Test
    void noToolLetsTheModelChooseTheClient() {
        ToolCallback[] callbacks = ToolCallbacks.from(tools);

        assertThat(callbacks).isNotEmpty();
        for (ToolCallback callback : callbacks) {
            assertThat(callback.getToolDefinition().inputSchema())
                    .as("input schema the model sees for tool '%s'", callback.getToolDefinition().name())
                    .doesNotContainIgnoringCase("client");
        }
    }

    @Test
    void toolsAnswerForTheClientInTheToolContext() {
        Exposure exposure = tools.exposure(Exposure.Dimension.ASSET_CLASS,
                new ToolContext(Map.of(PortfolioTools.CLIENT_ID, "C-1003")));

        assertThat(exposure.lines().getFirst().key()).isEqualTo("BOND");
    }

    @Test
    void toolsRefuseToRunWithoutACurrentClient() {
        assertThatThrownBy(() -> tools.holdings(new ToolContext(Map.of())))
                .isInstanceOf(IllegalStateException.class);
    }
}
