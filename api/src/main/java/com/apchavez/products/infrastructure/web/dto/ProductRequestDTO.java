package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear un producto")
public record ProductRequestDTO(

        @NotBlank(message = "El SKU es requerido")
        @Size(max = 64, message = "El SKU no puede superar 64 caracteres")
        @Schema(description = "SKU único del producto", example = "SKU-001")
        String sku,

        @NotBlank(message = "El nombre es requerido")
        @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
        @Schema(description = "Nombre del producto", example = "Wireless Mouse")
        String name,

        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        @Schema(description = "Descripción del producto", example = "Mouse inalámbrico ergonómico")
        String description,

        @NotNull(message = "La categoría (categoryId) es requerida")
        @Positive(message = "categoryId debe ser mayor que cero")
        @Schema(description = "ID de la categoría (relación JPA @ManyToOne)", example = "1")
        Integer categoryId,

        @NotNull(message = "El precio es requerido")
        @PositiveOrZero(message = "El precio debe ser mayor o igual a cero")
        @Schema(description = "Precio del producto", example = "29.99")
        Double price,

        @NotNull(message = "El stock es requerido")
        @PositiveOrZero(message = "El stock debe ser mayor o igual a cero")
        @Schema(description = "Cantidad en stock", example = "150")
        Integer stock,

        @NotNull(message = "El estado activo es requerido")
        @Schema(description = "Indica si el producto está activo", example = "true")
        Boolean active) {
}
