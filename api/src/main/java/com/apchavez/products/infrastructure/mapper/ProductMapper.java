package com.apchavez.products.infrastructure.mapper;

import com.apchavez.products.application.ProductImportResult;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.infrastructure.web.dto.ProductImportResultDTO;
import com.apchavez.products.infrastructure.web.dto.ProductImportRowError;
import com.apchavez.products.infrastructure.web.dto.ProductRequestDTO;
import com.apchavez.products.infrastructure.web.dto.ProductResponseDTO;
import com.apchavez.products.infrastructure.web.dto.ProductUpdateRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDomain(ProductRequestDTO dto) {
        return new Product(
                null,
                dto.sku(),
                dto.name(),
                dto.description(),
                dto.categoryId(),
                null,
                dto.price(),
                dto.stock(),
                dto.active());
    }

    public Product toDomain(ProductUpdateRequestDTO dto) {
        return new Product(
                null,
                dto.sku(),
                dto.name(),
                dto.description(),
                dto.categoryId(),
                null,
                dto.price(),
                dto.stock(),
                dto.active());
    }

    public ProductResponseDTO toResponseDTO(Product product) {
        return new ProductResponseDTO(
                product.id(),
                product.sku(),
                product.name(),
                product.description(),
                product.categoryId(),
                product.categoryName(),
                product.price(),
                product.stock(),
                product.active());
    }

    public ProductImportResultDTO toDTO(ProductImportResult result) {
        return new ProductImportResultDTO(
                result.totalRows(),
                result.imported(),
                result.failed(),
                result.errors().stream()
                        .map(e -> new ProductImportRowError(e.rowNumber(), e.sku(), e.message()))
                        .toList());
    }
}
