package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Agregado de stock/valor por categoría, solo productos activos")
public record CategoryStockSummaryDTO(

        @Schema(description = "ID de la categoría", example = "1")
        Integer categoryId,

        @Schema(description = "Nombre de la categoría", example = "Electronics")
        String categoryName,

        @Schema(description = "Cantidad de productos activos en la categoría", example = "12")
        long productCount,

        @Schema(description = "Suma de stock de productos activos en la categoría", example = "340")
        long totalStock,

        @Schema(description = "Suma de price * stock de productos activos en la categoría", example = "15230.50")
        double totalValue) {
}
