package com.example.wealth.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record Client(String id, String name, RiskProfile riskProfile, List<Holding> holdings) {

    public enum RiskProfile { CONSERVATIVE, BALANCED, GROWTH }

    public BigDecimal portfolioValue() {
        return holdings.stream().map(Holding::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Exposure exposureBy(Exposure.Dimension dimension) {
        return Exposure.of(dimension, holdings);
    }

    public PolicyCheck policyCheck() {
        return PolicyCheck.of(riskProfile, holdings);
    }
}
