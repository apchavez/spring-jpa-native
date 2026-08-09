package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error de importación para una fila específica del CSV")
public record ProductImportRowError(

        @Schema(description = "Número de línea del CSV (1 = encabezado)", example = "3")
        int rowNumber,

        @Schema(description = "SKU de la fila (puede estar vacío si el valor era inválido)", example = "SKU-003")
        String sku,

        @Schema(description = "Motivo del fallo", example = "Ya existe un producto con el SKU: SKU-003")
        String message) {
}
