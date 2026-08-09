package com.apchavez.products.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear una categoría")
public record CategoryRequestDTO(
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        @Schema(description = "Nombre único de la categoría", example = "Electrónica")
        String name) {
}
