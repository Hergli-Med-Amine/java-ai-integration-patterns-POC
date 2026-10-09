package com.example.wealth.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.example.wealth.portfolio.Client.RiskProfile;
import com.example.wealth.portfolio.Exposure.Dimension;
import com.example.wealth.portfolio.Exposure.Line;
import com.example.wealth.portfolio.Holding.AssetClass;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExposureTest {

    private final Client client = new Client("C-TEST", "Test Client", RiskProfile.BALANCED, List.of(
            holding("AAA", AssetClass.EQUITY, "TECHNOLOGY", "NORTH_AMERICA", "10", "50"),
            holding("BBB", AssetClass.EQUITY, "TECHNOLOGY", "EUROPE", "5", "40"),
            holding("CCC", AssetClass.BOND, "GOVERNMENT", "EUROPE", "3", "100"),
            holding("CASH", AssetClass.CASH, "CASH", "EUROPE", "100", "1")));

    @Test
    void portfolioValueIsSumOfMarketValues() {
        assertThat(client.portfolioValue()).isEqualByComparingTo("1100");
    }

    @Test
    void groupsBySectorLargestFirst() {
        Exposure exposure = client.exposureBy(Dimension.SECTOR);

        assertThat(exposure.lines()).extracting(Line::key).containsExactly("TECHNOLOGY", "GOVERNMENT", "CASH");
        Line tech = exposure.lines().getFirst();
        assertThat(tech.marketValue()).isEqualByComparingTo("700");
        assertThat(tech.percent()).isEqualByComparingTo("63.64");
    }

    @Test
    void groupsByRegionAndAssetClass() {
        assertThat(client.exposureBy(Dimension.REGION).lines())
                .extracting(Line::key, Line::percent)
                .containsExactly(
                        tuple("EUROPE", new BigDecimal("54.55")),
                        tuple("NORTH_AMERICA", new BigDecimal("45.45")));
        assertThat(client.exposureBy(Dimension.ASSET_CLASS).lines())
                .extracting(Line::key)
                .containsExactly("EQUITY", "BOND", "CASH");
    }

    @Test
    void emptyPortfolioHasNoExposureLines() {
        Client empty = new Client("C-EMPTY", "Empty", RiskProfile.GROWTH, List.of());

        Exposure exposure = empty.exposureBy(Dimension.SECTOR);

        assertThat(exposure.portfolioValue()).isEqualByComparingTo("0");
        assertThat(exposure.lines()).isEmpty();
    }

    private static Holding holding(String symbol, AssetClass assetClass, String sector, String region,
                                   String quantity, String price) {
        return new Holding(symbol, symbol, assetClass, sector, region, new BigDecimal(quantity), new BigDecimal(price));
    }
}
