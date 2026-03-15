package com.cartisan.data.query;

import com.cartisan.data.query.config.JooqAutoConfiguration;
import com.cartisan.data.query.page.PageQuery;
import com.cartisan.data.query.support.JooqTenantSupport;
import com.cartisan.security.context.TenantTestSupport;
import com.cartisan.test.container.PostgresTestContainer;
import com.cartisan.test.base.IntegrationTestBase;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * jOOQ 集成测试。
 *
 * <p>验证 DSLContext 与数据库的集成，以及多租户查询功能。</p>
 *
 * @since 0.3.0
 */
@SpringBootTest(classes = JooqIntegrationTest.TestApp.class)
@ImportAutoConfiguration({
    PostgresTestContainer.class,
    DataSourceAutoConfiguration.class,
    JooqAutoConfiguration.class
})
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
class JooqIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DSLContext dslContext;

    @Nested
    @DisplayName("DSLContext 可用性")
    class DslContextAvailability {

        @Test
        @DisplayName("given_dslContextAutoConfigured_whenExecuteSimpleQuery_thenReturnsResult")
        void given_dslContextAutoConfigured_whenExecuteSimpleQuery_thenReturnsResult() {
            // Given: DSLContext 由自动配置创建

            // When: 执行简单查询
            Result<Record1<Integer>> result = dslContext
                .select(DSL.one())
                .fetch();

            // Then: 返回结果
            assertThat(result).hasSize(1);
            assertThat(result.get(0).value1()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("租户过滤")
    class TenantFiltering {

        @Test
        @DisplayName("given_tenantContextAndTableWithTenantId_whenQueryWithEqTenantId_thenFiltersByTenant")
        void given_tenantContextAndTableWithTenantId_whenQueryWithEqTenantId_thenFiltersByTenant() {
            // Given: 创建临时表并插入多租户数据
            createTestUserTable();
            insertTestUserData();

            var testUserTable = DSL.table(DSL.name("test_user"));
            // 直接创建字段，不依赖表的元数据
            var tenantIdField = DSL.field(
                DSL.name("test_user", "tenant_id"),
                org.jooq.impl.SQLDataType.BIGINT
            );

            // When: 在租户 100 上下文中执行带租户过滤的查询
            AtomicReference<List<Record>> results = new AtomicReference<>();
            TenantTestSupport.runWithTenant(100L, () -> {
                results.set(dslContext
                    .selectFrom(testUserTable)
                    .where(JooqTenantSupport.eqTenantId(tenantIdField))
                    .fetch());
            });

            // Then: 只返回租户 100 的数据
            assertThat(results.get()).hasSize(1);
            assertThat(results.get().get(0).get("name", String.class)).isEqualTo("Alice");
        }

        @AfterEach
        void cleanup() {
            dslContext.execute("DROP TABLE IF EXISTS test_user");
        }

        private void createTestUserTable() {
            dslContext.execute("""
                CREATE TEMP TABLE test_user (
                    id BIGINT,
                    tenant_id BIGINT,
                    name VARCHAR
                )
                """);
        }

        private void insertTestUserData() {
            dslContext.execute("INSERT INTO test_user VALUES (1, 100, 'Alice')");
            dslContext.execute("INSERT INTO test_user VALUES (2, 200, 'Bob')");
        }
    }

    @Nested
    @DisplayName("分页参数执行")
    class PaginationExecution {

        @Test
        @DisplayName("given_pageQuery_whenQueryWithLimitOffset_thenExecutesSuccessfully")
        void given_pageQuery_whenQueryWithLimitOffset_thenExecutesSuccessfully() {
            // Given: PageQuery
            PageQuery pageQuery = PageQuery.of(2, 10);  // offset = 10

            // When: 执行带 LIMIT/OFFSET 的查询
            Result<?> result = dslContext
                .resultQuery("SELECT 1 LIMIT ? OFFSET ?", pageQuery.size(), pageQuery.offset())
                .fetch();

            // Then: 查询成功执行（无断言，只验证不报错）
            assertThat(result).isNotNull();
        }
    }

    @org.springframework.boot.autoconfigure.SpringBootApplication(proxyBeanMethods = false)
    @Import(PostgresTestContainer.class)
    static class TestApp {
    }
}
