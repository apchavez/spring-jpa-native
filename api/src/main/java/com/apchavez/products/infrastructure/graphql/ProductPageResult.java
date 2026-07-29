package com.apchavez.products.infrastructure.graphql;

import com.apchavez.products.domain.model.Product;

import java.util.List;

/** Maps 1:1 to the GraphQL {@code ProductPage} type. */
public record ProductPageResult(List<Product> items, int totalCount, int page, int size) {
}
