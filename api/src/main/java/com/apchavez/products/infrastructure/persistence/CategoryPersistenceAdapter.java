package com.apchavez.products.infrastructure.persistence;

import com.apchavez.products.domain.model.Category;
import com.apchavez.products.domain.port.CategoryRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class CategoryPersistenceAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository jpaRepository;

    public CategoryPersistenceAdapter(CategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Category save(Category category) {
        CategoryEntity saved = jpaRepository.save(new CategoryEntity(category.name()));
        return toDomain(saved);
    }

    @Override
    public Optional<Category> findById(Integer id) {
        return jpaRepository.findById(id).map(CategoryPersistenceAdapter::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return jpaRepository.findAll().stream().map(CategoryPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<Category> findAllByIds(Collection<Integer> ids) {
        return jpaRepository.findAllById(ids).stream().map(CategoryPersistenceAdapter::toDomain).toList();
    }

    static Category toDomain(CategoryEntity entity) {
        return new Category(entity.getId(), entity.getName());
    }
}
