package com.apchavez.products.infrastructure.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaConfigTest {

    private final KafkaConfig config = new KafkaConfig();

    @Test
    void productProducerFactory_conSoloBootstrapServers_omiteSasl() {
        Environment environment = mock(Environment.class);
        when(environment.getRequiredProperty("spring.kafka.producer.bootstrap-servers"))
                .thenReturn("localhost:9092");
        when(environment.getProperty("spring.kafka.properties.security.protocol")).thenReturn(null);
        when(environment.getProperty("spring.kafka.properties.sasl.mechanism")).thenReturn(null);
        when(environment.getProperty("spring.kafka.properties.sasl.jaas.config")).thenReturn(null);

        ProducerFactory<String, String> factory = config.productProducerFactory(environment);

        assertThat(factory).isInstanceOf(DefaultKafkaProducerFactory.class);
        var props = ((DefaultKafkaProducerFactory<String, String>) factory).getConfigurationProperties();
        assertThat(props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9092");
        assertThat(props).doesNotContainKey("security.protocol");
        assertThat(props).doesNotContainKey("sasl.mechanism");
        assertThat(props).doesNotContainKey("sasl.jaas.config");
    }

    @Test
    void productProducerFactory_conSaslCompleto_incluyeTodasLasPropiedades() {
        Environment environment = mock(Environment.class);
        when(environment.getRequiredProperty("spring.kafka.producer.bootstrap-servers"))
                .thenReturn("broker:9093");
        when(environment.getProperty("spring.kafka.properties.security.protocol"))
                .thenReturn("SASL_SSL");
        when(environment.getProperty("spring.kafka.properties.sasl.mechanism"))
                .thenReturn("PLAIN");
        when(environment.getProperty("spring.kafka.properties.sasl.jaas.config"))
                .thenReturn("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"u\" password=\"p\";");

        ProducerFactory<String, String> factory = config.productProducerFactory(environment);

        var props = ((DefaultKafkaProducerFactory<String, String>) factory).getConfigurationProperties();
        assertThat(props.get("security.protocol")).isEqualTo("SASL_SSL");
        assertThat(props.get("sasl.mechanism")).isEqualTo("PLAIN");
        assertThat(props.get("sasl.jaas.config")).asString().contains("PlainLoginModule");
    }

    @Test
    void productProducerFactory_conValorEnBlanco_seOmite() {
        Environment environment = mock(Environment.class);
        when(environment.getRequiredProperty("spring.kafka.producer.bootstrap-servers"))
                .thenReturn("localhost:9092");
        when(environment.getProperty("spring.kafka.properties.security.protocol")).thenReturn("   ");
        when(environment.getProperty("spring.kafka.properties.sasl.mechanism")).thenReturn(null);
        when(environment.getProperty("spring.kafka.properties.sasl.jaas.config")).thenReturn(null);

        ProducerFactory<String, String> factory = config.productProducerFactory(environment);

        var props = ((DefaultKafkaProducerFactory<String, String>) factory).getConfigurationProperties();
        assertThat(props).doesNotContainKey("security.protocol");
    }

    @Test
    void productKafkaTemplate_envuelveLaProducerFactory() {
        Environment environment = mock(Environment.class);
        when(environment.getRequiredProperty("spring.kafka.producer.bootstrap-servers"))
                .thenReturn("localhost:9092");
        ProducerFactory<String, String> factory = config.productProducerFactory(environment);

        KafkaTemplate<String, String> template = config.productKafkaTemplate(factory);

        assertThat(template.getProducerFactory()).isSameAs(factory);
    }
}
