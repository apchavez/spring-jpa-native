package com.apchavez.products.infrastructure.web.exception;

import com.apchavez.products.domain.exception.CategoryNotFoundException;
import com.apchavez.products.domain.exception.DuplicateSkuException;
import com.apchavez.products.domain.exception.InvalidCategoryException;
import com.apchavez.products.domain.exception.InvalidProductException;
import com.apchavez.products.domain.exception.ProductNotFoundException;
import com.apchavez.products.domain.exception.StaleProductException;
import com.apchavez.products.infrastructure.auth.InvalidCredentialsException;
import com.apchavez.products.infrastructure.csv.ProductCsvFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.core.MethodParameter;
import org.springframework.context.MessageSourceResolvable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNoEncontrado_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleNoEncontrado(new ProductNotFoundException(1));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().mensaje()).contains("1");
    }

    @Test
    void handleCategoryNotFound_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleCategoryNotFound(new CategoryNotFoundException(5));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleDuplicateSku_retorna409() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateSku(new DuplicateSkuException("SKU-1"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleStaleProduct_retorna409() {
        ResponseEntity<ErrorResponse> response = handler.handleStaleProduct(new StaleProductException(2));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleInvalido_conInvalidProductException_retorna422() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalido(new InvalidProductException("precio inválido"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void handleInvalido_conInvalidCategoryException_retorna422() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalido(new InvalidCategoryException("nombre inválido"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void handleProductCsvFormat_retorna400() {
        ResponseEntity<ErrorResponse> response = handler.handleProductCsvFormat(new ProductCsvFormatException("columna faltante"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleInvalidCredentials_retorna401() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(new InvalidCredentialsException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleValidation_retorna400ConErroresDeCampo() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("product", "name", "no debe estar vacío");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errores()).hasSize(1);
    }

    @Test
    void handleMethodValidation_retorna400() throws NoSuchMethodException {
        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        MessageSourceResolvable error = mock(MessageSourceResolvable.class);
        when(error.getDefaultMessage()).thenReturn("debe ser positivo");
        doReturn(List.of(error)).when(ex).getAllErrors();
        Method method = Object.class.getMethod("toString");
        when(ex.getMethod()).thenReturn(method);

        ResponseEntity<ErrorResponse> response = handler.handleMethodValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errores()).hasSize(1);
    }

    @Test
    void handleConstraintViolation_retorna400() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("skuAValidar");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("no puede estar vacío");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errores()).hasSize(1);
    }

    @Test
    void handleGeneric_retorna500() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("boom"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().mensaje()).isEqualTo("Error interno del servidor");
    }
}
