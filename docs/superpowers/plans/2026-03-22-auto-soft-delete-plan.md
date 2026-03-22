# 自动软删除支持实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `repository.delete(entity)` 和 `repository.deleteById(id)` 对软删除实体自动执行软删除操作

**Architecture:** 在 `BaseRepositoryImpl` 中重写 `delete()` 系列方法，检测 `SoftDeletable` 实体并调用 `markAsDeleted()` + `save()`；通过 `JpaRepositoryFactoryEntryCustomizer` 全局注册基类

**Tech Stack:** Spring Data JPA, Hibernate, JUnit 5, AssertJ

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `SoftDeletable.java` | 修改 | 添加 `markAsDeleted()` 领域方法 |
| `BaseRepositoryImpl.java` | 修改 | 重写 `delete()`, `deleteById()`, `deleteAll()`, `deleteAllById()` |
| `CartisanDataJpaAutoConfiguration.java` | 修改 | 添加 `JpaRepositoryFactoryEntryCustomizer` Bean |
| `AutoDeleteSoftDeletableIntegrationTest.java` | 创建 | 集成测试验证自动软删除行为 |
| `SoftDeletableIntegrationTest.java` | 修改 | 更新现有测试（保留兼容性） |

---

## Task 1: SoftDeletable 添加 markAsDeleted() 方法

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/SoftDeletable.java`

- [ ] **Step 1: 添加 markAsDeleted() 领域方法**

在 `SoftDeletable` 类中添加公共方法：

```java
/**
 * 标记为已删除（领域方法）。
 *
 * <p>供 Repository.delete() 调用，业务端通常不需要直接调用。</p>
 *
 * @since 0.3.0
 */
public void markAsDeleted() {
    this.deleted = true;
}
```

插入位置：在 `isDeleted()` 方法之前。

- [ ] **Step 2: 编译验证**

```bash
./gradlew :cartisan-data-jpa:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 运行现有 SoftDeletable 测试**

```bash
./gradlew :cartisan-data-jpa:test --tests SoftDeletableTest
```

Expected: PASSED

- [ ] **Step 4: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/SoftDeletable.java
git commit -m "feat(domain): add markAsDeleted() method to SoftDeletable

Provides domain method for automatic soft deletion via repository.delete().

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: BaseRepositoryImpl 重写 delete() 方法

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java`

- [ ] **Step 1: 添加 delete() 方法重写**

在 `save()` 方法之后添加：

```java
/**
 * 删除实体，软删除实体自动标记为已删除。
 *
 * <p>如果实体实现了 {@link com.cartisan.data.jpa.domain.SoftDeletable}，
 * 则调用 {@code markAsDeleted()} 并保存，否则执行物理删除。</p>
 *
 * @param entity 要删除的实体，不能为 null
 */
