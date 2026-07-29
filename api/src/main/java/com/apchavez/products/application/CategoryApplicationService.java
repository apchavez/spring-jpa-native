package com.apchavez.products.application;

import com.apchavez.products.domain.model.Category;
import com.apchavez.products.domain.port.CategoryRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryApplicationService {

    private final CategoryRepositoryPort repositoryPort;

    public CategoryApplicationService(CategoryRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Transactional
    public Category createCategory(Category category) {
        return repositoryPort.save(category);
    }

    public List<Category> listCategories() {
        return repositoryPort.findAll();
    }
}
