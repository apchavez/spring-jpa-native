package com.apchavez.products.infrastructure.csv;

/**
 * Thrown for structural CSV problems only — an unreadable/empty file or a missing/wrong header —
 * i.e. the cases where the whole import request should fail with a 400, as opposed to a single
 * bad data row (which is recorded as a row-level error by
 * {@code ProductApplicationService.importProducts} instead).
 */
public class ProductCsvFormatException extends RuntimeException {
    public ProductCsvFormatException(String message) {
        super(message);
    }
}
