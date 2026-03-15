package com.cartisan.data.query.support;

import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.TableField;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JooqTenantSupport 单元测试。
 * <p>
 * 测试多租户条件生成逻辑：
 * - 无租户上下文时返回 noCondition
 * - null 参数抛出 NullPointerException
 * </p>
 * <p>
 * 注意：有租户上下文的场景需要 ScopedValue 支持，
 * 留给 F04-05 集成测试验证。
 * </p>
 */
class JooqTenantSupportTest {

    @Test
    void givenNoTenantContext_whenEqTenantId_thenReturnsNoCondition() {
        // Given - 无租户上下文（默认状态）+ mock 字段
        TableField<?, Long> mockField = createMockTenantIdField();

        // When - 调用 eqTenantId
        Condition result = JooqTenantSupport.eqTenantId(mockField);

        // Then - 应返回 noCondition
        assertThat(result).isEqualTo(DSL.noCondition());
    }

    @Test
    void givenNullField_whenEqTenantId_thenThrowsNullPointerException() {
        // Given - null 参数
        TableField<?, Long> nullField = null;

        // When & Then - 应抛出 NullPointerException
        assertThatThrownBy(() -> JooqTenantSupport.eqTenantId(nullField))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tenantIdField");
    }

    /**
     * 创建模拟的租户 ID 字段。
     * 使用 Mockito 创建 TableField mock 对象。
     */
    @SuppressWarnings("unchecked")
    private TableField<?, Long> createMockTenantIdField() {
        return Mockito.mock(TableField.class);
    }
}
