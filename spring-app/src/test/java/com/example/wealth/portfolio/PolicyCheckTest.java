package com.example.wealth.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wealth.portfolio.Client.RiskProfile;
import com.example.wealth.portfolio.Holding.AssetClass;
import com.example.wealth.portfolio.PolicyCheck.Limit;
import com.example.wealth.portfolio.PolicyCheck.Status;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyCheckTest {

    @Test
    void cryptoAboveFivePercentIsReportedWithTheExcess() {
        Client client = client(RiskProfile.GROWTH,
                holding(AssetClass.EQUITY, "52000"), holding(AssetClass.CRYPTO, "8000"), holding(AssetClass.CASH, "3000"));

        PolicyCheck check = client.policyCheck();

        Limit crypto = limit(check, "Crypto-assets");
        assertThat(crypto.percent()).isEqualByComparingTo("12.70");
        assertThat(crypto.maxPercent()).isEqualByComparingTo("5");
        assertThat(crypto.status()).isEqualTo(Status.ABOVE_MAX);
        assertThat(crypto.deviationPercentagePoints()).isEqualByComparingTo("7.70");
        assertThat(crypto.deviationEur()).isEqualByComparingTo("4850");
        assertThat(check.withinPolicy()).isFalse();
    }

    @Test
    void cryptoIsNotPermittedOutsideTheGrowthProfile() {
        Client client = client(RiskProfile.BALANCED,
                holding(AssetClass.EQUITY, "45"), holding(AssetClass.BOND, "45"),
                holding(AssetClass.CASH, "9"), holding(AssetClass.CRYPTO, "1"));

        Limit crypto = limit(client.policyCheck(), "Crypto-assets");

        assertThat(crypto.status()).isEqualTo(Status.NOT_PERMITTED);
        assertThat(crypto.maxPercent()).isEqualByComparingTo("0");
        assertThat(crypto.deviationEur()).isEqualByComparingTo("1");
    }

    @Test
    void assetClassRangesOfTheRiskProfileAreChecked() {
        Client client = client(RiskProfile.BALANCED,
                holding(AssetClass.EQUITY, "50"), holding(AssetClass.FUND, "20"),
                holding(AssetClass.BOND, "29"), holding(AssetClass.CASH, "1"));

        PolicyCheck check = client.policyCheck();

        Limit equitiesAndFunds = limit(check, "Equities and funds");
        assertThat(equitiesAndFunds.percent()).isEqualByComparingTo("70");
        assertThat(equitiesAndFunds.status()).isEqualTo(Status.ABOVE_MAX);
        assertThat(equitiesAndFunds.deviationPercentagePoints()).isEqualByComparingTo("10");
        Limit cash = limit(check, "Cash");
        assertThat(cash.status()).isEqualTo(Status.BELOW_MIN);
        assertThat(cash.deviationPercentagePoints()).isEqualByComparingTo("1");
        assertThat(cash.deviationEur()).isEqualByComparingTo("1");
        assertThat(limit(check, "Bonds").status()).isEqualTo(Status.WITHIN);
    }

    @Test
    void portfolioInsideAllLimitsIsWithinPolicy() {
        Client client = client(RiskProfile.CONSERVATIVE,
                holding(AssetClass.EQUITY, "20"), holding(AssetClass.BOND, "70"), holding(AssetClass.CASH, "10"));

        PolicyCheck check = client.policyCheck();

        assertThat(check.withinPolicy()).isTrue();
        assertThat(check.limits()).allSatisfy(limit -> {
            assertThat(limit.status()).isEqualTo(Status.WITHIN);
            assertThat(limit.deviationEur()).isEqualByComparingTo("0");
        });
    }

    @Test
    void namesTheLimitsItDoesNotCheck() {
        PolicyCheck check = client(RiskProfile.GROWTH, holding(AssetClass.EQUITY, "100")).policyCheck();

        assertThat(check.notChecked()).anyMatch(text -> text.contains("issuer"))
                .anyMatch(text -> text.contains("sector"))
                .anyMatch(text -> text.contains("region"));
    }

    @Test
    void emptyPortfolioHasNothingToBreach() {
        PolicyCheck check = client(RiskProfile.BALANCED).policyCheck();

        assertThat(check.portfolioValue()).isEqualByComparingTo("0");
        assertThat(check.limits()).isEmpty();
        assertThat(check.withinPolicy()).isTrue();
    }

    private static Limit limit(PolicyCheck check, String name) {
        return check.limits().stream().filter(limit -> limit.name().equals(name)).findFirst().orElseThrow();
    }

    private static Client client(RiskProfile riskProfile, Holding... holdings) {
        return new Client("C-TEST", "Test Client", riskProfile, List.of(holdings));
    }

    private static Holding holding(AssetClass assetClass, String marketValue) {
        return new Holding(assetClass.name(), assetClass.name(), assetClass, "SECTOR", "REGION",
                BigDecimal.ONE, new BigDecimal(marketValue));
    }
}
