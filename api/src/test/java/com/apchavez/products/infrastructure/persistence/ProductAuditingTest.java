package com.apchavez.products.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves {@code @EnableJpaAuditing} (JpaAuditingConfig) actually populates the timestamps — without it, both fields stay null forever. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductAuditingTest {

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
    private PlatformTransactionManager transactionManager;

    @Test
    void createdAtAndUpdatedAt_arePopulatedAutomaticallyOnInsertAndUpdate() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Integer productId = tx.execute(status -> {
            CategoryEntity category = categoryJpaRepository.save(new CategoryEntity("Auditing Category"));
            ProductEntity saved = productJpaRepository.save(
                    new ProductEntity("AUDIT-1", "Audited Product", null, category, 1.0, 1, true));
            return saved.getId();
        });

        ProductEntity afterInsert = tx.execute(status -> productJpaRepository.findById(productId).orElseThrow());
        assertThat(afterInsert.getCreatedAt()).isNotNull();
        assertThat(afterInsert.getUpdatedAt()).isNotNull();

        tx.executeWithoutResult(status -> {
            ProductEntity toUpdate = productJpaRepository.findById(productId).orElseThrow();
            toUpdate.setStock(99);
            productJpaRepository.save(toUpdate);
        });

        ProductEntity afterUpdate = tx.execute(status -> productJpaRepository.findById(productId).orElseThrow());
        assertThat(afterUpdate.getCreatedAt()).isEqualTo(afterInsert.getCreatedAt());
        assertThat(afterUpdate.getUpdatedAt()).isAfterOrEqualTo(afterInsert.getUpdatedAt());
    }
}
