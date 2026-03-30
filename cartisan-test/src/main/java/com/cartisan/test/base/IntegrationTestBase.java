package com.cartisan.test.base;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 集成测试基类。
 *
 * <p>提供基础的集成测试能力，不依赖 Testcontainers。
 * 测试环境需要开发者手动启动 PostgreSQL 和 Redis：</p>
 * <pre>{@code
 * # 启动 PostgreSQL
 * docker run -d -p 5432:5432 \
 *   -e POSTGRES_DB=testdb \
 *   -e POSTGRES_USER=test \
 *   -e POSTGRES_PASSWORD=test \
 *   postgres:16-alpine
 *
 * # 启动 Redis
 * docker run -d -p 6379:6379 redis:7-alpine
 * }</pre>
 *
 * <h3>特性</h3>
 * <ul>
 *   <li>每次测试后自动回滚事务，保持数据隔离</li>
 *   <li>支持通过环境变量配置数据库连接</li>
 *   <li>@BeforeEach 提供 JdbcTemplate 用于数据准备</li>
 * </ul>
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
 *         // 数据库已清理，可以放心插入数据
 *         Order order = new Order("O001");
 *         orderRepository.save(order);
 *
 *         // 测试结束后自动回滚，不影响其他测试
 *     }
 * }
 * }</pre>
 *
 * <h3>环境变量配置</h3>
 * <table>
 *   <tr><th>变量名</th><th>默认值</th><th>说明</th></tr>
 *   <tr><td>TEST_DB_URL</td><td>jdbc:postgresql://localhost:5432/testdb</td><td>数据库 URL</td></tr>
 *   <tr><td>TEST_DB_USER</td><td>test</td><td>数据库用户</td></tr>
 *   <tr><td>TEST_DB_PASSWORD</td><td>test</td><td>数据库密码</td></tr>
 *   <tr><td>TEST_REDIS_HOST</td><td>localhost</td><td>Redis 主机</td></tr>
 *   <tr><td>TEST_REDIS_PORT</td><td>6379</td><td>Redis 端口</td></tr>
 * </table>
 *
 * @since 0.2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * 每次测试前的准备。
     *
     * <p>默认实现为空，子类可以重写此方法准备测试数据。</p>
     */
    @BeforeEach
    void setUp() {
        // 默认不做任何清理
        // 子类可以重写此方法准备测试数据
    }
}
