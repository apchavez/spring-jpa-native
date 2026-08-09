package com.apchavez.products.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Integer> {

    Optional<ProductEntity> findBySku(String sku);

    /**
     * Deliberately N+1-prone: {@code category} is lazy (see {@link ProductEntity}), so iterating
     * this page and calling {@code getCategory().getName()} on each row issues one extra SELECT
     * per product — kept around (and exercised in {@code ProductNPlusOneQueryTest}) specifically
     * to prove the fix below actually fixes something, rather than fixing a problem that was
     * never real.
     */
    Page<ProductEntity> findByActive(Boolean active, Pageable pageable);

    /**
     * The fix: {@code @EntityGraph} tells Hibernate to fetch {@code category} in the same query
     * (a JOIN) instead of lazily per-row — same result set, one SQL statement regardless of page
     * size instead of 1+N.
     */
    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findWithCategoryByActive(Boolean active, Pageable pageable);

    Page<ProductEntity> findByNameStartingWithIgnoreCase(String prefix, Pageable pageable);

    /**
     * Custom JPQL query (not a derived method name) — an explicit {@code JOIN FETCH} so this one
     * is also single-statement regardless of category laziness, combining an equality filter
     * (category) with a range filter (price) in one query.
     */
    @Query("""
            SELECT p FROM ProductEntity p JOIN FETCH p.category c
            WHERE c.id = :categoryId AND p.price BETWEEN :minPrice AND :maxPrice
            ORDER BY p.price ASC
            """)
    java.util.List<ProductEntity> searchByCategoryAndPriceRange(
            @Param("categoryId") Integer categoryId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice);

    /** Sum of stock across active products — {@code COALESCE} avoids a null when there are none. */
    @Query("SELECT COALESCE(SUM(p.stock), 0L) FROM ProductEntity p WHERE p.active = true")
    long sumActiveStock();

    /** Sum of price * stock across active products — {@code COALESCE} avoids a null when there are none. */
    @Query("SELECT COALESCE(SUM(p.price * p.stock), 0.0) FROM ProductEntity p WHERE p.active = true")
    double sumActiveValue();

    /**
     * Per-category breakdown (count/stock/value) for active products only, computed in one JPQL
     * {@code GROUP BY} query — avoids the N+1 that looping over {@link #findAll()} in Java would
     * cause when resolving each product's lazy category.
     */
    @Query("""
            SELECT new com.apchavez.products.infrastructure.persistence.CategoryStockSummaryProjection(
                c.id, c.name, COUNT(p), COALESCE(SUM(p.stock), 0L), COALESCE(SUM(p.price * p.stock), 0.0))
            FROM ProductEntity p JOIN p.category c
            WHERE p.active = true
            GROUP BY c.id, c.name
            ORDER BY c.name ASC
            """)
    java.util.List<CategoryStockSummaryProjection> categoryStockSummaryForActive();
}
