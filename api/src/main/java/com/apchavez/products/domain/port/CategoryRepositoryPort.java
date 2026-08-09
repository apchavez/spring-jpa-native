package com.apchavez.products.domain.port;

import com.apchavez.products.domain.model.Category;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {
    Category save(Category category);
    Optional<Category> findById(Integer id);
    List<Category> findAll();

    /**
     * Batch lookup for the GraphQL {@code Product.category} DataLoader — a single {@code IN (...)}
     * query for a whole batch of ids, the same "load in bulk, not one-by-one" idea as
     * {@code @EntityGraph} for REST, applied at the resolver layer instead of the repository layer.
     */
    List<Category> findAllByIds(Collection<Integer> ids);
}
