package com.apchavez.products.infrastructure.web;

import com.apchavez.products.AbstractIntegrationTest;
import com.apchavez.products.infrastructure.config.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ReportControllerIntegrationTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String adminToken() {
        return jwtService.generateToken("admin", List.of("ADMIN"));
    }

    private String userToken() {
        return jwtService.generateToken("user", List.of("USER"));
    }

    @Test
    void inventorySummary_afterCreatingActiveProduct_reflectsItInTotalsAndCategoryBreakdown() throws Exception {
        String categoryBody = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Report Test Category"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int categoryId = com.jayway.jsonpath.JsonPath.read(categoryBody, "$.id");

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"REPORT-CTRL-1","name":"Report Product","description":"d","categoryId":%d,"price":10.0,"stock":4,"active":true}
                                """.formatted(categoryId)))
                .andExpect(status().isCreated());

        String responseBody = mockMvc.perform(get("/api/v1/reports/inventory-summary")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProductCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalActiveStock", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.byCategory[?(@.categoryId == " + categoryId + ")]").exists())
                .andReturn().getResponse().getContentAsString();

        java.util.List<Integer> matchingStocks = com.jayway.jsonpath.JsonPath.read(
                responseBody, "$.byCategory[?(@.categoryId == " + categoryId + ")].totalStock");
        org.assertj.core.api.Assertions.assertThat(matchingStocks).containsExactly(4);
    }

    @Test
    void inventorySummary_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/reports/inventory-summary"))
                .andExpect(status().isUnauthorized());
    }
}
