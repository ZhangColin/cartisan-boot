package com.cartisan.test.base;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("IntegrationTestBase 集成测试")
@SpringBootTest(classes = TestConfiguration.class)
class IntegrationTestBaseTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("应能注入 JdbcTemplate 和 RedisTemplate")
    void given_whenStartTest_thenTemplatesInjected() {
        // Then
        assertThat(jdbcTemplate).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    @DisplayName("应能查询数据库元数据")
    void given_whenQueryDatabase_thenReturnsMetadata() {
        // When
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public'",
            Integer.class
        );

        // Then
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("应能向 Redis 写入数据")
    void given_whenSetRedis_thenDataPersisted() {
        // When
        redisTemplate.opsForValue().set("test:key", "test:value");

        // Then
        assertThat(redisTemplate.opsForValue().get("test:key")).isEqualTo("test:value");
    }
}
