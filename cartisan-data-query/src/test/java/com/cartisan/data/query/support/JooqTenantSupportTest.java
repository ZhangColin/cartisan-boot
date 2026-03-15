package com.cartisan.data.query.support;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JooqTenantSupport 单元测试。
 *
 * <p>测试多租户条件生成逻辑：
 * <ul>
 *   <li>无租户上下文时返回 noCondition</li>
 *   <li>null 参数抛出 NullPointerException</li>
 * </ul>
 *
 * <p>注意：有租户上下文的场景需要 ScopedValue 支持，
 * 留给 F04-05 集成测试验证。</p>
 */
class JooqTenantSupportTest {

    @Test
    void givenNoTenantContext_whenEqTenantId_thenReturnsNoCondition() {
        // Given - 无租户上下文（默认状态）+ mock 字段
        Field<Long> mockField = createMockTenantIdField();

        // When - 调用 eqTenantId
        Condition result = JooqTenantSupport.eqTenantId(mockField);

        // Then - 应返回 noCondition
        assertThat(result).isEqualTo(DSL.noCondition());
    }

    @Test
    void givenNullField_whenEqTenantId_thenThrowsNullPointerException() {
        // Given - null 参数
        Field<Long> nullField = null;

        // When & Then - 应抛出 NullPointerException
        assertThatThrownBy(() -> JooqTenantSupport.eqTenantId(nullField))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tenantIdField");
    }

    /**
     * 创建模拟的租户 ID 字段。
     * 使用 Mockito 创建 Field mock 对象。
     */
    @SuppressWarnings("unchecked")
    private Field<Long> createMockTenantIdField() {
        return Mockito.mock(Field.class);
    }
}
