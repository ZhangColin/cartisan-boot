package com.cartisan.web.resubmit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ResubmitLock 单元测试。
 *
 * <p>测试 Redis 版本的防重复提交锁机制。</p>
 */
@ExtendWith(MockitoExtension.class)
class ResubmitLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ResubmitLock resubmitLock;

    @Test
    void shouldLockSuccessfully() {
        // Given: Redis 中不存在该 key
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // When: 尝试获取锁
        boolean result = resubmitLock.lock("resubmit:test:abc123", 10);

        // Then: 加锁成功
        assertThat(result).isTrue();

        // Verify: 调用了 setIfAbsent
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeoutCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(valueOperations).setIfAbsent(keyCaptor.capture(), valueCaptor.capture(),
                timeoutCaptor.capture(), unitCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("resubmit:test:abc123");
        assertThat(valueCaptor.getValue()).isEqualTo("1");
        assertThat(timeoutCaptor.getValue()).isEqualTo(10L);
        assertThat(unitCaptor.getValue()).isEqualTo(TimeUnit.SECONDS);
    }

    @Test
    void shouldReturnFalseWhenKeyExists() {
        // Given: Redis 中已存在该 key
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        // When: 尝试获取锁
        boolean result = resubmitLock.lock("resubmit:test:abc123", 10);

        // Then: 加锁失败
        assertThat(result).isFalse();
    }

    @Test
    void shouldGenerateKeyFromArgs() {
        // Given: 前缀和参数哈希
        String prefix = "createUser";
        String argsHash = "abc123";

        // When: 生成 key
        String key = resubmitLock.generateKey(prefix, argsHash);

        // Then: key 格式正确
        assertThat(key).isEqualTo("resubmit:createUser:abc123");
    }

    @Test
    void shouldGenerateKeyWithEmptyPrefix() {
        // Given: 空前缀和参数哈希
        String prefix = "";
        String argsHash = "abc123";

        // When: 生成 key
        String key = resubmitLock.generateKey(prefix, argsHash);

        // Then: key 格式正确（保留空前缀的分隔符，保持一致性）
        assertThat(key).isEqualTo("resubmit::abc123");
    }

    @Test
    void shouldHandleNullFromRedis() {
        // Given: Redis 返回 null（网络异常等情况）
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(null);

        // When: 尝试获取锁
        boolean result = resubmitLock.lock("resubmit:test:abc123", 10);

        // Then: 返回 false（安全策略，异常情况下拒绝请求）
        assertThat(result).isFalse();
    }
}
