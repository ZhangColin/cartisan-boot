package com.cartisan.data.query.support;

import com.cartisan.security.context.TenantContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JooqTenantSupport 集成测试。
 *
 * <p>测试有租户上下文时的条件生成逻辑。
 * 需要 cartisan-security 模块支持。
 */
@DisplayName("JooqTenantSupport 集成测试（有租户上下文）")
class JooqTenantSupportIntegrationTest {

    private static final Long TEST_TENANT_ID = 123L;

    @Test
    @DisplayName("给定有租户上下文 - 调用 eqTenantId - 返回租户等值条件")
    void shouldReturnTenantEqCondition_whenTenantContextExists() {
        // Given: 使用 runWithTenantId 设置租户上下文
        var resultHolder = new Object() { Condition condition; };

        TenantContext.runWithTenantId(TEST_TENANT_ID, () -> {
            var tenantIdField = DSL.field("tenant_id", Long.class);
            resultHolder.condition = JooqTenantSupport.eqTenantId(tenantIdField);
        });

        // Then: 应返回租户等值条件（不是 noCondition）
        assertThat(resultHolder.condition).isNotNull();
        assertThat(resultHolder.condition).isNotEqualTo(DSL.noCondition());
    }

    @Test
    @DisplayName("给定无租户上下文 - 调用 eqTenantId - 返回 noCondition")
    void shouldReturnNoCondition_whenNoTenantContext() {
        // Given: 使用 null 租户 ID（无租户上下文）
        var resultHolder = new Object() { Condition condition; };

        TenantContext.runWithTenantId(null, () -> {
            var tenantIdField = DSL.field("tenant_id", Long.class);
            resultHolder.condition = JooqTenantSupport.eqTenantId(tenantIdField);
        });

        // Then
        assertThat(resultHolder.condition).isEqualTo(DSL.noCondition());
    }
}
