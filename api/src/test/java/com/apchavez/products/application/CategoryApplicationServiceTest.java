package com.apchavez.products.application;

import com.apchavez.products.domain.model.Category;
import com.apchavez.products.domain.port.CategoryRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryApplicationServiceTest {

    @Mock
    private CategoryRepositoryPort repositoryPort;

    private CategoryApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new CategoryApplicationService(repositoryPort);
    }

    @Test
    void createCategory_delegatesToRepositoryPort() {
        Category category = new Category(null, "Electrónica");
        Category saved = new Category(1, "Electrónica");
        when(repositoryPort.save(category)).thenReturn(saved);

        assertThat(applicationService.createCategory(category)).isEqualTo(saved);
        verify(repositoryPort).save(category);
    }

    @Test
    void listCategories_delegatesToRepositoryPort() {
        Category category = new Category(1, "Electrónica");
        when(repositoryPort.findAll()).thenReturn(List.of(category));

        assertThat(applicationService.listCategories()).containsExactly(category);
    }
}
