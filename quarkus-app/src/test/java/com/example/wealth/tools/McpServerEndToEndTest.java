package com.example.wealth.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wealth.AuditLog;
import com.example.wealth.FakeOllama;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(McpServerEndToEndTest.ClientC1003.class)
@WithTestResource(FakeOllama.Resource.class)
class McpServerEndToEndTest {

    public static class ClientC1003 implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("wealth.mcp.client-id", "C-1003");
        }
    }

    @TestHTTPResource("/")
    URI baseUri;

    McpSyncClient client;

    @BeforeEach
    void connect() {
        AuditLog.clear();
        client = McpClient.sync(HttpClientStreamableHttpTransport.builder(baseUri.toString()).build()).build();
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
        assertThat(client.listTools().tools()).allSatisfy(tool -> assertThat(tool.annotations().readOnlyHint()).isTrue());
    }

    @Test
    void answersForTheConfiguredClient() {
        CallToolResult result = client.callTool(new CallToolRequest("portfolioSummary", Map.of()));

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(((TextContent) result.content().getFirst()).text()).contains("Sofia Lindqvist");
        assertThat(AuditLog.lines()).anySatisfy(line -> assertThat(line)
                .startsWith("tool_call client=C-1003 tool=portfolioSummary").contains("outcome=success"));
    }

    @Test
    void aClientIdArgumentCannotChangeTheClient() {
        CallToolResult result = client.callTool(new CallToolRequest("portfolioSummary", Map.of("clientId", "C-1002")));

        // Unlike Spring AI, the Quarkus MCP server does not reject undeclared arguments; it ignores them.
        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(((TextContent) result.content().getFirst()).text()).contains("Sofia Lindqvist");
        assertThat(AuditLog.lines()).noneSatisfy(line -> assertThat(line).contains("client=C-1002"));
    }
}
