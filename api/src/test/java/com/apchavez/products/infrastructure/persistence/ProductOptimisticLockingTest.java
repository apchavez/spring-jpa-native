package com.apchavez.products.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves {@code @Version} optimistic locking really throws a real
 * {@link ObjectOptimisticLockingFailureException} against a real Postgres row — not mocked, not
 * asserted against a stubbed repository. Two independent, sequential transactions both start
 * from the SAME initial row version; the first to commit wins, the second's flush fails because
 * the version it read is no longer current.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductOptimisticLockingTest {

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
    void concurrentUpdatesToSameProduct_secondCommitFailsWithOptimisticLockException() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Integer productId = tx.execute(status -> {
            CategoryEntity category = categoryJpaRepository.save(new CategoryEntity("Optimistic Lock Category"));
            ProductEntity saved = productJpaRepository.save(
                    new ProductEntity("OPTLOCK-1", "Contended Product", null, category, 10.0, 100, true));
            return saved.getId();
        });

        // "Transaction A" and "Transaction B" both load the product at version 0 — simulating two
        // concurrent HTTP PUT requests that both read the current state before either writes.
        ProductEntity loadedByTransactionA = tx.execute(status -> productJpaRepository.findById(productId).orElseThrow());
        ProductEntity loadedByTransactionB = tx.execute(status -> productJpaRepository.findById(productId).orElseThrow());
        assertThat(loadedByTransactionA.getVersion()).isEqualTo(loadedByTransactionB.getVersion());

        // Transaction B commits first — stock 100 -> 80, version 0 -> 1.
        tx.executeWithoutResult(status -> {
            loadedByTransactionB.setStock(80);
            productJpaRepository.save(loadedByTransactionB);
        });

        // Transaction A still holds the version-0 entity it loaded before B committed. Its
        // update (stock 100 -> 50) must fail: the row is now at version 1, not the version-0
        // snapshot A is trying to overwrite.
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            loadedByTransactionA.setStock(50);
            productJpaRepository.saveAndFlush(loadedByTransactionA);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // The row reflects B's write only — A's lost update never landed, which is the entire
        // point of optimistic locking.
        ProductEntity finalState = tx.execute(status -> productJpaRepository.findById(productId).orElseThrow());
        assertThat(finalState.getStock()).isEqualTo(80);
        assertThat(finalState.getVersion()).isEqualTo(1L);
    }
}
