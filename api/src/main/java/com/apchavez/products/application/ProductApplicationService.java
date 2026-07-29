package com.apchavez.products.application;

import com.apchavez.products.domain.event.ProductEvent;
import com.apchavez.products.domain.exception.ProductDomainException;
import com.apchavez.products.domain.exception.StaleProductException;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.domain.port.ProductEventPublisherPort;
import com.apchavez.products.domain.service.ProductDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.apchavez.products.domain.event.ProductEventType.*;

@Service
public class ProductApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ProductApplicationService.class);

    private final ProductDomainService domainService;
    private final ProductEventPublisherPort eventPublisher;

    public ProductApplicationService(ProductDomainService domainService,
                                      ProductEventPublisherPort eventPublisher) {
        this.domainService = domainService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Product createProduct(Product product) {
        log.info("Crear producto — sku='{}', name='{}'", product.sku(), product.name());
        Product saved = domainService.createProduct(product);
        eventPublisher.publish(ProductEvent.of(PRODUCT_CREATED, saved));
        log.info("Producto creado — id={}", saved.id());
        return saved;
    }

    @Transactional(readOnly = true)
    public Product findById(Integer id) {
        return domainService.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findBySku(String sku) {
        return domainService.findBySku(sku);
    }

    @Transactional(readOnly = true)
    public List<Product> listActiveProducts(int page, int size) {
        return domainService.listActiveProducts(page, size);
    }

    @Transactional(readOnly = true)
    public long countActiveProducts() {
        return domainService.countActiveProducts();
    }

    @Transactional(readOnly = true)
    public List<Product> listActiveProductsLight(int page, int size) {
        return domainService.listActiveProductsLight(page, size);
    }

    @Transactional(readOnly = true)
    public List<Product> listInactiveProducts(int page, int size) {
        return domainService.listInactiveProducts(page, size);
    }

    @Transactional(readOnly = true)
    public long countInactiveProducts() {
        return domainService.countInactiveProducts();
    }

    @Transactional(readOnly = true)
    public List<Product> searchByNamePrefix(String prefix, int page, int size) {
        return domainService.searchByNamePrefix(prefix, page, size);
    }

    @Transactional(readOnly = true)
    public long countByNamePrefix(String prefix) {
        return domainService.countByNamePrefix(prefix);
    }

    @Transactional(readOnly = true)
    public List<Product> searchByCategoryAndPriceRange(Integer categoryId, Double minPrice, Double maxPrice) {
        return domainService.searchByCategoryAndPriceRange(categoryId, minPrice, maxPrice);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return domainService.listAllProducts();
    }

    /**
     * Optimistic-locking conflicts surface here as {@link OptimisticLockingFailureException}
     * (Hibernate throws {@code ObjectOptimisticLockingFailureException}, a subtype, when the
     * {@code @Version} column read at the start of the transaction no longer matches the row at
     * flush time — i.e. another request updated the same product concurrently). Translated to a
     * domain exception so the web layer maps it to a 409, not a generic 500.
     */
    @Transactional
    public Product updateProduct(Integer id, Product updatedData) {
        log.info("Actualizar producto — id={}", id);
        try {
            Product updated = domainService.updateProduct(id, updatedData);
            eventPublisher.publish(ProductEvent.of(PRODUCT_UPDATED, updated));
            log.info("Producto actualizado — id={}", updated.id());
            return updated;
        } catch (OptimisticLockingFailureException e) {
            throw new StaleProductException(id);
        }
    }

    @Transactional
    public void deleteProduct(Integer id) {
        log.info("Eliminar producto — id={}", id);
        Product deleted = domainService.deleteProduct(id);
        eventPublisher.publish(ProductEvent.of(PRODUCT_DELETED, deleted));
        log.info("Producto eliminado — id={}", id);
    }

    /**
     * Bulk CSV import — each row goes through the exact same domain rules as
     * {@link #createProduct(Product)} (unique SKU, category must exist) and publishes the same
     * {@code PRODUCT_CREATED} event on success. Rows are processed independently: a domain rule
     * violation or an unparseable numeric/boolean value on one row is recorded as a row-level
     * error and processing continues with the remaining rows — the whole request only fails for
     * genuinely malformed CSV (handled earlier, in the parser), never for bad row data.
     */
    @Transactional
    public ProductImportResult importProducts(List<ProductImportRow> rows) {
        log.info("Importación CSV de productos — {} filas", rows.size());
        int imported = 0;
        List<ProductImportResult.RowError> errors = new ArrayList<>();
        for (ProductImportRow row : rows) {
            try {
                Product product = toProduct(row);
                Product saved = domainService.createProduct(product);
                eventPublisher.publish(ProductEvent.of(PRODUCT_CREATED, saved));
                imported++;
            } catch (ProductDomainException e) {
                errors.add(new ProductImportResult.RowError(row.rowNumber(), row.sku(), e.getMessage()));
            } catch (IllegalArgumentException e) {
                errors.add(new ProductImportResult.RowError(row.rowNumber(), row.sku(), e.getMessage()));
            }
        }
        log.info("Importación CSV completada — total={}, importados={}, fallidos={}",
                rows.size(), imported, errors.size());
        return new ProductImportResult(rows.size(), imported, errors.size(), errors);
    }

    private Product toProduct(ProductImportRow row) {
        Integer categoryId = parseInt(row.categoryId(), "categoryId");
        Double price = parseDouble(row.price(), "price");
        Integer stock = parseInt(row.stock(), "stock");
        Boolean active = parseBoolean(row.active());
        String description = (row.description() == null || row.description().isBlank())
                ? null : row.description();
        return new Product(null, row.sku(), row.name(), description, categoryId, null, price, stock, active);
    }

    private static Integer parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Valor numérico inválido para '" + fieldName + "': '" + value + "'");
        }
    }

    private static Double parseDouble(String value, String fieldName) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Valor numérico inválido para '" + fieldName + "': '" + value + "'");
        }
    }

    private static Boolean parseBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Valor booleano inválido para 'active': '" + value + "'");
    }
}
