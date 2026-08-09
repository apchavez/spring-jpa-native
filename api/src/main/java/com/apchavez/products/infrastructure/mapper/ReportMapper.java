package com.apchavez.products.infrastructure.mapper;

import com.apchavez.products.application.InventorySummary;
import com.apchavez.products.infrastructure.web.dto.CategoryStockSummaryDTO;
import com.apchavez.products.infrastructure.web.dto.InventorySummaryDTO;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public InventorySummaryDTO toDTO(InventorySummary summary) {
        return new InventorySummaryDTO(
                summary.activeProductCount(),
                summary.inactiveProductCount(),
                summary.totalActiveStock(),
                summary.totalActiveValue(),
                summary.byCategory().stream()
                        .map(c -> new CategoryStockSummaryDTO(
                                c.categoryId(), c.categoryName(), c.productCount(), c.totalStock(), c.totalValue()))
                        .toList());
    }
}
