package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resumen de inventario para verificación end-to-end de la app móvil")
public record InventorySummaryDTO(

        @Schema(description = "Total de productos activos", example = "42")
        long activeProductCount,

        @Schema(description = "Total de productos inactivos", example = "5")
        long inactiveProductCount,

        @Schema(description = "Suma de stock de todos los productos activos", example = "1230")
        long totalActiveStock,

        @Schema(description = "Suma de price * stock de todos los productos activos", example = "48123.75")
        double totalActiveValue,

        @Schema(description = "Desglose por categoría (solo productos activos)")
        List<CategoryStockSummaryDTO> byCategory) {
}
