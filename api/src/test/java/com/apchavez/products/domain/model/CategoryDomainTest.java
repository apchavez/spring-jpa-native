package com.apchavez.products.domain.model;

import com.apchavez.products.domain.exception.InvalidCategoryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryDomainTest {

    @Test
    void validCategory_doesNotThrow() {
        assertThatCode(() -> new Category(1, "Electrónica")).doesNotThrowAnyException();
    }

    @Test
    void blankName_throws() {
        assertThatThrownBy(() -> new Category(1, " "))
                .isInstanceOf(InvalidCategoryException.class);
    }

    @Test
    void nameTooLong_throws() {
        assertThatThrownBy(() -> new Category(1, "x".repeat(101)))
                .isInstanceOf(InvalidCategoryException.class);
    }
}
