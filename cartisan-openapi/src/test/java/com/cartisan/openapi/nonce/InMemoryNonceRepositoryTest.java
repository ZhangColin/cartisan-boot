package com.cartisan.openapi.nonce;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryNonceRepositoryTest {

    private final InMemoryNonceRepository repository = new InMemoryNonceRepository();

    @Test
    void shouldReturnTrue_whenFirstAcquire() {
        boolean result = repository.tryAcquire("nonce-1", Duration.ofSeconds(60));

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenDuplicateAcquire() {
        repository.tryAcquire("nonce-1", Duration.ofSeconds(60));

        boolean result = repository.tryAcquire("nonce-1", Duration.ofSeconds(60));

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrue_whenExpiredNonceReacquired() throws InterruptedException {
        repository.tryAcquire("nonce-1", Duration.ofMillis(10));
        Thread.sleep(50);

        boolean result = repository.tryAcquire("nonce-1", Duration.ofSeconds(60));

        assertThat(result).isTrue();
    }

    @Test
    void shouldHandleDifferentNoncesIndependently() {
        boolean result1 = repository.tryAcquire("nonce-1", Duration.ofSeconds(60));
        boolean result2 = repository.tryAcquire("nonce-2", Duration.ofSeconds(60));

        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
    }
}
