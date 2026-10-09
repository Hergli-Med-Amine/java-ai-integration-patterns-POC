package com.example.wealth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Stands in for Ollama at the HTTP level, so the real Spring AI clients, tool loop and retrieval run in tests.
public final class FakeOllama {

    private static final int DIMENSIONS = 256;

    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpServer server;
    private final Queue<String> chatResponses = new ConcurrentLinkedQueue<>();
    private final List<String> chatRequests = new CopyOnWriteArrayList<>();

    private FakeOllama(HttpServer server) {
        this.server = server;
        server.createContext("/api/chat", this::chat);
        server.createContext("/api/embed", this::embed);
    }

    public static FakeOllama start() {
        try {
            FakeOllama fake = new FakeOllama(HttpServer.create(new InetSocketAddress("localhost", 0), 0));
            fake.server.start();
            return fake;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String url() {
        return "http://localhost:" + server.getAddress().getPort();
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

    public void stop() {
        server.stop(0);
    }

    private void chat(HttpExchange exchange) throws IOException {
        chatRequests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        send(exchange, chatResponses.remove());
    }

    private void embed(HttpExchange exchange) throws IOException {
        JsonNode request = json.readTree(exchange.getRequestBody());
        List<int[]> embeddings = request.get("input").valueStream().map(text -> embedding(text.asString())).toList();
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
}
