package com.apchavez.products.domain.exception;

public class InvalidCategoryException extends ProductDomainException {
    public InvalidCategoryException(String message) {
        super(message);
    }
}
