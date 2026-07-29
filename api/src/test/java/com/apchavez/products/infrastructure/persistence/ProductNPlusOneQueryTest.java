package com.apchavez.products.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the concrete N+1 problem this repo exists to demonstrate. 10 products, each pointing at
 * its OWN distinct category (so lazy-loading each one is a genuinely separate row, not something
 * Hibernate's session-level identity map can dedupe away) — one repository method is deliberately
 * N+1-prone ({@link ProductJpaRepository#findByActive}), the other fixes it via
 * {@code @EntityGraph} ({@link ProductJpaRepository#findWithCategoryByActive}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductNPlusOneQueryTest {

    private static final int PRODUCT_COUNT = 10;

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

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        for (int i = 0; i < PRODUCT_COUNT; i++) {
            CategoryEntity category = categoryJpaRepository.save(new CategoryEntity("N+1 Category " + i));
            productJpaRepository.save(new ProductEntity(
                    "NPLUSONE-" + i, "Product " + i, null, category, 9.99, 5, true));
        }
        statistics.clear();
    }

    @AfterEach
    void tearDown() {
        productJpaRepository.deleteAll();
        categoryJpaRepository.deleteAll();
    }

    // Without an open persistence context, dereferencing the lazy Category proxy below throws
    // LazyInitializationException instead of issuing the N extra SELECTs this test is proving —
    // @Transactional keeps the session open across the initial query and the lazy-load loop,
    // mirroring what @Transactional(readOnly = true) does for the real request path. Because
    // @Transactional on a @Test method wraps @BeforeEach in the SAME persistence context, the
    // categories saved in setUp() would otherwise already sit in the L1 cache — entityManager
    // .clear() evicts them so the fetch below actually has to hit the database again.
    @Test
    @Transactional
    void findByActive_withoutFetch_issuesOneExtraQueryPerDistinctCategory() {
        entityManager.clear();
        statistics.clear();

        List<ProductEntity> page = productJpaRepository.findByActive(true, PageRequest.of(0, 20)).getContent();
        assertThat(page).hasSize(PRODUCT_COUNT);

        long queriesBeforeAccess = statistics.getPrepareStatementCount();
        // Dereferencing the lazy proxy for every row is exactly what a real controller/mapper
        // does (ProductPersistenceAdapter.toDomain calls entity.getCategory().getName()) — this
        // loop reproduces that access pattern against 10 distinct category rows.
        page.forEach(p -> assertThat(p.getCategory().getName()).startsWith("N+1 Category"));
        long queriesAfterAccess = statistics.getPrepareStatementCount();

        assertThat(queriesAfterAccess - queriesBeforeAccess)
                .as("touching a distinct lazy category on each of the %d products must issue exactly %d extra SELECTs (the N+1 this test exists to prove) — not 0 and not some other count"
                        .formatted(PRODUCT_COUNT, PRODUCT_COUNT))
                .isEqualTo(PRODUCT_COUNT);
    }

    @Test
    void findWithCategoryByActive_usingEntityGraph_issuesExactlyOneQueryTotal() {
        statistics.clear();

        List<ProductEntity> page = productJpaRepository.findWithCategoryByActive(true, PageRequest.of(0, 20)).getContent();
        page.forEach(p -> assertThat(p.getCategory().getName()).startsWith("N+1 Category"));

        assertThat(statistics.getPrepareStatementCount())
                .as("@EntityGraph(attributePaths = \"category\") must fetch every category in the same JOIN as the page query — one SQL statement regardless of how many distinct categories the rows reference")
                .isEqualTo(1);
    }
}
