package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resumen de una importación masiva de productos vía CSV")
public record ProductImportResultDTO(

        @Schema(description = "Total de filas de datos leídas del CSV (sin contar el encabezado)", example = "10")
        int totalRows,

        @Schema(description = "Cantidad de productos creados exitosamente", example = "8")
        int imported,

        @Schema(description = "Cantidad de filas que fallaron", example = "2")
        int failed,

        @Schema(description = "Detalle de errores por fila")
        List<ProductImportRowError> errors) {
}
