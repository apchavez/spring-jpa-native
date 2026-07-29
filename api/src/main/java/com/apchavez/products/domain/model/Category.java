package com.apchavez.products.domain.model;

import com.apchavez.products.domain.exception.InvalidCategoryException;

public record Category(Integer id, String name) {

    public Category {
        if (name == null || name.isBlank()) {
            throw new InvalidCategoryException("El nombre de la categoría no puede estar vacío");
        }
        if (name.length() > 100) {
            throw new InvalidCategoryException("El nombre de la categoría no puede superar los 100 caracteres");
        }
    }
}
