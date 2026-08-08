package com.apchavez.products.application;

import com.apchavez.products.domain.service.ProductDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportApplicationService {

    private final ProductDomainService domainService;

    public ReportApplicationService(ProductDomainService domainService) {
        this.domainService = domainService;
    }

    @Transactional(readOnly = true)
    public InventorySummary inventorySummary() {
        long activeCount = domainService.countActiveProducts();
        long inactiveCount = domainService.countInactiveProducts();
        long totalActiveStock = domainService.sumActiveStock();
        double totalActiveValue = domainService.sumActiveValue();
        return new InventorySummary(activeCount, inactiveCount, totalActiveStock, totalActiveValue,
                domainService.categoryStockSummaryForActive());
    }
}
