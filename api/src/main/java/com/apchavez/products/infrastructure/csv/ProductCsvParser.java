package com.apchavez.products.infrastructure.csv;

import com.apchavez.products.application.ProductImportRow;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Hand-rolled CSV parser for the product bulk-import endpoint — deliberately simple (no new
 * dependency such as commons-csv/opencsv) since the demo CSV is comma-delimited with an optional
 * minimal quoted-field escape, not a full RFC 4180 implementation (no embedded newlines inside
 * quoted fields).
 *
 * <p>Only file-level problems (unreadable file, missing/blank file, wrong header) throw
 * {@link ProductCsvFormatException} and fail the whole request. Per-row data problems (wrong
 * column count, bad numeric/boolean values) are surfaced as {@link ProductImportRow} fields that
 * fail later, in {@code ProductApplicationService.importProducts}, as row-level errors — this
 * parser never rejects the request because of a single bad data row.
 */
@Component
public class ProductCsvParser {

    private static final List<String> EXPECTED_HEADERS =
            List.of("sku", "name", "description", "categoryid", "price", "stock", "active");

    public List<ProductImportRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProductCsvFormatException("El archivo CSV está vacío o no fue enviado");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ProductCsvFormatException("El archivo CSV está vacío");
            }
            validateHeader(splitLine(headerLine));

            List<ProductImportRow> rows = new ArrayList<>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = splitLine(line);
                rows.add(new ProductImportRow(
                        rowNumber,
                        field(fields, 0),
                        field(fields, 1),
                        field(fields, 2),
                        field(fields, 3),
                        field(fields, 4),
                        field(fields, 5),
                        field(fields, 6)));
            }
            return rows;
        } catch (IOException e) {
            throw new ProductCsvFormatException("No se pudo leer el archivo CSV: " + e.getMessage());
        }
    }

    private static String field(String[] fields, int index) {
        return index < fields.length ? fields[index].trim() : "";
    }

    private static void validateHeader(String[] headers) {
        if (headers.length != EXPECTED_HEADERS.size()) {
            throw new ProductCsvFormatException(
                    "Encabezado CSV inválido: se esperaban las columnas " + EXPECTED_HEADERS
                            + " pero se encontraron " + headers.length + " columnas");
        }
        for (int i = 0; i < headers.length; i++) {
            if (!headers[i].trim().equalsIgnoreCase(EXPECTED_HEADERS.get(i))) {
                throw new ProductCsvFormatException(
                        "Encabezado CSV inválido: se esperaba la columna '" + EXPECTED_HEADERS.get(i)
                                + "' en la posición " + (i + 1) + " pero se encontró '" + headers[i].trim() + "'");
            }
        }
    }

    /**
     * Minimal CSV line splitter: handles double-quoted fields (so a quoted description may
     * contain commas) and {@code ""} as an escaped quote inside a quoted field.
     */
    private static String[] splitLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }
}
