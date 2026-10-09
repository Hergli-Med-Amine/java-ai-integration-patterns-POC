package com.example.wealth.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Exposure(Dimension dimension, BigDecimal portfolioValue, List<Line> lines) {

    public enum Dimension {
        SECTOR(Holding::sector),
        REGION(Holding::region),
        ASSET_CLASS(holding -> holding.assetClass().name());

        private final Function<Holding, String> key;

        Dimension(Function<Holding, String> key) {
            this.key = key;
        }
    }

    public record Line(String key, BigDecimal marketValue, BigDecimal percent) {}

    static Exposure of(Dimension dimension, List<Holding> holdings) {
        BigDecimal total = holdings.stream().map(Holding::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) {
            return new Exposure(dimension, total, List.of());
        }
        Map<String, BigDecimal> valueByKey = holdings.stream().collect(Collectors.groupingBy(
                dimension.key, Collectors.reducing(BigDecimal.ZERO, Holding::marketValue, BigDecimal::add)));
        List<Line> lines = valueByKey.entrySet().stream()
                .map(entry -> new Line(entry.getKey(), entry.getValue(), percentOf(entry.getValue(), total)))
                .sorted(Comparator.comparing(Line::marketValue).reversed().thenComparing(Line::key))
                .toList();
        return new Exposure(dimension, total, lines);
    }

    private static BigDecimal percentOf(BigDecimal part, BigDecimal total) {
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }
}
