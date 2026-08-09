package com.apchavez.products.infrastructure.graphql;

/** Maps 1:1 to the GraphQL {@code CreateProductInput} input type. */
public record CreateProductInput(
        String sku,
        String name,
        String description,
        Integer categoryId,
        Double price,
        Integer stock,
        Boolean active) {
}
