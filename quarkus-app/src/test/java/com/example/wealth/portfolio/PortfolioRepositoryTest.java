package com.example.wealth.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wealth.portfolio.Exposure.Dimension;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

class PortfolioRepositoryTest {

    private final PortfolioRepository repository =
            newRepository();

    @Test
    void loadsSharedFixtures() {
        Client client = repository.find("C-1002");

        assertThat(client.riskProfile()).isEqualTo(Client.RiskProfile.GROWTH);
        assertThat(client.portfolioValue()).isEqualByComparingTo("63000");
        assertThat(client.exposureBy(Dimension.SECTOR).lines().getFirst())
                .satisfies(line -> {
                    assertThat(line.key()).isEqualTo("TECHNOLOGY");
                    assertThat(line.percent()).isEqualByComparingTo("69.05");
                });
    }

    @Test
    void unknownClientIsRejected() {
        assertThatThrownBy(() -> repository.find("C-9999"))
                .isInstanceOf(PortfolioRepository.UnknownClientException.class)
                .hasMessageContaining("C-9999");
    }

    static PortfolioRepository newRepository() {
        try {
            return new PortfolioRepository(new ObjectMapper(), "../data");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
