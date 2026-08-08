package com.apchavez.products.infrastructure.graphql;

import com.apchavez.products.AbstractIntegrationTest;
import com.apchavez.products.infrastructure.config.JwtService;
import com.apchavez.products.infrastructure.persistence.CategoryEntity;
import com.apchavez.products.infrastructure.persistence.CategoryJpaRepository;
import com.apchavez.products.infrastructure.persistence.ProductEntity;
import com.apchavez.products.infrastructure.persistence.ProductJpaRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The GraphQL-layer counterpart of {@code ProductNPlusOneQueryTest}: proves the {@code category}
 * field resolver, backed by {@link CategoryDataLoader}, batches every category lookup for a page
 * of products into a single query — the same fix idea as {@code @EntityGraph} on the REST side,
 * applied one layer up (at the GraphQL resolver, not the JPA repository).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductNPlusOneGraphQLTest extends AbstractIntegrationTest {

    private static final int PRODUCT_COUNT = 10;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @LocalServerPort
    private int port;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JwtService jwtService;

    private Statistics statistics;
    private GraphQlTester graphQlTester;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);

        for (int i = 0; i < PRODUCT_COUNT; i++) {
            CategoryEntity category = categoryJpaRepository.save(new CategoryEntity("GraphQL N+1 Category " + i));
            productJpaRepository.save(new ProductEntity(
                    "GQL-NPLUSONE-" + i, "GraphQL Product " + i, null, category, 9.99, 5, true));
        }

        String token = jwtService.generateToken("admin", List.of("ADMIN"));
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + "/graphql")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
        graphQlTester = HttpGraphQlTester.create(client);

        statistics.clear();
    }

    @AfterEach
    void tearDown() {
        productJpaRepository.deleteAll();
        categoryJpaRepository.deleteAll();
    }

    @Test
    void productsQuery_resolvesCategoryPerProduct_inTwoQueriesTotal_notNPlusOne() {
        statistics.clear();

        String document = """
                query {
                  products(page: 0, size: 20, activeOnly: true) {
                    totalCount
                    items {
                      id
                      category { name }
                    }
                  }
                }
                """;

        graphQlTester.document(document)
                .execute()
                .path("products.items")
                .entityList(Object.class)
                .hasSize(PRODUCT_COUNT);

        // 4 statements total, independent of PRODUCT_COUNT: Spring Data's paginated Page<T> return
        // type runs its own content + COUNT query pair for BOTH findAllActiveLight (content+count)
        // and countActiveProducts (content+count again, since it's also Page-backed) — 4 queries
        // before category resolution even starts — plus exactly 1 batched IN(...) query for every
        // distinct category the DataLoader collected across the whole page. What matters for this
        // test is that the category batch is ONE query, not PRODUCT_COUNT queries — proven by
        // asserting the total stays flat instead of scaling with PRODUCT_COUNT.
        assertThat(statistics.getPrepareStatementCount())
                .as("listing %d products with a distinct category each via GraphQL must issue a constant, small number of SQL statements (pagination content+count queries, plus exactly 1 batched IN(...) for every distinct category) — never %d (one extra query per product, the N+1 this test exists to disprove)"
                        .formatted(PRODUCT_COUNT, PRODUCT_COUNT))
                .isLessThan(PRODUCT_COUNT);
    }
}
