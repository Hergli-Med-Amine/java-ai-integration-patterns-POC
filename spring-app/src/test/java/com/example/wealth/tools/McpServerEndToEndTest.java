package com.example.wealth.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wealth.FakeOllama;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "wealth.mcp.client-id=C-1003")
@ExtendWith(OutputCaptureExtension.class)
class McpServerEndToEndTest {

    static final FakeOllama ollama = FakeOllama.start();

    @DynamicPropertySource
    static void ollamaUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", ollama::url);
    }

    @AfterAll
    static void stopOllama() {
        ollama.stop();
    }

    @LocalServerPort
    int port;

    McpSyncClient client;

    @BeforeEach
    void connect() {
        client = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port).build()).build();
        client.initialize();
    }

    @AfterEach
    void disconnect() {
        client.closeGracefully();
    }

    @Test
    void exposesThePortfolioToolsWithoutAClientParameter() {
        assertThat(client.listTools().tools())
                .extracting(Tool::name)
                .containsExactlyInAnyOrder("portfolioSummary", "holdings", "exposure", "policyCheck");
        assertThat(client.listTools().tools())
                .allSatisfy(tool -> assertThat(tool.inputSchema().toString()).doesNotContainIgnoringCase("client"));
    }

    @Test
    void answersForTheConfiguredClient(CapturedOutput output) {
        CallToolResult result = client.callTool(new CallToolRequest("portfolioSummary", Map.of()));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(((TextContent) result.content().getFirst()).text()).contains("Sofia Lindqvist");
        assertThat(output).contains("tool_call client=C-1003 tool=portfolioSummary").contains("outcome=success");
    }

    @Test
    void rejectsAClientIdArgumentBeforeTheToolRuns(CapturedOutput output) {
        CallToolResult result = client.callTool(new CallToolRequest("portfolioSummary", Map.of("clientId", "C-1002")));

        assertThat(result.isError()).isTrue();
        assertThat(output).doesNotContain("tool=portfolioSummary");
    }
}
