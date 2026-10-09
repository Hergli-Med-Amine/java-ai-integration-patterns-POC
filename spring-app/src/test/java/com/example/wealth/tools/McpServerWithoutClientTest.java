package com.example.wealth.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wealth.FakeOllama;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerWithoutClientTest {

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

    @Test
    void exposesNoToolsUnlessAClientIsConfigured() {
        McpSyncClient client =
                McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port).build()).build();
        client.initialize();

        assertThat(client.listTools().tools()).isEmpty();

        client.closeGracefully();
    }
}
