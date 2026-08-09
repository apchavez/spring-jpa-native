package com.apchavez.products.domain.exception;

/**
 * Domain-level wrapper for a JPA optimistic-locking conflict (concurrent modification of the
 * same {@code @Version}-ed row) — keeps Spring's {@code ObjectOptimisticLockingFailureException}
 * out of the domain/application layers.
 */
public class StaleProductException extends ProductDomainException {
    public StaleProductException(Integer id) {
        super("El producto " + id + " fue modificado por otra petición mientras tanto; vuelve a intentarlo");
    }
}
