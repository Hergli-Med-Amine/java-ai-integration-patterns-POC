package com.example.wealth.portfolio;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PortfolioRepository {

    private final Map<String, Client> clientsById;

    // Injected as a String: Spring's String-to-Path conversion resolves "../data" as a servlet resource and fails.
    public PortfolioRepository(JsonMapper jsonMapper, @Value("${wealth.data-dir}") String dataDir) {
        List<Client> clients = jsonMapper.readValue(Path.of(dataDir, "clients.json").toFile(), new TypeReference<>() {});
        this.clientsById = clients.stream().collect(Collectors.toUnmodifiableMap(Client::id, Function.identity()));
    }

    public Client find(String clientId) {
        Client client = clientsById.get(clientId);
        if (client == null) {
            throw new UnknownClientException(clientId);
        }
        return client;
    }

    public static class UnknownClientException extends RuntimeException {
        UnknownClientException(String clientId) {
            super("Unknown client: " + clientId);
        }
    }
}
