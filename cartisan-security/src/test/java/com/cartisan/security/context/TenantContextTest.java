package com.cartisan.security.context;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantContext 单元测试。
 * <p>
 * 测试类放在与生产类相同的包下，以访问 package-private 方法。
 * </p>
 */
class TenantContextTest {

    @Test
    void givenNoTenant_whenGetCurrentTenantId_thenReturnsNull() {
        // Given - 无租户上下文

        // When
        Long tenantId = TenantContext.getCurrentTenantId();

        // Then
        assertThat(tenantId).isNull();
    }

    @Test
    void givenTenant_whenGetCurrentTenantId_thenReturnsTenantId() {
        // Given
        Long expectedTenantId = 123L;
        AtomicReference<Long> actualTenantId = new AtomicReference<>();

        TenantContext.runWithTenant(expectedTenantId, () -> {
            // When
            actualTenantId.set(TenantContext.getCurrentTenantId());
        });

        // Then
        assertThat(actualTenantId.get()).isEqualTo(expectedTenantId);
    }

    @Test
    void givenNullTenant_whenGetCurrentTenantId_thenReturnsNull() {
        // Given
        AtomicReference<Long> actualTenantId = new AtomicReference<>();

        TenantContext.runWithTenant(null, () -> {
            // When
            actualTenantId.set(TenantContext.getCurrentTenantId());
        });

        // Then
        assertThat(actualTenantId.get()).isNull();
    }

    @Test
    void givenTenant_whenScopeEnds_thenTenantIsCleared() {
        // Given & When - 在作用域内设置租户
        AtomicReference<Long> tenantIdInScope = new AtomicReference<>();
        TenantContext.runWithTenant(111L, () -> {
            tenantIdInScope.set(TenantContext.getCurrentTenantId());
        });

        // Then - 作用域结束后租户被清除
        assertThat(tenantIdInScope.get()).isEqualTo(111L);
        assertThat(TenantContext.getCurrentTenantId()).isNull();
    }
}
