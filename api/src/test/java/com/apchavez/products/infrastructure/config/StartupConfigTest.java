package com.apchavez.products.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class StartupConfigTest {

    private final StartupConfig config = new StartupConfig();

    @Test
    void validateEnvVars_sinVariablesDeEntornoRequeridas_lanzaIllegalStateException() {
        assertThatThrownBy(config::validateEnvVars)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_HOST");
    }

    @Test
    void logStartupInfo_noLanzaExcepcion() {
        assertThatCode(config::logStartupInfo).doesNotThrowAnyException();
    }
}
