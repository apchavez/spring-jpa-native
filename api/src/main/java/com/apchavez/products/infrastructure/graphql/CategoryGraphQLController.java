package com.apchavez.products.infrastructure.graphql;

import com.apchavez.products.application.CategoryApplicationService;
import com.apchavez.products.domain.model.Category;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class CategoryGraphQLController {

    private final CategoryApplicationService categoryService;

    public CategoryGraphQLController(CategoryApplicationService categoryService) {
        this.categoryService = categoryService;
    }

    @QueryMapping
    public List<Category> categories() {
        return categoryService.listCategories();
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Category createCategory(@Argument CreateCategoryInput input) {
        return categoryService.createCategory(new Category(null, input.name()));
    }
}
