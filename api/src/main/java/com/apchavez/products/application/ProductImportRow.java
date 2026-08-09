package com.apchavez.products.application;

/**
 * One CSV data row, produced by the infrastructure CSV parser and consumed by
 * {@link ProductApplicationService#importProducts(java.util.List)}. Fields are kept as raw
 * strings — numeric/boolean parsing happens per-row in the application service so a single
 * malformed value (e.g. a non-numeric price) becomes a row-level error instead of failing the
 * whole import.
 */
public record ProductImportRow(
        int rowNumber,
        String sku,
        String name,
        String description,
        String categoryId,
        String price,
        String stock,
        String active) {
}
