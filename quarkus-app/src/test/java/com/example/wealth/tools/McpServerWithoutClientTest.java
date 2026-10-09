package com.example.wealth.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wealth.FakeOllama;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URI;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(FakeOllama.Resource.class)
class McpServerWithoutClientTest {

    @TestHTTPResource("/")
    URI baseUri;

    @Test
    void exposesNoToolsUnlessAClientIsConfigured() {
        McpSyncClient client = McpClient.sync(HttpClientStreamableHttpTransport.builder(baseUri.toString()).build()).build();
        InitializeResult server = client.initialize();

        assertThat(server.capabilities().tools()).as("tools capability").isNull();

        client.closeGracefully();
    }
}
