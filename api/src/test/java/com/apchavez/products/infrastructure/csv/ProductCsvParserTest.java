package com.apchavez.products.infrastructure.csv;

import com.apchavez.products.application.ProductImportRow;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductCsvParserTest {

    private final ProductCsvParser parser = new ProductCsvParser();

    @Test
    void parse_validCsv_returnsOneRowPerDataLine() {
        String csv = """
                sku,name,description,categoryId,price,stock,active
                SKU-1,Mouse,Wireless mouse,1,29.99,150,true
                SKU-2,Keyboard,,2,79.99,10,false
                """;
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        List<ProductImportRow> rows = parser.parse(file);

        assertThat(rows).hasSize(2);
        ProductImportRow first = rows.get(0);
        assertThat(first.rowNumber()).isEqualTo(2);
        assertThat(first.sku()).isEqualTo("SKU-1");
        assertThat(first.name()).isEqualTo("Mouse");
        assertThat(first.description()).isEqualTo("Wireless mouse");
        assertThat(first.categoryId()).isEqualTo("1");
        assertThat(first.price()).isEqualTo("29.99");
        assertThat(first.stock()).isEqualTo("150");
        assertThat(first.active()).isEqualTo("true");

        ProductImportRow second = rows.get(1);
        assertThat(second.rowNumber()).isEqualTo(3);
        assertThat(second.description()).isEmpty();
    }

    @Test
    void parse_quotedDescriptionContainingComma_isKeptAsOneField() {
        String csv = """
                sku,name,description,categoryId,price,stock,active
                SKU-1,Mouse,"Wireless, ergonomic mouse",1,29.99,150,true
                """;
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        List<ProductImportRow> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).description()).isEqualTo("Wireless, ergonomic mouse");
    }

    @Test
    void parse_blankLinesAreSkipped() {
        String csv = """
                sku,name,description,categoryId,price,stock,active
                SKU-1,Mouse,desc,1,29.99,150,true

                SKU-2,Keyboard,desc,1,79.99,10,true
                """;
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        List<ProductImportRow> rows = parser.parse(file);

        assertThat(rows).hasSize(2);
    }

    @Test
    void parse_emptyFile_throwsProductCsvFormatException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(ProductCsvFormatException.class);
    }

    @Test
    void parse_nullFile_throwsProductCsvFormatException() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(ProductCsvFormatException.class);
    }

    @Test
    void parse_wrongHeader_throwsProductCsvFormatException() {
        String csv = """
                id,name,price
                1,Mouse,29.99
                """;
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.parse(file))
                .isInstanceOf(ProductCsvFormatException.class)
                .hasMessageContaining("Encabezado");
    }

    @Test
    void parse_headerOnlyIsCaseInsensitive() {
        String csv = "SKU,Name,Description,CategoryId,Price,Stock,Active\n";
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        List<ProductImportRow> rows = parser.parse(file);

        assertThat(rows).isEmpty();
    }

    @Test
    void parse_dataRowWithMissingTrailingColumns_yieldsEmptyStringsForMissingFields() {
        String csv = """
                sku,name,description,categoryId,price,stock,active
                SKU-1,Mouse,desc,1
                """;
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        List<ProductImportRow> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).price()).isEmpty();
        assertThat(rows.get(0).stock()).isEmpty();
        assertThat(rows.get(0).active()).isEmpty();
    }
}
