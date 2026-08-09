package com.apchavez.products.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Wires Spring Data JPA's {@code @CreatedDate}/{@code @LastModifiedDate} auditing (see
 * {@link com.apchavez.products.infrastructure.persistence.ProductEntity}) — without
 * {@code @EnableJpaAuditing}, those fields would simply stay {@code null}, since
 * {@code AuditingEntityListener} only activates once this is present.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
