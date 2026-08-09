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

/** Exercises the custom JPQL query ({@code ProductJpaRepository.searchByCategoryAndPriceRange}). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductJpqlSearchQueryTest {

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
        electronics = categoryJpaRepository.save(new CategoryEntity("JPQL Electronics"));
        home = categoryJpaRepository.save(new CategoryEntity("JPQL Home"));

        productJpaRepository.save(new ProductEntity("JPQL-CHEAP", "Cheap electronics", null, electronics, 5.0, 10, true));
        productJpaRepository.save(new ProductEntity("JPQL-MID", "Mid electronics", null, electronics, 50.0, 10, true));
        productJpaRepository.save(new ProductEntity("JPQL-EXPENSIVE", "Expensive electronics", null, electronics, 500.0, 10, true));
        productJpaRepository.save(new ProductEntity("JPQL-HOME-MID", "Mid home item", null, home, 50.0, 10, true));
    }

    // The Postgres container is shared (@ServiceConnection) across every test method in this
    // class, and CategoryEntity.name has a unique constraint — without cleanup, the second test
    // method's setUp() fails with a duplicate-key DataIntegrityViolationException on "JPQL
    // Electronics" left over from the first method.
    @AfterEach
    void tearDown() {
        productJpaRepository.deleteAll();
        categoryJpaRepository.deleteAll();
    }

    @Test
    void searchByCategoryAndPriceRange_filtersOnBothCategoryAndPrice() {
        List<ProductEntity> results = productJpaRepository.searchByCategoryAndPriceRange(
                electronics.getId(), 10.0, 100.0);

        assertThat(results).extracting(ProductEntity::getSku).containsExactly("JPQL-MID");
    }

    @Test
    void searchByCategoryAndPriceRange_excludesOtherCategoriesEvenInPriceRange() {
        List<ProductEntity> results = productJpaRepository.searchByCategoryAndPriceRange(
                electronics.getId(), 0.0, 1000.0);

        assertThat(results).extracting(ProductEntity::getSku)
                .containsExactly("JPQL-CHEAP", "JPQL-MID", "JPQL-EXPENSIVE")
                .doesNotContain("JPQL-HOME-MID");
    }
}
