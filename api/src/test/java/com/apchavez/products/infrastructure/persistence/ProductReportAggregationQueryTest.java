package com.apchavez.products.infrastructure.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Exercises the JPQL aggregate queries backing the inventory summary report
 * ({@code sumActiveStock}, {@code sumActiveValue}, {@code categoryStockSummaryForActive}) — all
 * computed with {@code GROUP BY}/{@code SUM} in the database, not by looping in Java.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductReportAggregationQueryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    private CategoryEntity electronics;
    private CategoryEntity home;

    @BeforeEach
    void setUp() {
        electronics = categoryJpaRepository.save(new CategoryEntity("Report Electronics"));
        home = categoryJpaRepository.save(new CategoryEntity("Report Home"));

        productJpaRepository.save(new ProductEntity("REPORT-E1", "Active Electronics 1", null, electronics, 10.0, 5, true));
        productJpaRepository.save(new ProductEntity("REPORT-E2", "Active Electronics 2", null, electronics, 20.0, 3, true));
        productJpaRepository.save(new ProductEntity("REPORT-E3", "Inactive Electronics", null, electronics, 100.0, 7, false));
        productJpaRepository.save(new ProductEntity("REPORT-H1", "Active Home 1", null, home, 5.0, 10, true));
    }

    @AfterEach
    void tearDown() {
        productJpaRepository.deleteAll();
        categoryJpaRepository.deleteAll();
    }

    @Test
    void sumActiveStock_onlyCountsActiveProducts() {
        // active: 5 + 3 + 10 = 18 (excludes the inactive row's 7)
        assertThat(productJpaRepository.sumActiveStock()).isEqualTo(18L);
    }

    @Test
    void sumActiveValue_onlyCountsActiveProducts() {
        // active: 10*5 + 20*3 + 5*10 = 50 + 60 + 50 = 160 (excludes the inactive row's 700)
        assertThat(productJpaRepository.sumActiveValue()).isCloseTo(160.0, within(0.001));
    }

    @Test
    void categoryStockSummaryForActive_groupsPerCategoryExcludingInactive() {
        List<CategoryStockSummaryProjection> summaries = productJpaRepository.categoryStockSummaryForActive();

        assertThat(summaries).hasSize(2);

        CategoryStockSummaryProjection electronicsSummary = summaries.stream()
                .filter(s -> s.getCategoryId().equals(electronics.getId()))
                .findFirst().orElseThrow();
        assertThat(electronicsSummary.getProductCount()).isEqualTo(2L);
        assertThat(electronicsSummary.getTotalStock()).isEqualTo(8L);
        assertThat(electronicsSummary.getTotalValue()).isCloseTo(110.0, within(0.001));

        CategoryStockSummaryProjection homeSummary = summaries.stream()
                .filter(s -> s.getCategoryId().equals(home.getId()))
                .findFirst().orElseThrow();
        assertThat(homeSummary.getProductCount()).isEqualTo(1L);
        assertThat(homeSummary.getTotalStock()).isEqualTo(10L);
        assertThat(homeSummary.getTotalValue()).isCloseTo(50.0, within(0.001));
    }

    @Test
    void sumActiveStock_noProducts_returnsZeroNotNull() {
        productJpaRepository.deleteAll();

        assertThat(productJpaRepository.sumActiveStock()).isZero();
        assertThat(productJpaRepository.sumActiveValue()).isZero();
        assertThat(productJpaRepository.categoryStockSummaryForActive()).isEmpty();
    }
}
