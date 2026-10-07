package com.example.wealth.portfolio;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PortfolioRepository {

    private final Map<String, Client> clientsById;

    public PortfolioRepository(JsonMapper jsonMapper, @Value("${wealth.data-dir}") Path dataDir) {
        List<Client> clients = jsonMapper.readValue(dataDir.resolve("clients.json").toFile(), new TypeReference<>() {});
        this.clientsById = clients.stream().collect(Collectors.toUnmodifiableMap(Client::id, Function.identity()));
    }

    public Client find(String clientId) {
        Client client = clientsById.get(clientId);
        if (client == null) {
            throw new NoSuchElementException("Unknown client: " + clientId);
        }
        return client;
    }
}
