package com.apchavez.products.infrastructure.web;

import com.apchavez.products.AbstractIntegrationTest;
import com.apchavez.products.infrastructure.config.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ProductControllerIntegrationTest extends AbstractIntegrationTest {

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
    void fullLifecycle_createReadUpdateDelete_withRealCategoryRelationship() throws Exception {
        // Create a category first — Product.categoryId must reference a real row.
        String categoryBody = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Controller Test Category"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int categoryId = com.jayway.jsonpath.JsonPath.read(categoryBody, "$.id");

        // Create a product in that category.
        String createBody = """
                {"sku":"CTRL-1","name":"Controller Product","description":"d","categoryId":%d,"price":9.99,"stock":5,"active":true}
                """.formatted(categoryId);
        String productBody = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId", org.hamcrest.Matchers.is(categoryId)))
                .andExpect(jsonPath("$.categoryName", org.hamcrest.Matchers.is("Controller Test Category")))
                .andReturn().getResponse().getContentAsString();
        int productId = com.jayway.jsonpath.JsonPath.read(productBody, "$.id");

        // Read it back by ID — categoryName must be resolved via the @ManyToOne relationship.
        mockMvc.perform(get("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName", org.hamcrest.Matchers.is("Controller Test Category")));

        // Appears in the active listing.
        mockMvc.perform(get("/api/v1/products/active")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.sku == 'CTRL-1')]").exists());

        // Update it.
        String updateBody = """
                {"sku":"CTRL-1","name":"Updated Name","description":"d2","categoryId":%d,"price":19.99,"stock":3,"active":true}
                """.formatted(categoryId);
        mockMvc.perform(put("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", org.hamcrest.Matchers.is("Updated Name")));

        // A regular USER cannot write.
        mockMvc.perform(put("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());

        // Delete it.
        mockMvc.perform(delete("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_withNonExistentCategoryId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"CTRL-BAD-CAT","name":"n","categoryId":999999,"price":1.0,"stock":1,"active":true}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void importProducts_validCsv_withMixOfValidAndInvalidRows_reportsPerRowResults() throws Exception {
        String categoryBody = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"CSV Import Category"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int categoryId = com.jayway.jsonpath.JsonPath.read(categoryBody, "$.id");

        String csv = """
                sku,name,description,categoryId,price,stock,active
                CSV-1,Imported Product 1,desc 1,%d,9.99,10,true
                CSV-2,Imported Product 2,desc 2,999999,19.99,5,true
                CSV-1,Duplicate SKU,desc,%d,9.99,10,true
                CSV-3,Bad Price,desc,%d,not-a-number,5,true
                """.formatted(categoryId, categoryId, categoryId);
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/products/import").file(file)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows", is(4)))
                .andExpect(jsonPath("$.imported", is(1)))
                .andExpect(jsonPath("$.failed", is(3)))
                .andExpect(jsonPath("$.errors.length()", is(3)));

        mockMvc.perform(get("/api/v1/products/sku/CSV-1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Imported Product 1")));
    }

    @Test
    void importProducts_malformedHeader_returns400ForWholeRequest() throws Exception {
        String csv = "id,name,price\n1,Mouse,9.99\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/products/import").file(file)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importProducts_asRegularUser_returns403() throws Exception {
        String csv = "sku,name,description,categoryId,price,stock,active\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/products/import").file(file)
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void importProducts_withoutToken_returns401() throws Exception {
        String csv = "sku,name,description,categoryId,price,stock,active\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/products/import").file(file))
                .andExpect(status().isUnauthorized());
    }
}
