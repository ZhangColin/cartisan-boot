package com.cartisan.data.jpa.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Auditable 单元测试。
 *
 * <p>测试命名遵循 TEST-002 规则：given_{条件}_when_{操作}_then_{预期结果}</p>
 */
class AuditableTest {

    // ==================== AC1: 审计字段存在 ====================
    @Test
    void given_auditableSubclass_when_instantiated_then_hasAuditFields() {
        // Given: 创建一个 Auditable 的匿名子类实例
        TestAuditableEntity entity = new TestAuditableEntity();

        // When & Then: 验证审计字段存在且初始值为 null
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getLastModifiedDate()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getLastModifiedBy()).isNull();
    }

    @Test
    void given_auditableSubclass_when_getterCalled_then_doesNotThrowException() {
        // Given: 创建一个 Auditable 的匿名子类实例
        TestAuditableEntity entity = new TestAuditableEntity();

        // When & Then: 验证 getter 存在且不抛异常
        assertThat(entity).isNotNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    /**
     * 测试用 Auditable 子类。
     */
    static class TestAuditableEntity extends Auditable {
        // 空实现，仅用于测试
    }
}
