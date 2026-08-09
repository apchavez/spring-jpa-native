package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de una categoría en la respuesta")
public record CategoryResponseDTO(
        @Schema(description = "ID de la categoría", example = "1") Integer id,
        @Schema(description = "Nombre de la categoría", example = "Electrónica") String name) {
}
