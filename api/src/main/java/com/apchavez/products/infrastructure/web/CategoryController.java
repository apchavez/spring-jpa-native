package com.apchavez.products.infrastructure.web;

import com.apchavez.products.application.CategoryApplicationService;
import com.apchavez.products.domain.model.Category;
import com.apchavez.products.infrastructure.web.dto.CategoryRequestDTO;
import com.apchavez.products.infrastructure.web.dto.CategoryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Categorías de producto — el lado @OneToMany de la relación @ManyToOne de Product")
public class CategoryController {

    private final CategoryApplicationService applicationService;

    public CategoryController(CategoryApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @Operation(summary = "Crear categoría (ADMIN)")
    public ResponseEntity<CategoryResponseDTO> create(@Valid @RequestBody CategoryRequestDTO dto) {
        Category saved = applicationService.createCategory(new Category(null, dto.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @GetMapping
    @Operation(summary = "Listar categorías")
    public List<CategoryResponseDTO> list() {
        return applicationService.listCategories().stream().map(CategoryController::toDto).toList();
    }

    private static CategoryResponseDTO toDto(Category category) {
        return new CategoryResponseDTO(category.id(), category.name());
    }
}