@Override
public void delete(T entity) {
    if (entity instanceof com.cartisan.data.jpa.domain.SoftDeletable softDeletable) {
        softDeletable.markAsDeleted();
        save(entity);  // 复用 save() 的事件发布逻辑
    } else {
        super.delete(entity);  // 非软删除实体，物理删除
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :cartisan-data-jpa:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java
git commit -m "feat(repo): override delete() to support auto soft deletion

SoftDeletable entities are marked as deleted instead of being physically removed.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: BaseRepositoryImpl 重写 deleteById() 方法

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java`

- [ ] **Step 1: 添加 deleteById() 方法重写**

在 `delete()` 方法之后添加：

```java
/**
 * 根据 ID 删除实体，软删除实体自动标记为已删除。
 *
 * <p>先通过 ID 查找实体，然后调用 {@link #delete(Object)}。</p>
 *
 * @param id 实体 ID，不能为 null
 * @throws IllegalArgumentException 如果 id 为 null
 */
@Override
public void deleteById(ID id) {
    findById(id).ifPresent(this::delete);
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :cartisan-data-jpa:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java
git commit -m "feat(repo): override deleteById() to support auto soft deletion

Delegates to delete() after finding entity by ID.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: BaseRepositoryImpl 重写 deleteAll(Iterable) 方法

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java`

- [ ] **Step 1: 添加 deleteAll(Iterable) 方法重写**

在 `deleteById()` 方法之后添加：

```java
/**
 * 批量删除实体，软删除实体自动标记为已删除。
 *
 * <p>实现了 {@link SoftDeletable} 的实体执行软删除，其他实体执行物理删除。</p>
 *
 * @param entities 要删除的实体集合，不能为 null
 */
@Override
public void deleteAll(Iterable<? extends T> entities) {
    java.util.List<T> softDeletable = new java.util.ArrayList<>();
    java.util.List<T> physicalDelete = new java.util.ArrayList<>();

    entities.forEach(e -> {
        if (e instanceof com.cartisan.data.jpa.domain.SoftDeletable) {
            softDeletable.add(e);
        } else {
            physicalDelete.add(e);
        }
    });

    // 软删除：标记并保存
    softDeletable.forEach(e -> ((com.cartisan.data.jpa.domain.SoftDeletable) e).markAsDeleted());
    if (!softDeletable.isEmpty()) {
        saveAll(softDeletable);
    }

    // 物理删除
    if (!physicalDelete.isEmpty()) {
        super.deleteAll(physicalDelete);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :cartisan-data-jpa:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java
git commit -m "feat(repo): override deleteAll(Iterable) to support auto soft deletion

Handles mixed soft-deletable and non-soft-deletable entities.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: BaseRepositoryImpl 重写 deleteAll() 和 deleteAllById() 方法

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java`

- [ ] **Step 1: 添加私有辅助方法 isSoftDeletableEntityType()**

在类的末尾（最后一个 `}` 之前）添加：

```java
/**
 * 判断当前 Repository 的实体类型是否支持软删除。
 *
 * @return true 如果实体类型是 SoftDeletable 的子类
 */
private boolean isSoftDeletableEntityType() {
    return com.cartisan.data.jpa.domain.SoftDeletable.class.isAssignableFrom(getDomainClass());
}
```

- [ ] **Step 2: 添加 deleteAll() 方法重写**

在 `deleteAll(Iterable)` 方法之后添加：

```java
/**
 * 删除所有实体。
 *
 * <p>如果实体类型支持软删除，则批量更新所有记录的 deleted 标记，
 * 否则执行物理删除。</p>
 */
@Override
public void deleteAll() {
    if (isSoftDeletableEntityType()) {
        // 软删除：批量更新所有记录
        jakarta.persistence.Query query = getEntityManager().createQuery(
            "UPDATE " + getDomainClass().getSimpleName() + " e SET e.deleted = true"
        );
        query.executeUpdate();
    } else {
        super.deleteAll();
    }
}
```

- [ ] **Step 3: 添加 deleteAllById() 方法重写**

在 `deleteAll()` 方法之后添加：

```java
/**
 * 根据 ID 批量删除实体。
 *
 * <p>先查找所有实体，然后调用 {@link #deleteAll(Iterable)}。</p>
 *
 * @param ids 实体 ID 集合，不能为 null
 */
@Override
public void deleteAllById(Iterable<? extends ID> ids) {
    java.util.List<T> entities = new java.util.ArrayList<>();
    ids.forEach(id -> findById(id).ifPresent(entities::add));
    deleteAll(entities);
}
```

- [ ] **Step 4: 编译验证**

```bash
./gradlew :cartisan-data-jpa:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java
git commit -m "feat(repo): override deleteAll() and deleteAllById() for soft deletion

Batch operations now support soft-deletable entities.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: AutoConfiguration 添加全局配置

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java`

- [ ] **Step 1: 添加 import 语句**

在现有 import 之后添加：

```java
import org.springframework.data.jpa.repository.config.JpaRepositoryFactoryEntryCustomizer;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
```

- [ ] **Step 2: 添加 repositoryFactoryEntryCustomizer Bean**

在 `configureDomainEventPublisherHolder` 方法之后添加：

```java
/**
 * 全局配置 Repository 基类。
 *
 * <p>所有继承 {@link com.cartisan.data.jpa.repository.BaseRepository} 的接口
 * 自动使用 {@link com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl}，
 * 业务端无需手动指定 {@code repositoryBaseClass}。</p>
 *
 * <p>参考 @ docs/PITFALLS.md 规则 BOOT-001</p>
 *
 * @return JpaRepositoryFactoryEntryCustomizer Bean
 */
@Bean
public JpaRepositoryFactoryEntryCustomizer repositoryFactoryEntryCustomizer() {
    return (JpaRepositoryFactoryBean<?, ?, ?> factoryBean) -> {
        factoryBean.setRepositoryBaseClass(BaseRepositoryImpl.class);
    };
}
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew :cartisan-data-jpa:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java
git commit -m "feat(config): add global Repository base class registration

Uses JpaRepositoryFactoryEntryCustomizer to automatically apply
BaseRepositoryImpl to all repositories. Business projects no longer
need to specify repositoryBaseClass manually.

Ref: PITFALLS.md rule BOOT-001

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: 创建自动软删除集成测试

**Files:**
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AutoDeleteSoftDeletableIntegrationTest.java`

- [ ] **Step 1: 创建测试类**

创建新文件 `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AutoDeleteSoftDeletableIntegrationTest.java`：

```java
package com.cartisan.data.jpa.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动软删除功能集成测试。
 *
 * <p>验证 repository.delete() 和 repository.deleteById() 自动触发软删除。</p>
 */
@DataJpaTest
@EntityScan(basePackageClasses = TestSoftDeletableEntity.class)
class AutoDeleteSoftDeletableIntegrationTest {

    @Autowired
    private TestSoftDeletableEntityRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    // ==================== delete(entity) 自动软删除 ====================
    @Test
    void given_existingEntity_when_delete_then_markedAsDeleted() {
        // Given: 创建实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("To Delete");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        assertThat(repository.findAll()).hasSize(1);

        // When: 调用 delete(entity)
        repository.delete(saved);

        // Then: 实体被标记为已删除，查询时过滤
        assertThat(repository.findAll()).isEmpty();
    }

    // ==================== deleteById(id) 自动软删除 ====================
    @Test
    @Transactional
    void given_existingEntity_when_deleteById_then_markedAsDeleted() {
        // Given: 创建实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("To Delete");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        assertThat(repository.findAll()).hasSize(1);

        // When: 调用 deleteById(id)
        repository.deleteById(saved.getId());

        // Then: 实体被标记为已删除
        assertThat(repository.findAll()).isEmpty();
    }

    // ==================== deleteAll(Iterable) 混合实体 ====================
    @Test
    void given_multipleEntities_when_deleteAll_then_allMarkedAsDeleted() {
        // Given: 创建多个实体
        TestSoftDeletableEntity entity1 = new TestSoftDeletableEntity();
        entity1.setName("Entity 1");
        TestSoftDeletableEntity entity2 = new TestSoftDeletableEntity();
        entity2.setName("Entity 2");
        TestSoftDeletableEntity entity3 = new TestSoftDeletableEntity();
        entity3.setName("Entity 3");

        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);
        repository.flush();

        assertThat(repository.findAll()).hasSize(3);

        // When: 批量删除
        repository.deleteAll(List.of(entity1, entity2, entity3));

        // Then: 全部被标记为已删除
        assertThat(repository.findAll()).isEmpty();
    }

    // ==================== deleteAll() 批量删除 ====================
    @Test
    void given_multipleEntities_when_deleteAllAll_then_allMarkedAsDeleted() {
        // Given: 创建多个实体
        TestSoftDeletableEntity entity1 = new TestSoftDeletableEntity();
        entity1.setName("Entity 1");
        TestSoftDeletableEntity entity2 = new TestSoftDeletableEntity();
        entity2.setName("Entity 2");

        repository.save(entity1);
        repository.save(entity2);
        repository.flush();

        assertThat(repository.findAll()).hasSize(2);

        // When: 删除所有
        repository.deleteAll();

        // Then: 全部被标记为已删除
        assertThat(repository.findAll()).isEmpty();
    }

    // ==================== 通过 ID 仍可找到已删除实体 ====================
    @Test
    @Transactional
    void given_deletedEntity_when_findById_then_stillFound() {
        // Given: 创建并删除实体
        TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
        entity.setName("Deleted");
        TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

        repository.delete(saved);

        // When: 通过 ID 查询
        TestSoftDeletableEntity found = repository.findById(saved.getId()).orElse(null);

        // Then: 仍能找到（但 findAll() 过滤）
        assertThat(found).isNotNull();
        assertThat(found.isDeleted()).isTrue();
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
./gradlew :cartisan-data-jpa:test --tests AutoDeleteSoftDeletableIntegrationTest
```

Expected: PASSED

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AutoDeleteSoftDeletableIntegrationTest.java
git commit -m "test(repo): add AutoDeleteSoftDeletableIntegrationTest

Verifies automatic soft deletion via delete() and deleteById().

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: 更新现有 SoftDeletableIntegrationTest（兼容性）

**Files:**
- Modify: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/SoftDeletableIntegrationTest.java`

- [ ] **Step 1: 添加 delete() 自动软删除测试**

在 `SoftDeletableIntegrationTest` 类中添加新测试方法（在文件末尾 `}` 之前）：

```java
// ==================== 新增：自动软删除测试 ====================
@Test
void given_existingEntity_when_deleteAuto_then_markedAsDeleted() {
    // Given: 创建实体
    TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
    entity.setName("Auto Delete");
    TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

    assertThat(repository.findAll()).hasSize(1);

    // When: 使用 repository.delete() 自动软删除
    repository.delete(saved);

    // Then: 实体被标记为已删除，常规查询过滤
    assertThat(repository.findAll()).isEmpty();
}

@Test
void given_existingEntity_when_deleteByIdAuto_then_markedAsDeleted() {
    // Given: 创建实体
    TestSoftDeletableEntity entity = new TestSoftDeletableEntity();
    entity.setName("Auto Delete By Id");
    TestSoftDeletableEntity saved = repository.saveAndFlush(entity);

    assertThat(repository.findAll()).hasSize(1);

    // When: 使用 repository.deleteById() 自动软删除
    repository.deleteById(saved.getId());

    // Then: 实体被标记为已删除
    assertThat(repository.findAll()).isEmpty();
}
```

- [ ] **Step 2: 运行所有测试**

```bash
./gradlew :cartisan-data-jpa:test --tests SoftDeletableIntegrationTest
```

Expected: PASSED

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/SoftDeletableIntegrationTest.java
git commit -m "test(repo): add auto soft delete tests to SoftDeletableIntegrationTest

Ensures backward compatibility with new automatic soft deletion behavior.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: 运行完整测试套件

**Files:**
- All

- [ ] **Step 1: 运行 cartisan-data-jpa 全量测试**

```bash
./gradlew :cartisan-data-jpa:test
```

Expected: ALL TESTS PASSED

- [ ] **Step 2: 检查测试覆盖率**

```bash
./gradlew :cartisan-data-jpa:test jacocoTestReport
```

Expected: Coverage report generated

- [ ] **Step 3: 如有失败，分析并修复**

查看失败日志，修复问题后重新运行测试。

- [ ] **Step 4: 提交（如有修复）**

```bash
git add -A
git commit -m "fix(repo): resolve test failures in soft delete implementation

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: 文档更新

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/SoftDeletable.java`
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/BaseRepository.java`

- [ ] **Step 1: 更新 SoftDeletable JavaDoc**

更新 `SoftDeletable.java` 的类级别注释，添加自动软删除说明：

```java
/**
 * 可软删除实体基类。
 *
 * <p>继承 {@link Auditable}，增加软删除能力：</p>
 * <ul>
 *   <li>{@code deleted} 字段标记是否已删除</li>
 *   <li>{@code @SQLRestriction} 在查询时自动过滤 {@code deleted = true} 的记录</li>
 *   <li>调用 {@code repository.delete(entity)} 自动软删除（无需手动设置 {@code deleted}）</li>
 * </ul>
 *
 * <h3>软删除行为</h3>
 * <ul>
 *   <li>调用 {@code repository.delete(entity)} 自动将 {@code deleted} 设为 {@code true}</li>
 *   <li>调用 {@code repository.deleteById(id)} 自动将对应记录的 {@code deleted} 设为 {@code true}</li>
 *   <li>所有查询（如 {@code findAll()}）自动排除 {@code deleted = true} 的记录</li>
 * </ul>
 *
 * ...
 * (保留原有文档)
 * ...
 */
```

- [ ] **Step 2: 更新 BaseRepository JavaDoc**

更新 `BaseRepository.java` 的类级别注释：

```java
/**
 * 聚合根仓储基类，仅聚合根类型可声明 Repository。
 *
 * <p>继承 {@link JpaRepository} 与 {@link JpaSpecificationExecutor}，具体实现由
 * {@code BaseRepositoryImpl} 提供，save 时自动发布领域事件。</p>
 *
 * <p>软删除支持：对于实现了 {@link com.cartisan.data.jpa.domain.SoftDeletable}
 * 的实体，调用 {@code delete()} 或 {@code deleteById()} 会自动执行软删除。</p>
 *
 * <p>示例：</p>
 * <pre>{@code
 * public interface OrderRepository extends BaseRepository<Order, Long> {}
 *
 * // 使用
 * orderRepository.delete(order);          // 自动软删除（如果 Order 继承 SoftDeletable）
 * orderRepository.deleteById(orderId);    // 自动软删除
 * }</pre>
 *
 * ...
 * (保留原有文档)
 * ...
 */
```

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/SoftDeletable.java
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/BaseRepository.java
git commit -m "docs(repo): update JavaDoc for automatic soft deletion

Clarifies that delete() and deleteById() automatically perform soft deletion
for SoftDeletable entities.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: 验证自动配置生效

**Files:**
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/config/RepositoryFactoryCustomizerTest.java`

- [ ] **Step 1: 创建自动配置测试**

创建新文件 `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/config/RepositoryFactoryCustomizerTest.java`：

```java
package com.cartisan.data.jpa.config;

import com.cartisan.data.jpa.domain.SoftDeletable;
import com.cartisan.data.jpa.repository.BaseRepository;
import com.cartisan.data.jpa.repository.impl.BaseRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository 工厂自动配置测试。
 *
 * <p>验证 JpaRepositoryFactoryEntryCustomizer 正确应用 BaseRepositoryImpl 作为基类。</p>
 */
@DataJpaTest
class RepositoryFactoryCustomizerTest {

    @Autowired
    private JpaRepositoryFactoryBean<?, ?, ?> factoryBean;

    @Test
    void repositoryFactoryEntryCustomizer_isApplied() {
        // Then: BaseRepositoryImpl 被设置为基类
        assertThat(factoryBean.getRepositoryBaseClass()).isEqualTo(BaseRepositoryImpl.class);
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
./gradlew :cartisan-data-jpa:test --tests RepositoryFactoryCustomizerTest
```

Expected: PASSED

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/config/RepositoryFactoryCustomizerTest.java
git commit -m "test(config): add RepositoryFactoryCustomizerTest

Verifies that JpaRepositoryFactoryEntryCustomizer correctly applies
BaseRepositoryImpl as the global repository base class.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 12: 最终验证

**Files:**
- All

- [ ] **Step 1: 编译整个项目**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行 cartisan-data-jpa 所有测试**

```bash
./gradlew :cartisan-data-jpa:test
```

Expected: ALL TESTS PASSED

- [ ] **Step 3: 检查是否有遗留的 TODO 或 FIXME**

```bash
grep -r "TODO\|FIXME" cartisan-data-jpa/src/main/java/
```

Expected: No relevant TODOs/FIXMEs in new code

- [ ] **Step 4: 最终提交（如有调整）**

```bash
git add -A
git commit -m "chore: final cleanup for auto soft deletion feature

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## 完成标准

- [ ] 所有 `delete()`, `deleteById()`, `deleteAll()`, `deleteAllById()` 方法正确处理软删除
- [ ] 非软删除实体行为不变（物理删除）
- [ ] 业务端无需手动指定 `repositoryBaseClass`
- [ ] 所有测试通过
- [ ] JavaDoc 已更新

## 参考资料

- 设计文档: `docs/superpowers/specs/2026-03-22-auto-soft-delete-design.md`
- PITFALLS.md 规则 BOOT-001: `docs/PITFALLS.md#L1286`
