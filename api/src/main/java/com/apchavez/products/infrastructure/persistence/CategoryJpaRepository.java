package com.apchavez.products.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Integer> {
}
