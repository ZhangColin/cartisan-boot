package com.cartisan.data.jpa.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

/**
 * 审计功能集成测试。
 *
 * <p>测试命名遵循 TEST-002 规则：given_{条件}_when_{操作}_then_{预期结果}</p>
 *
 * <p>验证 AC1-AC2、AC5、AC6：</p>
 * <ul>
 *   <li>AC1: 首次保存时审计字段自动填充</li>
 *   <li>AC2: 更新时时间/人字段自动更新</li>
 *   <li>AC5: 条件装配（有 AuditorAware 时启用）</li>
 *   <li>AC6-1: 新建实体审计字段初始为 null</li>
 *   <li>AC6-4: AuditorAware 返回 null 时 by 字段保持 null</li>
 * </ul>
 */
@DataJpaTest
@EntityScan(basePackageClasses = TestAuditableEntity.class)
class AuditingIntegrationTest {

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager testEntityManager;

    @Autowired
    private TestAuditableEntityRepository repository;

    @Autowired
    private JpaTestApplication.TestAuditorAware testAuditorAware;

    @BeforeEach
    void setUp() {
        testAuditorAware.setCurrentAuditor(1L);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
        testAuditorAware.clearCurrentAuditor();
    }

    // ==================== AC1: 首次保存时审计字段自动填充 ====================
    @Test
    void given_newEntity_when_save_then_auditFieldsAutoPopulated() {
        // Given: 创建一个新实体
        TestAuditableEntity entity = new TestAuditableEntity();
        entity.setName("Test Entity");

        // When: 保存实体
        TestAuditableEntity saved = repository.save(entity);

        // Then: 审计字段被自动填充
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo(1L);
        assertThat(saved.getUpdatedBy()).isEqualTo(1L);
    }

    // ==================== AC2: 更新时时间/人字段自动更新 ====================
    @Test
    @Transactional
    void given_existingEntity_when_update_then_updatedAtFieldsUpdated() {
        // Given: 创建并保存一个实体
        TestAuditableEntity entity = new TestAuditableEntity();
        entity.setName("Original Name");
        TestAuditableEntity saved = repository.saveAndFlush(entity);
        LocalDateTime originalLastModifiedDate = saved.getUpdatedAt();

        // 切换审计人
        testAuditorAware.setCurrentAuditor(2L);

        // When: 更新实体并刷新
        saved.setName("Updated Name");
        testEntityManager.flush();
        testEntityManager.clear();

        TestAuditableEntity updated = repository.findById(saved.getId()).orElseThrow();

        // Then: createdAt/createdBy 保持不变，updatedAt/updatedBy 被更新
        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(updated.getCreatedBy()).isEqualTo(1L);
        assertThat(updated.getUpdatedBy()).isEqualTo(2L);
        assertThat(updated.getUpdatedAt()).isAfter(originalLastModifiedDate);
    }

    // ==================== AC6-1: 新建实体审计字段初始为 null ====================
    @Test
    void given_newEntityInstance_when_notPersisted_then_auditFieldsAreNull() {
        // Given: 创建一个新实例（未持久化）
        TestAuditableEntity entity = new TestAuditableEntity();

        // When & Then: 审计字段为 null
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
    }

    // ==================== AC6-4: AuditorAware 返回 null ====================
    @Test
    void given_auditorAwareReturnsNull_when_save_then_byFieldsRemainNull() {
        // Given: AuditorAware 返回 null
        testAuditorAware.setCurrentAuditor(null);

        TestAuditableEntity entity = new TestAuditableEntity();
        entity.setName("Test Entity");

        // When: 保存实体
        TestAuditableEntity saved = repository.save(entity);

        // Then: 时间字段被填充，by 字段保持 null
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isNull();
        assertThat(saved.getUpdatedBy()).isNull();
    }

}
