package com.apchavez.products.domain.model;

/**
 * Per-category aggregate over active products only — backs the inventory summary report.
 * Computed via JPQL {@code GROUP BY} in the persistence layer, never by looping in Java.
 */
public record CategoryStockSummary(
        Integer categoryId,
        String categoryName,
        long productCount,
        long totalStock,
        double totalValue) {
}
