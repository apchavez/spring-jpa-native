package com.apchavez.products.infrastructure.graphql;

import com.apchavez.products.domain.model.Category;
import com.apchavez.products.domain.port.CategoryRepositoryPort;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registers the {@code category} batch loader: GraphQL Java collects every {@code Product.category}
 * key requested across a single query into one batch before calling this loader once, instead of
 * once per product — the resolver-layer equivalent of the {@code @EntityGraph} fix used by the REST
 * side of this same repo. See {@code ProductNPlusOneGraphQLTest} for the query-count proof.
 *
 * The JPA lookup is blocking, so it runs on {@code boundedElastic} rather than the Netty/servlet
 * event loop that Spring for GraphQL's batch-loading machinery itself runs on.
 */
@Component
public class CategoryDataLoader {

    private final CategoryRepositoryPort categoryRepositoryPort;

    public CategoryDataLoader(CategoryRepositoryPort categoryRepositoryPort, BatchLoaderRegistry registry) {
        this.categoryRepositoryPort = categoryRepositoryPort;
        registry.forTypePair(Integer.class, Category.class)
                .registerMappedBatchLoader((ids, env) -> loadBatch(ids));
    }

    private Mono<Map<Integer, Category>> loadBatch(Set<Integer> ids) {
        return Mono.fromCallable(() -> categoryRepositoryPort.findAllByIds(ids).stream()
                        .collect(Collectors.toMap(Category::id, Function.identity())))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
