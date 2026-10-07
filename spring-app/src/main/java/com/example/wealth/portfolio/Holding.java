package com.example.wealth.portfolio;

import java.math.BigDecimal;

public record Holding(
        String symbol,
        String name,
        AssetClass assetClass,
        String sector,
        String region,
        BigDecimal quantity,
        BigDecimal price) {

    public enum AssetClass { EQUITY, BOND, FUND, CRYPTO, CASH }

    public BigDecimal marketValue() {
        return quantity.multiply(price);
    }
}
