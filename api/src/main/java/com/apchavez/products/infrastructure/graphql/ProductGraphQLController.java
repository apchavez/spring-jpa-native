package com.apchavez.products.infrastructure.graphql;

import com.apchavez.products.application.ProductApplicationService;
import com.apchavez.products.domain.model.Category;
import com.apchavez.products.domain.model.Product;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.concurrent.CompletableFuture;

/**
 * Same application service the REST {@code ProductController} uses — GraphQL is purely a second
 * transport/query surface over the exact same domain rules, not a parallel implementation of them.
 */
@Controller
public class ProductGraphQLController {

    private final ProductApplicationService productService;

    public ProductGraphQLController(ProductApplicationService productService) {
        this.productService = productService;
    }

    @QueryMapping
    public Product product(@Argument Integer id) {
        return productService.findById(id);
    }

    @QueryMapping
    public ProductPageResult products(@Argument int page, @Argument int size, @Argument boolean activeOnly) {
        var items = activeOnly
                ? productService.listActiveProductsLight(page, size)
                : productService.listInactiveProducts(page, size);
        long total = activeOnly ? productService.countActiveProducts() : productService.countInactiveProducts();
        return new ProductPageResult(items, (int) total, page, size);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product createProduct(@Argument CreateProductInput input) {
        Product product = new Product(null, input.sku(), input.name(), input.description(),
                input.categoryId(), null, input.price(), input.stock(),
                input.active() == null ? Boolean.TRUE : input.active());
        return productService.createProduct(product);
    }

    /**
     * The DataLoader story: GraphQL Java batches every {@code category} key requested by a single
     * query (e.g. all 20 products on a page) and calls {@code CategoryDataLoader} exactly once with
     * the whole batch, instead of resolving this field once per {@code Product} — same fix as
     * {@code @EntityGraph} on the REST side, applied one layer up.
     */
    @SchemaMapping(typeName = "Product", field = "category")
    public CompletableFuture<Category> category(Product product, DataLoader<Integer, Category> dataLoader) {
        return dataLoader.load(product.categoryId());
    }
}
