package com.apchavez.products.application;

import com.apchavez.products.domain.exception.CategoryNotFoundException;
import com.apchavez.products.domain.exception.DuplicateSkuException;
import com.apchavez.products.domain.exception.StaleProductException;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.domain.port.ProductEventPublisherPort;
import com.apchavez.products.domain.service.ProductDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

    @Mock
    private ProductDomainService domainService;

    @Mock
    private ProductEventPublisherPort eventPublisher;

    private ProductApplicationService applicationService;

    private static final Product PRODUCT =
            new Product(1, "SKU-1", "Name", "desc", 1, "Cat", 1.0, 1, true);

    @BeforeEach
    void setUp() {
        applicationService = new ProductApplicationService(domainService, eventPublisher);
    }

    @Test
    void createProduct_publishesEvent() {
        when(domainService.createProduct(any())).thenReturn(PRODUCT);

        Product result = applicationService.createProduct(PRODUCT);

        assertThat(result).isEqualTo(PRODUCT);
        verify(eventPublisher).publish(any());
    }

    @Test
    void updateProduct_publishesEvent() {
        when(domainService.updateProduct(eq(1), any())).thenReturn(PRODUCT);

        Product result = applicationService.updateProduct(1, PRODUCT);

        assertThat(result).isEqualTo(PRODUCT);
        verify(eventPublisher).publish(any());
    }

    @Test
    void updateProduct_translatesOptimisticLockingFailureExceptionToStaleProductException() {
        when(domainService.updateProduct(eq(1), any())).thenThrow(new OptimisticLockingFailureException("stale"));

        assertThatThrownBy(() -> applicationService.updateProduct(1, PRODUCT))
                .isInstanceOf(StaleProductException.class)
                .hasMessageContaining("1");

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void deleteProduct_publishesEvent() {
        when(domainService.deleteProduct(1)).thenReturn(PRODUCT);

        applicationService.deleteProduct(1);

        verify(eventPublisher).publish(any());
    }

    @Test
    void findById_delegatesToDomainService() {
        when(domainService.findById(1)).thenReturn(PRODUCT);

        assertThat(applicationService.findById(1)).isEqualTo(PRODUCT);
    }

    @Test
    void getAllProducts_delegatesToDomainService() {
        when(domainService.listAllProducts()).thenReturn(List.of(PRODUCT));

        assertThat(applicationService.getAllProducts()).containsExactly(PRODUCT);
    }

    @Test
    void importProducts_rowWithNonNumericStock_isRecordedAsRowError() {
        ProductImportRow row = new ProductImportRow(2, "SKU-STOCK", "Name G", "desc", "1", "9.99", "not-a-number", "true");

        ProductImportResult result = applicationService.importProducts(List.of(row));

        assertThat(result.imported()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(2);
        assertThat(result.errors().get(0).message()).contains("stock");
    }

    @Test
    void importProducts_allValidRows_importsAllAndPublishesEventsPerRow() {
        ProductImportRow row1 = new ProductImportRow(2, "SKU-A", "Name A", "desc", "1", "9.99", "5", "true");
        ProductImportRow row2 = new ProductImportRow(3, "SKU-B", "Name B", "", "2", "19.99", "0", "false");
        when(domainService.createProduct(any())).thenReturn(PRODUCT);

        ProductImportResult result = applicationService.importProducts(List.of(row1, row2));

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(domainService, times(2)).createProduct(any());
        verify(eventPublisher, times(2)).publish(any());
    }

    @Test
    void importProducts_rowWithDuplicateSku_isRecordedAsRowErrorAndProcessingContinues() {
        ProductImportRow badRow = new ProductImportRow(2, "SKU-DUP", "Name A", "desc", "1", "9.99", "5", "true");
        ProductImportRow goodRow = new ProductImportRow(3, "SKU-B", "Name B", "desc", "2", "19.99", "0", "false");
        when(domainService.createProduct(any()))
                .thenThrow(new DuplicateSkuException("SKU-DUP"))
                .thenReturn(PRODUCT);

        ProductImportResult result = applicationService.importProducts(List.of(badRow, goodRow));

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(2);
        assertThat(result.errors().get(0).sku()).isEqualTo("SKU-DUP");
        assertThat(result.errors().get(0).message()).contains("SKU-DUP");
        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    void importProducts_rowWithNonExistentCategory_isRecordedAsRowError() {
        ProductImportRow row = new ProductImportRow(2, "SKU-C", "Name C", "desc", "999", "9.99", "5", "true");
        when(domainService.createProduct(any())).thenThrow(new CategoryNotFoundException(999));

        ProductImportResult result = applicationService.importProducts(List.of(row));

        assertThat(result.imported()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errors().get(0).message()).contains("999");
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void importProducts_rowWithNonNumericPrice_isRecordedAsRowErrorWithoutCallingDomainService() {
        ProductImportRow row = new ProductImportRow(2, "SKU-D", "Name D", "desc", "1", "not-a-number", "5", "true");

        ProductImportResult result = applicationService.importProducts(List.of(row));

        assertThat(result.imported()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(2);
        verify(domainService, never()).createProduct(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void importProducts_rowWithInvalidBooleanActive_isRecordedAsRowError() {
        ProductImportRow row = new ProductImportRow(2, "SKU-E", "Name E", "desc", "1", "9.99", "5", "maybe");

        ProductImportResult result = applicationService.importProducts(List.of(row));

        assertThat(result.imported()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errors().get(0).message()).contains("active");
        verify(domainService, never()).createProduct(any());
    }

    @Test
    void importProducts_blankDescription_isMappedToNull() {
        ProductImportRow row = new ProductImportRow(2, "SKU-F", "Name F", "   ", "1", "9.99", "5", "true");
        when(domainService.createProduct(any())).thenReturn(PRODUCT);

        applicationService.importProducts(List.of(row));

        org.mockito.ArgumentCaptor<Product> captor = org.mockito.ArgumentCaptor.forClass(Product.class);
        verify(domainService).createProduct(captor.capture());
        assertThat(captor.getValue().description()).isNull();
    }

    @Test
    void importProducts_emptyList_returnsZeroedResult() {
        ProductImportResult result = applicationService.importProducts(List.of());

        assertThat(result.totalRows()).isZero();
        assertThat(result.imported()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(domainService, never()).createProduct(any());
    }
}
