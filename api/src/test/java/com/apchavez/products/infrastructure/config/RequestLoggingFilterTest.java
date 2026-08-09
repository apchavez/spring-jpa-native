package com.apchavez.products.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void solicitudExitosa_agregaHeaderYLoguea() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/products/active");
        when(response.getStatus()).thenReturn(200);

        filter.doFilterInternal(request, response, chain);

        assertThat(org.slf4j.MDC.get(RequestLoggingFilter.REQUEST_ID_CONTEXT_KEY)).isNull();
    }

    @Test
    void solicitudFallida_relanzaExcepcionYLoguea() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        doThrow(new IOException("stream cerrado")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(IOException.class)
                .hasMessage("stream cerrado");
        assertThat(org.slf4j.MDC.get(RequestLoggingFilter.REQUEST_ID_CONTEXT_KEY)).isNull();
    }
}
