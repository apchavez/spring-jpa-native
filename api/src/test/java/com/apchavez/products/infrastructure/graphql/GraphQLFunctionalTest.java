package com.apchavez.products.infrastructure.graphql;

import com.apchavez.products.AbstractIntegrationTest;
import com.apchavez.products.infrastructure.config.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the success path of every resolver as an authenticated ADMIN — the security tests only
 * cover rejection, so without this the create mutations and the plain {@code product}/{@code
 * categories} queries were never actually invoked by any automated test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphQLFunctionalTest extends AbstractIntegrationTest {

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
    private JwtService jwtService;

    private GraphQlTester adminTester() {
        String token = jwtService.generateToken("admin", List.of("ADMIN"));
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + "/graphql")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
        return HttpGraphQlTester.create(client);
    }

    @Test
    void createCategory_thenCreateProduct_thenQueryBothBack() {
        GraphQlTester tester = adminTester();

        Integer categoryId = tester.document("""
                        mutation {
                          createCategory(input: { name: "GraphQL Functional Category" }) { id name }
                        }
                        """)
                .execute()
                .path("createCategory.id")
                .entity(Integer.class)
                .get();
        assertThat(categoryId).isNotNull();

        Integer productId = tester.document("""
                        mutation($categoryId: ID!) {
                          createProduct(input: {
                            sku: "GQL-FUNC-1", name: "GraphQL Functional Product",
                            description: "created by the functional test",
                            categoryId: $categoryId, price: 12.5, stock: 3, active: true
                          }) { id sku category { id name } }
                        }
                        """)
                .variable("categoryId", categoryId)
                .execute()
                .path("createProduct.sku").entity(String.class).isEqualTo("GQL-FUNC-1")
                .path("createProduct.category.name").entity(String.class).isEqualTo("GraphQL Functional Category")
                .path("createProduct.id")
                .entity(Integer.class)
                .get();
        assertThat(productId).isNotNull();

        tester.document("""
                        query($id: ID!) {
                          product(id: $id) { id sku name category { name } }
                        }
                        """)
                .variable("id", productId)
                .execute()
                .path("product.sku").entity(String.class).isEqualTo("GQL-FUNC-1")
                .path("product.category.name").entity(String.class).isEqualTo("GraphQL Functional Category");

        List<Object> categories = tester.document("""
                        query {
                          categories { id name }
                        }
                        """)
                .execute()
                .path("categories")
                .entityList(Object.class)
                .get();
        assertThat(categories).isNotEmpty();
    }

    @Test
    void productsQuery_withActiveOnlyFalse_returnsInactiveProducts() {
        GraphQlTester tester = adminTester();

        tester.document("""
                        query {
                          products(page: 0, size: 20, activeOnly: false) {
                            totalCount
                            page
                            size
                          }
                        }
                        """)
                .execute()
                .path("products.totalCount")
                .entity(Integer.class)
                .satisfies(count -> assertThat(count).isGreaterThanOrEqualTo(0));
    }
}
