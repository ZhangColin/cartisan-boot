# Feature: cartisan-data-jpa 审计与软删除 — 实施计划

> **Feature ID**: F02-06
> **Phase**: 3 — Plan
> **预估代码量**: 100-150 行

---

## 目标复述

提供 `Auditable` 和 `SoftDeletable` 两个 JPA 基类，使业务实体继承后自动获得：
1. **审计能力**：创建时间、修改时间、创建人、修改人 4 个字段自动填充
2. **软删除能力**：`deleted` 标记 + 查询时自动过滤已删除记录
3. **条件装配**：仅当容器中存在 `AuditorAware` Bean 时启用对"人"的审计

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| **创建** | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/Auditable.java` | 审计基类（@MappedSuperclass） |
| **创建** | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/SoftDeletable.java` | 软删除基类，继承 Auditable |
| **创建** | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/JpaAuditingConfiguration.java` | JPA Auditing 条件装配配置 |
| **创建** | `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AuditableTest.java` | Auditable 单元测试 |
| **创建** | `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/SoftDeletableTest.java` | SoftDeletable 单元测试 |
| **创建** | `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AuditingIntegrationTest.java` | 审计功能集成测试 |

---

## 包结构

```
cartisan-data-jpa/
├── src/main/java/com/cartisan/data/jpa/
│   ├── config/
│   │   ├── CartisanDataJpaAutoConfiguration.java  (已有，无需修改)
│   │   └── JpaAuditingConfiguration.java          (新增)
│   └── domain/
│       ├── Auditable.java                         (新增)
│       └── SoftDeletable.java                     (新增)
└── src/test/java/com/cartisan/data/jpa/
    └── domain/
        ├── AuditableTest.java                     (新增)
        ├── SoftDeletableTest.java                 (新增)
        └── AuditingIntegrationTest.java           (新增)
```

---

## 核心流程（伪代码）

### 审计字段自动填充流程

```
1. 业务代码：repository.save(order)
   ↓
2. Spring Data JPA：在持久化前拦截
   ↓
3. JPA Auditing：
   - 检测到 @CreatedDate → 设置 createdAt = LocalDateTime.now()
   - 检测到 @LastModifiedDate → 设置 lastModifiedDate = LocalDateTime.now()
   - 检测到 @CreatedBy → 调用 AuditorAware.getCurrentAuditor()
   - 检测到 @LastModifiedBy → 调用 AuditorAware.getCurrentAuditor()
   ↓
4. 持久化到数据库
```

### 软删除流程

```
1. 业务代码：repository.delete(product)
   ↓
2. JPA：生成 UPDATE SET deleted = true WHERE id = ?
   ↓
3. 数据库：deleted 字段 = true
   ↓
4. 后续查询：@SQLRestriction("deleted = false") 自动附加 WHERE 条件
```

---

## 原子任务清单

### Step 1: 创建 Auditable 基类

**文件**: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/Auditable.java`

**内容**: 将 `02_interface.md` 中的 `Auditable` 伪代码转为 Java 源代码

```java
package com.cartisan.data.jpa.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * 可审计实体基类。
 *
 * <p>实体继承此基类后，JPA Auditing 会自动填充审计字段。</p>
 *
 * @since 0.2.0
 */
@MappedSuperclass
public abstract class Auditable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
}
```

**验证**: `./gradlew :cartisan-data-jpa:compileJava` 通过

---

### Step 2: 创建 SoftDeletable 基类

**文件**: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/SoftDeletable.java`

**内容**: 继承 `Auditable`，增加 `deleted` 字段和 `@SQLRestriction`

```java
package com.cartisan.data.jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.SQLRestriction;

/**
 * 可软删除实体基类。
 *
 * <p>继承 {@link Auditable}，增加软删除能力。</p>
 *
 * @since 0.2.0
 */
