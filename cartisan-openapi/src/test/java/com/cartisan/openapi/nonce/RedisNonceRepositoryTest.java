package com.cartisan.openapi.nonce;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisNonceRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldReturnTrue_whenKeyNotExists() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(true);

        RedisNonceRepository repository = new RedisNonceRepository(redisTemplate);
        boolean result = repository.tryAcquire("nonce-1", Duration.ofSeconds(60));

        assertThat(result).isTrue();
        verify(valueOps).setIfAbsent(eq("openapi:nonce:nonce-1"), eq("1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void shouldReturnFalse_whenKeyAlreadyExists() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(false);

        RedisNonceRepository repository = new RedisNonceRepository(redisTemplate);
        boolean result = repository.tryAcquire("nonce-1", Duration.ofSeconds(60));

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalse_whenSetIfAbsentReturnsNull() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(any(), eq("1"), any(Duration.class))).thenReturn(null);

        RedisNonceRepository repository = new RedisNonceRepository(redisTemplate);
        boolean result = repository.tryAcquire("nonce-1", Duration.ofSeconds(60));

        assertThat(result).isFalse();
    }
}
