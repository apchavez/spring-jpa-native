package com.apchavez.products.domain.exception;

public class CategoryNotFoundException extends ProductDomainException {
    public CategoryNotFoundException(Integer id) {
        super("No se encontró una categoría con el ID: " + id);
    }
}