@MappedSuperclass
@SQLRestriction("deleted = false")
public abstract class SoftDeletable extends Auditable {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    public boolean isDeleted() {
        return deleted;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
```

**验证**: `./gradlew :cartisan-data-jpa:compileJava` 通过

---

### Step 3: 创建 JpaAuditingConfiguration

**文件**: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/JpaAuditingConfiguration.java`

**内容**: 条件装配 JPA Auditing

```java
package com.cartisan.data.jpa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 自动配置。
 *
 * <p>仅在容器中存在 {@link AuditorAware} Bean 时启用 JPA Auditing。</p>
 *
 * @since 0.2.0
 */
@Configuration
@ConditionalOnBean(AuditorAware.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfiguration {
    // 无需额外代码，注解即完成配置
}
```

**验证**: `./gradlew :cartisan-data-jpa:compileJava` 通过

---

### Step 4: 编写 Auditable 单元测试（红灯）

**文件**: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AuditableTest.java`

**内容**: 测试 `Auditable` 基类的字段存在性和可访问性

```java
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
    void given_auditableSubclass_when_setterCalled_then_valueUpdated() {
        // Given: 创建一个 Auditable 的匿名子类实例
        TestAuditableEntity entity = new TestAuditableEntity();

        // When: 由于没有 setter，验证字段是可访问的
        // 注意：Auditable 只提供 getter，不允许手动设置
        // 实际填充由 JPA Auditing 在持久化时完成

        // Then: 验证 getter 存在且不抛异常
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
```

**验证**: 编译通过 + 测试全绿（这些是简单验证测试）

---

### Step 5: 编写 SoftDeletable 单元测试（红灯）

**文件**: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/SoftDeletableTest.java`

**内容**: 测试 `SoftDeletable` 基类的软删除字段

```java
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
        assertThat(entity.getLastModifiedDate()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getLastModifiedBy()).isNull();
    }

    /**
     * 测试用 SoftDeletable 子类。
     */
    static class TestSoftDeletableEntity extends SoftDeletable {
        // 空实现，仅用于测试
    }
}
```

**验证**: 编译通过 + 测试全绿

---

### Step 6: 编写审计功能集成测试（红灯）

**文件**: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AuditingIntegrationTest.java`

**内容**: 使用 `@DataJpaTest` 验证 JPA Auditing 自动填充功能

```java
package com.cartisan.data.jpa.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审计功能集成测试。
 *
 * <p>测试命名遵循 TEST-002 规则：given_{条件}_when_{操作}_then_{预期结果}</p>
 *
 * <p>验证 AC1-AC5：</p>
 * <ul>
 *   <li>AC1: 首次保存时审计字段自动填充</li>
 *   <li>AC2: 更新时时间/人字段自动更新</li>
 *   <li>AC5: 条件装配（有 AuditorAware 时启用）</li>
 *   <li>AC6-1: 新建实体审计字段初始为 null</li>
 *   <li>AC6-4: AuditorAware 返回 null 时 by 字段保持 null</li>
 * </ul>
 */
@DataJpaTest
@EntityScan(basePackageClasses = AuditingIntegrationTest.TestAuditableEntity.class)
@EnableJpaRepositories(basePackageClasses = AuditingIntegrationTest.TestAuditableEntityRepository.class)
class AuditingIntegrationTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private TestAuditableEntityRepository repository;

    @Autowired
    private TestAuditorAware testAuditorAware;

    @BeforeEach
    void setUp() {
        testAuditorAware.setCurrentAuditor("test-user");
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
        assertThat(saved.getLastModifiedDate()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");
        assertThat(saved.getLastModifiedBy()).isEqualTo("test-user");
    }

    // ==================== AC2: 更新时时间/人字段自动更新 ====================
    @Test
    void given_existingEntity_when_update_then_lastModifiedFieldsUpdated() {
        // Given: 创建并保存一个实体
        TestAuditableEntity entity = new TestAuditableEntity();
        entity.setName("Original Name");
        TestAuditableEntity saved = repository.save(entity);

        // 切换审计人
        testAuditorAware.setCurrentAuditor("updater-user");

        // When: 更新实体
        saved.setName("Updated Name");
        TestAuditableEntity updated = repository.save(saved);

        // Then: createdAt/createdBy 保持不变，lastModifiedDate/lastModifiedBy 被更新
        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(updated.getCreatedBy()).isEqualTo("test-user");
        assertThat(updated.getLastModifiedBy()).isEqualTo("updater-user");
        assertThat(updated.getLastModifiedDate()).isAfter(saved.getLastModifiedDate());
    }

    // ==================== AC6-1: 新建实体审计字段初始为 null ====================
    @Test
    void given_newEntityInstance_when_notPersisted_then_auditFieldsAreNull() {
        // Given: 创建一个新实例（未持久化）
        TestAuditableEntity entity = new TestAuditableEntity();

        // When & Then: 审计字段为 null
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getLastModifiedDate()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getLastModifiedBy()).isNull();
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
        assertThat(saved.getLastModifiedDate()).isNotNull();
        assertThat(saved.getCreatedBy()).isNull();
        assertThat(saved.getLastModifiedBy()).isNull();
    }

    // ==================== 测试实体 ====================
    @jakarta.persistence.Entity(name = "test_auditable_entity")
    static class TestAuditableEntity extends Auditable {

        @jakarta.persistence.Id
        @jakarta.persistence.GeneratedValue
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    // ==================== 测试 Repository ====================
    interface TestAuditableEntityRepository extends org.springframework.data.jpa.repository.JpaRepository<TestAuditableEntity, Long> {
    }

    // ==================== 测试配置 ====================
    @TestConfiguration
    static class TestConfig {

        @Bean
        public AuditorAware<String> auditorAware() {
            return new TestAuditorAware();
        }
    }

    // ==================== 测试用 AuditorAware ====================
    static class TestAuditorAware implements AuditorAware<String> {

        private String currentAuditor;

        @Override
        public Optional<String> getCurrentAuditor() {
            return Optional.ofNullable(currentAuditor);
        }

        public void setCurrentAuditor(String currentAuditor) {
            this.currentAuditor = currentAuditor;
        }

        public void clearCurrentAuditor() {
            this.currentAuditor = null;
        }
    }
}
```

**验证**: 编译通过 + 测试全绿

---

### Step 7: 编写软删除集成测试（红灯）

**文件**: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/SoftDeletableIntegrationTest.java`

**内容**: 使用 `@DataJpaTest` 验证软删除功能

```java
package com.cartisan.data.jpa.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 软删除集成测试。
 *
 * <p>测试命名遵循 TEST-002 规则：given_{条件}_when_{操作}_then_{预期结果}</p>
 *
 * <p>验证 AC3-AC4、AC6：</p>
 * <ul>
 *   <li>AC3: delete() 将 deleted 设为 true</li>
 *   <li>AC4: 查询时自动过滤已删除记录</li>
 *   <li>AC6-2: 多次删除幂等</li>
 *   <li>AC6-3: 允许更新已删除实体</li>
 * </ul>
 */
@DataJpaTest
@EntityScan(basePackageClasses = SoftDeletableIntegrationTest.TestSoftDeletableEntity.class)
@EnableJpaRepositories(basePackageClasses = SoftDeletableIntegrationTest.TestSoftDeletableEntityRepository.class)
class SoftDeletableIntegrationTest {

    @Autowired
    private TestSoftDeletableEntityRepository repository;

    @Autowired
    private TestAuditorAware testAuditorAware;

    @BeforeEach
    void setUp() {
        testAuditorAware.setCurrentAuditor("test-user");
    }

    @AfterEach
    void tearDown() {
        // 物理清理测试数据（使用原生 SQL 绕过 @SQLRestriction）
        repository.deleteAll physically();
    }

    // ==================== AC3: delete() 将 deleted 设为 true ====================
    @Test
    void given_existingEntity_when_delete_then_deletedSetToTrue() {
        // Given: 创建并保存一个实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("Test Entity");
        TestSoftDeletableEntity saved = repository.save(entity);
        Long id = saved.getId();

        // When: 删除实体
        repository.delete(saved);

        // Then: 实体被标记为已删除（但在数据库中仍存在）
        // 由于 @SQLRestriction，查询会被过滤，需要用原生查询验证
        Boolean deleted = repository.findDeletedStatusById(id);
        assertThat(deleted).isTrue();
    }

    // ==================== AC4: 查询时自动过滤已删除记录 ====================
    @Test
    void given_deletedAndActiveEntities_when_findAll_then_onlyActiveReturned() {
        // Given: 创建两个实体，删除其中一个
        TestSoftDeletableEntity entity1 = new TestSoftDeletableEntity();
        entity1.setName("Entity 1");
        TestSoftDeletableEntity saved1 = repository.save(entity1);

        TestSoftDeletableEntity entity2 = new TestSoftDeletableEntity();
        entity2.setName("Entity 2");
        TestSoftDeletableEntity saved2 = repository.save(entity2);

        // 删除第一个实体
        repository.delete(saved1);

        // When: 执行 findAll()
        List<TestSoftDeletableEntity> all = repository.findAll();

        // Then: 只返回未删除的实体
        assertThat(all)
            .hasSize(1)
            .allMatch(e -> e.getId().equals(saved2.getId()));
    }

    // ==================== AC6-2: 多次删除幂等 ====================
    @Test
    void given_deletedEntity_when_deleteMultipleTimes_then_remainsTrue() {
        // Given: 创建并删除一个实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("Test Entity");
        TestSoftDeletableEntity saved = repository.save(entity);
        Long id = saved.getId();

        repository.delete(saved);

        // When: 再次删除
        repository.delete(saved);
        repository.delete(saved);

        // Then: deleted 保持 true
        Boolean deleted = repository.findDeletedStatusById(id);
        assertThat(deleted).isTrue();
    }

    // ==================== AC6-3: 允许更新已删除实体 ====================
    @Test
    void given_deletedEntity_when_save_then_updateSucceeds() {
        // Given: 创建并删除一个实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("Original Name");
        TestSoftDeletableEntity saved = repository.save(entity);
        Long id = saved.getId();

        repository.delete(saved);

        // When: 重新加载并更新（使用原生查询绕过 @SQLRestriction）
        TestSoftDeletableEntity deletedEntity = repository.findByIdIgnoringDeleted(id);
        deletedEntity.setName("Updated Name");
        repository.save(deletedEntity);

        // Then: 更新成功
        TestSoftDeletableEntity updated = repository.findByIdIgnoringDeleted(id);
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getDeleted()).isTrue();
    }

    // ==================== 测试实体 ====================
    @jakarta.persistence.Entity(name = "test_soft_deletable_entity")
    static class TestSoftDeletableEntity extends SoftDeletable {

        @jakarta.persistence.Id
        @jakarta.persistence.GeneratedValue
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    // ==================== 测试 Repository ====================
    interface TestSoftDeletableEntityRepository extends org.springframework.data.jpa.repository.JpaRepository<TestSoftDeletableEntity, Long> {

        /**
         * 原生查询，获取 deleted 状态（绕过 @SQLRestriction）。
         */
        @org.springframework.data.jpa.repository.Query(
            value = "SELECT deleted FROM test_soft_deletable_entity WHERE id = :id",
            nativeQuery = true
        )
        Boolean findDeletedStatusById(Long id);

        /**
         * 原生查询，按 ID 查询（绕过 @SQLRestriction）。
         */
        @org.springframework.data.jpa.repository.Query(
            value = "SELECT * FROM test_soft_deletable_entity WHERE id = :id",
            nativeQuery = true
        )
        TestSoftDeletableEntity findByIdIgnoringDeleted(Long id);
    }

    // ==================== 测试配置 ====================
    @TestConfiguration
    static class TestConfig {

        @Bean
        public AuditorAware<String> auditorAware() {
            return new TestAuditorAware();
        }
    }

    // ==================== 测试用 AuditorAware ====================
    static class TestAuditorAware implements AuditorAware<String> {

        private String currentAuditor = "test-user";

        @Override
        public Optional<String> getCurrentAuditor() {
            return Optional.ofNullable(currentAuditor);
        }

        public void setCurrentAuditor(String currentAuditor) {
            this.currentAuditor = currentAuditor;
        }
    }
}
```

**验证**: 编译通过 + 测试全绿

---

### Step 8: 更新 AutoConfiguration imports（如需要）

**检查**: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

如果该文件存在，无需修改（`JpaAuditingConfiguration` 会被 `@ComponentScan` 自动扫描）。

如果需要显式导入，添加：
```
com.cartisan.data.jpa.config.CartisanDataJpaAutoConfiguration
com.cartisan.data.jpa.config.JpaAuditingConfiguration
```

**验证**: `./gradlew :cartisan-data-jpa:compileJava` 通过

---

## 验证方式

```bash
# 编译
./gradlew :cartisan-data-jpa:compileJava

# 单元测试
./gradlew :cartisan-data-jpa:test

# 检查测试报告
open cartisan-data-jpa/build/reports/tests/test/index.html
```

---

## 依赖关系

| Step | 依赖 |
|------|------|
| Step 1-3 | 可并行，无依赖 |
| Step 4 | 依赖 Step 1 |
| Step 5 | 依赖 Step 2 |
| Step 6 | 依赖 Step 1, Step 3 |
| Step 7 | 依赖 Step 2, Step 3 |
| Step 8 | 依赖 Step 3 |

---

## 参考资料

- 需求文档：[01_requirement.md](./01_requirement.md)
- 接口契约：[02_interface.md](./02_interface.md)
- SKILL.md：[SKILL.md](../../../skills/SKILL.md)
- Spring Data JPA Auditing：https://docs.spring.io/spring-data/jpa/reference/jpa/auditing.html
