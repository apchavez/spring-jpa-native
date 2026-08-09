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
 * Mirrors the REST security contract: {@code createProduct}/{@code createCategory} mutations
 * require role ADMIN (enforced by {@code @PreAuthorize} on the resolvers, since URL-based matching
 * on {@code POST /graphql} can't distinguish a query body from a mutation body). A plain USER token
 * must be rejected the same way a non-admin REST write is.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphQLSecurityTest extends AbstractIntegrationTest {

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

    private static final String CREATE_CATEGORY_MUTATION = """
            mutation {
              createCategory(input: { name: "Not Allowed" }) { id }
            }
            """;

    private GraphQlTester testerWithToken(String token) {
        WebTestClient.Builder client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + "/graphql");
        if (token != null) {
            client = client.defaultHeader("Authorization", "Bearer " + token);
        }
        return HttpGraphQlTester.create(client.build());
    }

    @Test
    void createCategory_withUserRole_isForbidden() {
        String userToken = jwtService.generateToken("user", List.of("USER"));

        testerWithToken(userToken).document(CREATE_CATEGORY_MUTATION).execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    // No Authorization header at all is rejected by Spring Security's filter chain BEFORE the
    // request reaches the GraphQL handler — a raw HTTP 401, not a GraphQL error payload inside a
    // 200 response (that's what the @PreAuthorize/wrong-role case above produces instead). The
    // GraphQlTester wrapper asserts HTTP 200 internally, so this case needs the plain WebTestClient
    // to observe the actual status code.
    @Test
    void createCategory_withoutToken_isUnauthorized() {
        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + "/graphql")
                .build()
                .post().uri("")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("query", CREATE_CATEGORY_MUTATION))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
