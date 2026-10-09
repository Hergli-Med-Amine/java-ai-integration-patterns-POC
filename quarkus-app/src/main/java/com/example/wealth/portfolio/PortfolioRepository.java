package com.example.wealth.portfolio;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class PortfolioRepository {

    private final Map<String, Client> clientsById;

    public PortfolioRepository(ObjectMapper objectMapper, @ConfigProperty(name = "wealth.data-dir") String dataDir)
            throws IOException {
        List<Client> clients = objectMapper.readValue(Path.of(dataDir, "clients.json").toFile(), new TypeReference<>() {});
        this.clientsById = clients.stream().collect(Collectors.toUnmodifiableMap(Client::id, Function.identity()));
    }

    public Client find(String clientId) {
        Client client = clientId == null ? null : clientsById.get(clientId);
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
