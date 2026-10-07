package com.example.wealth.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AssistantEndToEndTest {

    // Stands in for Ollama at the HTTP level, so the real Spring AI client and tool-calling loop run.
    static final FakeOllama ollama = FakeOllama.start();

    @DynamicPropertySource
    static void ollamaUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", ollama::url);
    }

    @AfterAll
    static void stopOllama() {
        ollama.stop();
    }

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void resetOllama() {
        ollama.reset();
    }

    @Test
    void answersWithToolResultForTheClientInTheHeader(CapturedOutput output) throws Exception {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"",
                 "tool_calls":[{"function":{"name":"exposure","arguments":{"dimension":"SECTOR"}}}]},
                 "done":true,"done_reason":"stop"}""");
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"69.05% of your portfolio is in technology."},
                 "done":true,"done_reason":"stop"}""");

        mockMvc.perform(post("/assistant")
                        .header("X-Client-Id", "C-1002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What's my exposure to tech?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("69.05% of your portfolio is in technology."));

        assertThat(ollama.requests()).hasSize(2);
        assertThat(ollama.requests().get(1)).contains("TECHNOLOGY").contains("69.05");
        assertThat(output).contains("tool_call client=C-1002 tool=exposure").contains("outcome=success");
    }

    @Test
    void rejectsUnknownClientWithoutCallingTheModel() throws Exception {
        mockMvc.perform(post("/assistant")
                        .header("X-Client-Id", "C-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What's my exposure to tech?\"}"))
                .andExpect(status().isForbidden());

        assertThat(ollama.requests()).isEmpty();
    }

    record FakeOllama(HttpServer server, Queue<String> responses, List<String> requests) {

        static FakeOllama start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                FakeOllama fake = new FakeOllama(server, new ConcurrentLinkedQueue<>(), new CopyOnWriteArrayList<>());
                server.createContext("/api/chat", exchange -> {
                    fake.requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    byte[] body = fake.responses.remove().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
                server.start();
                return fake;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        String url() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        void respond(String json) {
            responses.add(json.replace("\n", ""));
        }

        void reset() {
            responses.clear();
            requests.clear();
        }

        void stop() {
            server.stop(0);
        }
    }
}
