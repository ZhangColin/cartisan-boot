package com.cartisan.data.jpa.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SoftDeletable 单元测试。
 *
 * <p>测试命名遵循 TEST-002 规则：given_{条件}_when_{操作}_then_{预期结果}</p>
 */
class SoftDeletableTest {

    // ==================== AC3: 软删除字段默认值 ====================
    @Test
    void given_softDeletableSubclass_when_instantiated_then_deletedIsFalse() {
        // Given: 创建一个 SoftDeletable 的匿名子类实例
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();

        // When & Then: 验证 deleted 字段默认为 false
        assertThat(entity.getDeleted()).isFalse();
        assertThat(entity.isDeleted()).isFalse();
    }

    // ==================== AC6-2: 多次删除幂等 ====================
    @Test
    void given_softDeletableEntity_when_setDeletedMultipleTimes_then_remainsTrue() {
        // Given: 创建一个 SoftDeletable 实例
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();

        // When: 多次设置 deleted = true
        entity.setDeleted(true);
        entity.setDeleted(true);
        entity.setDeleted(true);

        // Then: deleted 保持 true（幂等）
        assertThat(entity.getDeleted()).isTrue();
    }

    // ==================== 继承 Auditable ====================
    @Test
    void given_softDeletableSubclass_when_instantiated_then_hasAuditFields() {
        // Given: 创建一个 SoftDeletable 的匿名子类实例
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();

        // When & Then: 验证继承了 Auditable 的审计字段
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
    }

    /**
     * 测试用 AuditableSoftDeletable 子类。
     */
    static class TestSoftDeletableEntity extends AuditableSoftDeletable {
        // 空实现，仅用于测试
    }
}
