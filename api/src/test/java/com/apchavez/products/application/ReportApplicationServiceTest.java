package com.apchavez.products.application;

import com.apchavez.products.domain.model.CategoryStockSummary;
import com.apchavez.products.domain.service.ProductDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportApplicationServiceTest {

    @Mock
    private ProductDomainService domainService;

    private ReportApplicationService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportApplicationService(domainService);
    }

    @Test
    void inventorySummary_composesAllAggregatesFromDomainService() {
        CategoryStockSummary electronics = new CategoryStockSummary(1, "Electronics", 3L, 30L, 900.0);
        CategoryStockSummary accessories = new CategoryStockSummary(2, "Accessories", 1L, 5L, 50.0);
        when(domainService.countActiveProducts()).thenReturn(4L);
        when(domainService.countInactiveProducts()).thenReturn(2L);
        when(domainService.sumActiveStock()).thenReturn(35L);
        when(domainService.sumActiveValue()).thenReturn(950.0);
        when(domainService.categoryStockSummaryForActive()).thenReturn(List.of(electronics, accessories));

        InventorySummary summary = reportService.inventorySummary();

        assertThat(summary.activeProductCount()).isEqualTo(4L);
        assertThat(summary.inactiveProductCount()).isEqualTo(2L);
        assertThat(summary.totalActiveStock()).isEqualTo(35L);
        assertThat(summary.totalActiveValue()).isEqualTo(950.0);
        assertThat(summary.byCategory()).containsExactly(electronics, accessories);
    }

    @Test
    void inventorySummary_noProducts_returnsZeroedResult() {
        when(domainService.countActiveProducts()).thenReturn(0L);
        when(domainService.countInactiveProducts()).thenReturn(0L);
        when(domainService.sumActiveStock()).thenReturn(0L);
        when(domainService.sumActiveValue()).thenReturn(0.0);
        when(domainService.categoryStockSummaryForActive()).thenReturn(List.of());

        InventorySummary summary = reportService.inventorySummary();

        assertThat(summary.activeProductCount()).isZero();
        assertThat(summary.totalActiveValue()).isZero();
        assertThat(summary.byCategory()).isEmpty();
    }
}
