package com.apchavez.products.application;

import com.apchavez.products.domain.model.CategoryStockSummary;

import java.util.List;

/** Application-level result of the inventory summary report. */
public record InventorySummary(
        long activeProductCount,
        long inactiveProductCount,
        long totalActiveStock,
        double totalActiveValue,
        List<CategoryStockSummary> byCategory) {
}
