package com.apchavez.products.application;

import java.util.List;

/** Outcome of a CSV bulk import — one row succeeds or fails independently of the others. */
public record ProductImportResult(
        int totalRows,
        int imported,
        int failed,
        List<RowError> errors) {

    public record RowError(int rowNumber, String sku, String message) {
    }
}
