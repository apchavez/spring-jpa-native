package com.apchavez.products.infrastructure.messaging;

import com.apchavez.products.domain.event.ProductEvent;
import com.apchavez.products.domain.event.ProductEventType;
import com.apchavez.products.domain.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaProductEventPublisherTest {

    private final Product product = new Product(
            1, "SKU-1", "Producto Demo", null, 10, "Categoria", 9.99, 5, true);
    private final ProductEvent event = ProductEvent.of(ProductEventType.PRODUCT_CREATED, product);

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> mockTemplate() {
        return mock(KafkaTemplate.class);
    }

    @Test
    void publish_conEnvioExitoso_noLanzaExcepcion() {
        KafkaTemplate<String, String> kafkaTemplate = mockTemplate();
        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        KafkaProductEventPublisher publisher = new KafkaProductEventPublisher(kafkaTemplate);

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }

    @Test
    void publish_conInterruptedException_marcaElHiloComoInterrumpidoYNoPropaga() {
        KafkaTemplate<String, String> kafkaTemplate = mockTemplate();
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>() {
            @Override
            public SendResult<String, String> get() throws InterruptedException {
                throw new InterruptedException("interrupted");
            }
        };
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        KafkaProductEventPublisher publisher = new KafkaProductEventPublisher(kafkaTemplate);

        try {
            assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
            org.assertj.core.api.Assertions.assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void publish_conExcepcionGenerica_noLanzaYSoloLoguea() {
        KafkaTemplate<String, String> kafkaTemplate = mockTemplate();
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>() {
            @Override
            public SendResult<String, String> get() throws ExecutionException {
                throw new ExecutionException("kafka down", new RuntimeException("kafka down"));
            }
        };
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        KafkaProductEventPublisher publisher = new KafkaProductEventPublisher(kafkaTemplate);

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }
}
