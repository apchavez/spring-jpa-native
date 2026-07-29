package com.apchavez.products.infrastructure.persistence;

/**
 * JPQL constructor-expression target for {@link ProductJpaRepository#categoryStockSummaryForActive()}.
 * Plain class (not a record) because JPA constructor expressions require a matching public
 * constructor resolvable at query-compile time — kept as a thin carrier, mapped to the domain
 * {@code CategoryStockSummary} record in {@link ProductPersistenceAdapter}.
 */
public class CategoryStockSummaryProjection {

    private final Integer categoryId;
    private final String categoryName;
    private final long productCount;
    private final long totalStock;
    private final double totalValue;

    public CategoryStockSummaryProjection(Integer categoryId, String categoryName, long productCount,
                                           long totalStock, double totalValue) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.productCount = productCount;
        this.totalStock = totalStock;
        this.totalValue = totalValue;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public long getProductCount() {
        return productCount;
    }

    public long getTotalStock() {
        return totalStock;
    }

    public double getTotalValue() {
        return totalValue;
    }
}
