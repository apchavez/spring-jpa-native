package com.apchavez.products.infrastructure.persistence;

import com.apchavez.products.domain.exception.CategoryNotFoundException;
import com.apchavez.products.domain.model.CategoryStockSummary;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.domain.port.ProductRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository jpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;

    public ProductPersistenceAdapter(ProductJpaRepository jpaRepository,
                                      CategoryJpaRepository categoryJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @Override
    public Product save(Product product) {
        CategoryEntity category = requireCategory(product.categoryId());
        ProductEntity entity = new ProductEntity(product.sku(), product.name(), product.description(),
                category, product.price(), product.stock(), product.active());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Product update(Product product) {
        ProductEntity entity = jpaRepository.findById(product.id()).orElseThrow();
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setCategory(requireCategory(product.categoryId()));
        entity.setPrice(product.price());
        entity.setStock(product.stock());
        entity.setActive(product.active());
        return toDomain(entity);
    }

    @Override
    public Optional<Product> findById(Integer id) {
        return jpaRepository.findById(id).map(ProductPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return jpaRepository.findBySku(sku).map(ProductPersistenceAdapter::toDomain);
    }

    @Override
    public List<Product> findAllActive(int page, int size) {
        return fetchPage(true, page, size);
    }

    @Override
    public long countActive() {
        return jpaRepository.findByActive(true, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<Product> findAllActiveLight(int page, int size) {
        return jpaRepository.findByActive(true, PageRequest.of(page, size)).stream()
                .map(ProductPersistenceAdapter::toDomainLight)
                .toList();
    }

    @Override
    public List<Product> findAllInactive(int page, int size) {
        return fetchPage(false, page, size);
    }

    @Override
    public long countInactive() {
        return jpaRepository.findByActive(false, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<Product> searchByNamePrefix(String prefix, int page, int size) {
        return jpaRepository.findByNameStartingWithIgnoreCase(prefix, PageRequest.of(page, size))
                .map(ProductPersistenceAdapter::toDomain)
                .getContent();
    }

    @Override
    public long countByNamePrefix(String prefix) {
        return jpaRepository.findByNameStartingWithIgnoreCase(prefix, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<Product> searchByCategoryAndPriceRange(Integer categoryId, Double minPrice, Double maxPrice) {
        return jpaRepository.searchByCategoryAndPriceRange(categoryId, minPrice, maxPrice).stream()
                .map(ProductPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public void delete(Integer id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll().stream().map(ProductPersistenceAdapter::toDomain).toList();
    }

    @Override
    public long sumActiveStock() {
        return jpaRepository.sumActiveStock();
    }

    @Override
    public double sumActiveValue() {
        return jpaRepository.sumActiveValue();
    }

    @Override
    public List<CategoryStockSummary> categoryStockSummaryForActive() {
        return jpaRepository.categoryStockSummaryForActive().stream()
                .map(p -> new CategoryStockSummary(p.getCategoryId(), p.getCategoryName(),
                        p.getProductCount(), p.getTotalStock(), p.getTotalValue()))
                .toList();
    }

    /**
     * Uses {@code findWithCategoryByActive} (the {@code @EntityGraph} version), not the plain
     * {@code findByActive} — real request traffic always gets the N+1-free path. The N+1-prone
     * method exists only to be exercised directly by {@code ProductNPlusOneQueryTest}.
     */
    private List<Product> fetchPage(boolean active, int page, int size) {
        Page<ProductEntity> result = jpaRepository.findWithCategoryByActive(active, PageRequest.of(page, size));
        return result.map(ProductPersistenceAdapter::toDomain).getContent();
    }

    private CategoryEntity requireCategory(Integer categoryId) {
        return categoryJpaRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    static Product toDomain(ProductEntity entity) {
        return new Product(entity.getId(), entity.getSku(), entity.getName(), entity.getDescription(),
                entity.getCategory().getId(), entity.getCategory().getName(),
                entity.getPrice(), entity.getStock(), entity.getActive());
    }

    /**
     * Reads only {@code category.getId()} off the lazy proxy — Hibernate populates a lazy
     * {@code @ManyToOne} proxy with its id up front and only hits the database the first time a
     * non-id accessor (like {@code getName()}) is called, so this never triggers the per-row SELECT
     * that {@link #toDomain} deliberately does. {@code categoryName} stays {@code null}; the GraphQL
     * {@code category} field resolver fetches it separately via the batched DataLoader.
     */
    static Product toDomainLight(ProductEntity entity) {
        return new Product(entity.getId(), entity.getSku(), entity.getName(), entity.getDescription(),
                entity.getCategory().getId(), null,
                entity.getPrice(), entity.getStock(), entity.getActive());
    }
}
