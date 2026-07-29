package com.apchavez.products.domain.port;

import com.apchavez.products.domain.model.CategoryStockSummary;
import com.apchavez.products.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product product);
    Product update(Product product);
    Optional<Product> findById(Integer id);
    Optional<Product> findBySku(String sku);
    List<Product> findAllActive(int page, int size);
    long countActive();

    /**
     * Same rows as {@link #findAllActive}, but deliberately WITHOUT the {@code @EntityGraph} join —
     * {@code categoryName} comes back {@code null} on every row. Feeds the GraphQL {@code products}
     * query specifically, so {@code Product.category} has to be resolved independently by the
     * DataLoader-backed field resolver instead of arriving pre-joined — otherwise there would be
     * nothing left for the DataLoader to actually demonstrate.
     */
    List<Product> findAllActiveLight(int page, int size);
    List<Product> findAllInactive(int page, int size);
    long countInactive();
    List<Product> searchByNamePrefix(String prefix, int page, int size);
    long countByNamePrefix(String prefix);

    /**
     * JPQL-backed search combining category and an inclusive price range — the concrete
     * JPA adapter implements this with a custom {@code @Query}, not a derived method name.
     */
    List<Product> searchByCategoryAndPriceRange(Integer categoryId, Double minPrice, Double maxPrice);

    void delete(Integer id);
    List<Product> findAll();

    /** Sum of stock across active products only — JPQL aggregation, not a Java loop. */
    long sumActiveStock();

    /** Sum of price * stock across active products only — JPQL aggregation, not a Java loop. */
    double sumActiveValue();

    /** Per-category breakdown (count/stock/value) restricted to active products. */
    List<CategoryStockSummary> categoryStockSummaryForActive();
}
