package com.example.wealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

// Stands in for Ollama at the HTTP level, so the real quarkus-langchain4j clients, tool loop and retrieval run in tests.
public final class FakeOllama {

    public static final FakeOllama INSTANCE = start();

    private static final int DIMENSIONS = 256;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpServer server;
    private final Queue<String> chatResponses = new ConcurrentLinkedQueue<>();
    private final List<String> chatRequests = new CopyOnWriteArrayList<>();

    private FakeOllama(HttpServer server) {
        this.server = server;
        server.createContext("/api/chat", this::chat);
        server.createContext("/api/embed", this::embed);
    }

    private static FakeOllama start() {
        try {
            FakeOllama fake = new FakeOllama(HttpServer.create(new InetSocketAddress("localhost", 0), 0));
            fake.server.start();
            return fake;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void respond(String chatResponseJson) {
        chatResponses.add(chatResponseJson.replace("\n", ""));
    }

    public List<String> chatRequests() {
        return chatRequests;
    }

    public void reset() {
        chatResponses.clear();
        chatRequests.clear();
    }

    private void chat(HttpExchange exchange) throws IOException {
        chatRequests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        send(exchange, chatResponses.remove());
    }

    private void embed(HttpExchange exchange) throws IOException {
        JsonNode input = json.readTree(exchange.getRequestBody()).get("input");
        List<int[]> embeddings = new ArrayList<>();
        if (input.isArray()) {
            input.forEach(text -> embeddings.add(embedding(text.asText())));
        } else {
            embeddings.add(embedding(input.asText()));
        }
        send(exchange, json.writeValueAsString(Map.of("model", "fake", "embeddings", embeddings)));
    }

    // Word counts hashed into a fixed-size vector: texts sharing words are similar, which is enough to test retrieval.
    private static int[] embedding(String text) {
        int[] vector = new int[DIMENSIONS];
        for (String word : text.toLowerCase().split("[^a-z]+")) {
            if (!word.isEmpty()) {
                vector[Math.floorMod(word.hashCode(), DIMENSIONS)]++;
            }
        }
        return vector;
    }

    private static void send(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    // Points the app at the fake before it starts.
    public static class Resource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Map.of("quarkus.langchain4j.ollama.base-url", "http://localhost:" + INSTANCE.server.getAddress().getPort());
        }

        @Override
        public void stop() {
        }
    }
}
