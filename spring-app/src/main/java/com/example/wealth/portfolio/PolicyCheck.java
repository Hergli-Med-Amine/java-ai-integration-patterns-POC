package com.example.wealth.portfolio;

import com.example.wealth.portfolio.Client.RiskProfile;
import com.example.wealth.portfolio.Holding.AssetClass;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The investment policy limits that can be computed from the portfolio data, checked in code so the model only
// reports the result. The limits are copied from data/documents/investment-policy.txt (ADR 0006).
public record PolicyCheck(RiskProfile riskProfile, BigDecimal portfolioValue, boolean withinPolicy,
                          List<Limit> limits, List<String> notChecked) {

    public enum Status { WITHIN, ABOVE_MAX, BELOW_MIN, NOT_PERMITTED }

    // Deviations are zero when the limit is met, otherwise the distance to the nearest bound.
    public record Limit(String name, String source, BigDecimal percent, BigDecimal minPercent, BigDecimal maxPercent,
                        Status status, BigDecimal deviationPercentagePoints, BigDecimal deviationEur) {}

    private record Range(String name, Set<AssetClass> assetClasses, int minPercent, int maxPercent) {}

    private static final String RANGES_SOURCE = "Investment Policy, section 2. Risk profiles";
    private static final String CRYPTO_SOURCE = "Investment Policy, section 4. Crypto-assets";
    private static final int CRYPTO_MAX_PERCENT = 5;

    private static final Map<RiskProfile, List<Range>> RANGES = Map.of(
            RiskProfile.CONSERVATIVE, ranges(0, 30, 50, 90, 5, 30),
            RiskProfile.BALANCED, ranges(30, 60, 20, 60, 2, 20),
            RiskProfile.GROWTH, ranges(60, 95, 0, 30, 1, 15));

    private static final List<String> NOT_CHECKED = List.of(
            "Single issuer limit (section 3): needs bond ratings, which the portfolio data does not have.",
            "Single sector limit (section 3): the policy does not say how government bonds and cash count.",
            "Single region limit (section 3): needs the client's home region, which the portfolio data does not have.");

    static PolicyCheck of(RiskProfile riskProfile, List<Holding> holdings) {
        BigDecimal total = holdings.stream().map(Holding::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) {
            return new PolicyCheck(riskProfile, total, true, List.of(), NOT_CHECKED);
        }
        List<Limit> limits = new ArrayList<>();
        for (Range range : RANGES.get(riskProfile)) {
            limits.add(limit(range.name(), RANGES_SOURCE, valueOf(holdings, range.assetClasses()), total,
                    range.minPercent(), range.maxPercent()));
        }
        BigDecimal crypto = valueOf(holdings, Set.of(AssetClass.CRYPTO));
        if (riskProfile == RiskProfile.GROWTH) {
            limits.add(limit("Crypto-assets", CRYPTO_SOURCE, crypto, total, 0, CRYPTO_MAX_PERCENT));
        } else {
            Limit notPermitted = limit("Crypto-assets", CRYPTO_SOURCE, crypto, total, 0, 0);
            limits.add(notPermitted.status() == Status.WITHIN ? notPermitted : withStatus(notPermitted, Status.NOT_PERMITTED));
        }
        boolean withinPolicy = limits.stream().allMatch(limit -> limit.status() == Status.WITHIN);
        return new PolicyCheck(riskProfile, total, withinPolicy, List.copyOf(limits), NOT_CHECKED);
    }

    private static Limit limit(String name, String source, BigDecimal value, BigDecimal total,
                               int minPercent, int maxPercent) {
        BigDecimal min = percentOfTotal(minPercent, total);
        BigDecimal max = percentOfTotal(maxPercent, total);
        Status status = Status.WITHIN;
        BigDecimal deviation = BigDecimal.ZERO;
        if (value.compareTo(max) > 0) {
            status = Status.ABOVE_MAX;
            deviation = value.subtract(max);
        } else if (value.compareTo(min) < 0) {
            status = Status.BELOW_MIN;
            deviation = min.subtract(value);
        }
        return new Limit(name, source, percent(value, total), BigDecimal.valueOf(minPercent),
                BigDecimal.valueOf(maxPercent), status, percent(deviation, total), deviation.setScale(2, RoundingMode.HALF_UP));
    }

    private static Limit withStatus(Limit limit, Status status) {
        return new Limit(limit.name(), limit.source(), limit.percent(), limit.minPercent(), limit.maxPercent(), status,
                limit.deviationPercentagePoints(), limit.deviationEur());
    }

    private static BigDecimal valueOf(List<Holding> holdings, Set<AssetClass> assetClasses) {
        return holdings.stream().filter(holding -> assetClasses.contains(holding.assetClass()))
                .map(Holding::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal percentOfTotal(int percent, BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100));
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal total) {
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static List<Range> ranges(int equitiesMin, int equitiesMax, int bondsMin, int bondsMax,
                                      int cashMin, int cashMax) {
        return List.of(
                new Range("Equities and funds", Set.of(AssetClass.EQUITY, AssetClass.FUND), equitiesMin, equitiesMax),
                new Range("Bonds", Set.of(AssetClass.BOND), bondsMin, bondsMax),
                new Range("Cash", Set.of(AssetClass.CASH), cashMin, cashMax));
    }
}
