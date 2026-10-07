package com.example.wealth.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.wealth.portfolio.Exposure.Dimension;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PortfolioRepositoryTest {

    @Autowired
    PortfolioRepository repository;

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
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("C-9999");
    }
}
