package com.cartisan.test.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;

/**
 * 集成测试基类。
 *
 * <p>自动启动 PostgreSQL + Redis 容器，每个测试方法前清理全部数据，
 * 保证测试间完全隔离。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * class OrderRepositoryTest extends IntegrationTestBase {
 *
 *     @Autowired
 *     private OrderRepository orderRepository;
 *
 *     @Test
 *     void shouldSaveOrder() {
 *         // 数据库已清理，可直接测试
 *         Order order = new Order("O001");
 *         orderRepository.save(order);
 *         assertThat(orderRepository.findById("O001")).isPresent();
 *     }
 * }
 * }</pre>
 *
 * <h3>数据清理策略</h3>
 * <ul>
 *   <li>PostgreSQL：{@code TRUNCATE ... CASCADE} 自动处理外键</li>
 *   <li>Redis：{@code FLUSHDB} 清空当前数据库</li>
 *   <li>Flyway 迁移表 {@code flyway_schema_history} 被排除</li>
 * </ul>
 *
 * @since 0.1.0
 */
public abstract class IntegrationTestBase {

    /**
     * PostgreSQL 清理 SQL。
     *
     * <p>使用 {@code TRUNCATE ... CASCADE} 一次性清空所有表，
     * PostgreSQL 自动处理外键约束，无需排序。</p>
     */
    private static final String TRUNCATE_ALL_SQL = """
            DO $$
            DECLARE
                tables TEXT;
            BEGIN
                SELECT string_agg(tablename, ', ') INTO tables
                FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename != 'flyway_schema_history';
                IF tables IS NOT NULL THEN
                    EXECUTE 'TRUNCATE TABLE ' || tables || ' CASCADE';
                END IF;
            END $$
            """;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 每个测试方法前清理数据。
     *
     * <p>{@code required = false} 允许业务项目只使用部分容器。</p>
     */
    @BeforeEach
    void cleanData() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute(TRUNCATE_ALL_SQL);
        }
        if (redisTemplate != null) {
            redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
        }
    }
}
