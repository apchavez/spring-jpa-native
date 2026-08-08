package com.apchavez.products.domain.model;

import com.apchavez.products.domain.exception.InvalidProductException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProductDomainTest {

    @Test
    void validProduct_doesNotThrow() {
        assertThatCode(() -> new Product(1, "SKU-1", "Name", "desc", 1, "Cat", 1.0, 1, true))
                .doesNotThrowAnyException();
    }

    @Test
    void blankSku_throws() {
        assertThatThrownBy(() -> new Product(1, " ", "Name", "desc", 1, "Cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void skuTooLong_throws() {
        assertThatThrownBy(() -> new Product(1, "x".repeat(65), "Name", "desc", 1, "Cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void blankName_throws() {
        assertThatThrownBy(() -> new Product(1, "SKU-1", " ", "desc", 1, "Cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void nameTooLong_throws() {
        assertThatThrownBy(() -> new Product(1, "SKU-1", "x".repeat(201), "desc", 1, "Cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void descriptionTooLong_throws() {
        assertThatThrownBy(() -> new Product(1, "SKU-1", "Name", "x".repeat(1001), 1, "Cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void nullCategoryId_throws() {
        assertThatThrownBy(() -> new Product(1, "SKU-1", "Name", "desc", null, "Cat", 1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void nullPrice_throws() {
        assertThatThrownBy(() -> new Product(1, "SKU-1", "Name", "desc", 1, "Cat", null, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void negativePrice_throws() {
        assertThatThrownBy(() -> new Product(1, "SKU-1", "Name", "desc", 1, "Cat", -1.0, 1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void nullStock_throws() {
        assertThatThrownBy(() -> new Product(1, "SKU-1", "Name", "desc", 1, "Cat", 1.0, null, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void negativeStock_throws() {
        assertThatThrownBy(() -> new Product(1, "SKU-1", "Name", "desc", 1, "Cat", 1.0, -1, true))
                .isInstanceOf(InvalidProductException.class);
    }

    @Test
    void nullActive_throws() {
        assertThatThrownBy(() -> new Product(1, "SKU-1", "Name", "desc", 1, "Cat", 1.0, 1, null))
                .isInstanceOf(InvalidProductException.class);
    }
}
