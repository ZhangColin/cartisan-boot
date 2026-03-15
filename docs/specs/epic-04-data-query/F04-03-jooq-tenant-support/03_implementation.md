# F04-03 JooqTenantSupport Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 创建 JooqTenantSupport 工具类，为 jOOQ 查询提供多租户条件生成方法

**Architecture:** 静态工具类设计，集成 cartisan-security 的 TenantContext，按表字段生成租户过滤条件。无租户上下文时返回 noCondition() 优雅降级。

**Tech Stack:** Java 21, jOOQ, TenantContext (ScopedValue), JUnit 5, AssertJ

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| Modify | `cartisan-data-query/build.gradle.kts` | 添加 `compileOnly(project(":cartisan-security"))` |
| Create | `cartisan-data-query/src/main/java/com/cartisan/data/query/support/package-info.java` | support 包声明 |
| Create | `cartisan-data-query/src/main/java/com/cartisan/data/query/support/JooqTenantSupport.java` | 工具类实现 |
| Create | `cartisan-data-query/src/test/java/com/cartisan/data/query/support/JooqTenantSupportTest.java` | 单元测试 |

---

## Task 1: 更新 Gradle 依赖配置

**Files:**
- Modify: `cartisan-data-query/build.gradle.kts`

**Step 1: 添加 cartisan-security 依赖**

在 `dependencies` 块中添加 `compileOnly` 依赖：

```kotlin
dependencies {
    // Platform - versions managed by cartisan-dependencies
    api(platform(project(":cartisan-dependencies")))

    // cartisan-web - 复用 PageResponse<T>（api 声明，传递给使用者）
    api(project(":cartisan-web"))

    // jOOQ Core
    implementation("org.jooq:jooq")

    // Spring Boot AutoConfiguration 支持
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")

    // cartisan-security - 可选依赖（编译期需要，运行时由使用方提供）
    // 用于 JooqTenantSupport 访问 TenantContext
    compileOnly(project(":cartisan-security"))

    // 配置属性元数据处理器（IDE 自动补全提示）
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.4.0")

    // Testing
    testImplementation(project(":cartisan-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    // PostgreSQL JDBC 驱动（集成测试需要）
    testImplementation("org.postgresql:postgresql")
}
```

**Step 2: 验证编译**

Run: `./gradlew :cartisan-data-query:compileJava`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add cartisan-data-query/build.gradle.kts
git commit -m "feat(data-query): add compileOnly dependency on cartisan-security for F04-03"
```

---

## Task 2: 创建 support 包结构

**Files:**
- Create: `cartisan-data-query/src/main/java/com/cartisan/data/query/support/package-info.java`

**Step 1: 创建 package-info.java**

```java
/**
 * jOOQ 查询支持工具类包。
 *
 * <p>提供 jOOQ 查询的辅助工具，包括多租户条件生成、排序支持等。</p>
 *
 * <p>本包中的类为纯静态工具类，无状态，线程安全。</p>
 *
 * @since 0.3.0
 */
package com.cartisan.data.query.support;
```

**Step 2: 验证编译**

Run: `./gradlew :cartisan-data-query:compileJava`
Expected: SUCCESS

**Step 3: Commit**

```bash
git add cartisan-data-query/src/main/java/com/cartisan/data/query/support/
git commit -m "feat(data-query): create support package for jOOQ query utilities"
```

---

## Task 3: 编写单元测试（红灯）

**Files:**
- Create: `cartisan-data-query/src/test/java/com/cartisan/data/query/support/JooqTenantSupportTest.java`

**Step 1: 创建测试类框架**

```java
package com.cartisan.data.query.support;

import org.jooq.DSL;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JooqTenantSupport 单元测试。
 * <p>
 * 测试多租户条件生成逻辑：
 * - 有租户上下文时返回等值条件
 * - 无租户上下文时返回 noCondition
 * - null 参数抛出 NullPointerException
 * </p>
 */
class JooqTenantSupportTest {

    // 注意：此处需要 mock jOOQ 的 TableField，或使用 jOOQ 生成的测试表
    // 为简化测试，暂时使用 DSL.condition() 创建模拟字段
    // 实际实现时需要调整 mock 方式

    @Test
    void givenNoTenantContext_whenEqTenantId_thenReturnsNoCondition() {
        // Given - 无租户上下文（默认状态）

        // When - 调用 eqTenantId
        Condition result = JooqTenantSupport.eqTenantId(createMockTenantIdField());

        // Then - 应返回 noCondition
        assertThat(result).isEqualTo(DSL.noCondition());
    }

    @Test
    void givenTenantContext_whenEqTenantId_thenReturnsEqCondition() {
        // Given
        Long expectedTenantId = 123L;
        AtomicReference<Condition> result = new AtomicReference<>();

        // 在租户上下文中执行
        com.cartisan.security.context.TenantContext.runWithTenant(expectedTenantId, () -> {
            // When
            result.set(JooqTenantSupport.eqTenantId(createMockTenantIdField()));
        });

        // Then - 返回的条件应该是等值条件
        assertThat(result.get()).isNotNull();
        // 注意：此处断言方式取决于实际实现，可能需要调整
        // 可以通过 Condition.toString() 或反射验证内部结构
    }

    @Test
    void givenNullField_whenEqTenantId_thenThrowsNullPointerException() {
        // Given - null 参数
        TableField<?, Long> nullField = null;

        // When & Then - 应抛出 NullPointerException
        assertThatThrownBy(() -> JooqTenantSupport.eqTenantId(nullField))
            .isInstanceOf(NullPointerException.class);
    }

    /**
     * 创建模拟的租户 ID 字段。
     * 注意：实际实现时可能需要使用 jOOQ 的 Generated 表或 Mock 对象。
     */
    private TableField<?, Long> createMockTenantIdField() {
        // 方案1：使用 jOOQ DSL 创建虚拟表（推荐）
        return DSL.table("test_table").field("tenant_id", Long.class);

        // 方案2：若项目有 jOOQ generated 代码，使用真实表字段
        // return com.example.db.Tables.TEST.TENANT_ID;

        // 方案3：使用 Mockito mock
        // TableField<?, Long> mockField = mock(TableField.class);
        // when(mockField.getName()).thenReturn("tenant_id");
        // return mockField;
    }
}
```

**Step 2: 验证编译（此时 JooqTenantSupport 不存在，应编译失败）**

Run: `./gradlew :cartisan-data-query:compileTestJava`
Expected: FAIL with "cannot find symbol: class JooqTenantSupport"

**Step 3: 暂存测试文件（不提交，等实现完成后再一起提交）**

```bash
git add cartisan-data-query/src/test/java/com/cartisan/data/query/support/JooqTenantSupportTest.java
```

---

## Task 4: 实现 JooqTenantSupport（绿灯）

**Files:**
- Create: `cartisan-data-query/src/main/java/com/cartisan/data/query/support/JooqTenantSupport.java`

**Step 1: 创建 JooqTenantSupport 类**

```java
package com.cartisan.data.query.support;

import com.cartisan.security.context.TenantContext;
import org.jooq.Condition;
import org.jooq.DSL;
import org.jooq.TableField;

/**
 * jOOQ 多租户查询支持工具类。
 *
 * <p>提供租户过滤条件的便捷生成方法，支持按表字段添加租户等值条件。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * import static com.cartisan.data.query.support.JooqTenantSupport.eqTenantId;
 * import static com.example.db.Tables.USER;
 *
 * // 在 WHERE 子句中使用
 * List<UserRecord> users = dslContext.selectFrom(USER)
 *     .where(USER.NAME.like("%name%"))
 *     .and(eqTenantId(USER.TENANT_ID))  // 自动添加租户过滤
 *     .fetch();
 * }</pre>
 *
 * <h3>行为说明</h3>
 * <ul>
 *   <li>有租户上下文时：返回 {@code tenantIdField.eq(tenantId)} 等值条件</li>
 *   <li>无租户上下文时：返回 {@link DSL#noCondition()}，不影响查询</li>
 * </ul>
 *
 * <h3>依赖说明</h3>
 * <p>本类依赖 {@code cartisan-security} 的 {@link TenantContext}。
 * 使用 {@code compileOnly} 依赖范围，运行时由使用者引入 {@code cartisan-security}。</p>
 *
 * @since 0.3.0
 */
public final class JooqTenantSupport {

    /**
     * 防止实例化。
     *
     * @throws UnsupportedOperationException 始终抛出，表示工具类不可实例化
     */
    private JooqTenantSupport() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 为指定表的租户字段生成等值过滤条件。
     *
     * <p>根据当前租户上下文生成条件：</p>
     * <ul>
     *   <li>有租户：返回 {@code tenantIdField.eq(tenantId)}</li>
     *   <li>无租户：返回 {@link DSL#noCondition()}（空条件）</li>
     * </ul>
     *
     * @param tenantIdField 表的租户 ID 字段（如 {@code USER.TENANT_ID}）
     * @return jOOQ Condition 对象，永远非 null
     * @throws NullPointerException 若 {@code tenantIdField} 为 null
     */
    public static Condition eqTenantId(TableField<?, Long> tenantIdField) {
        // 前置检查
        if (tenantIdField == null) {
            throw new NullPointerException("tenantIdField cannot be null");
        }

        // 获取当前租户 ID
        Long tenantId = TenantContext.getCurrentTenantId();

        // 根据租户上下文返回条件
        if (tenantId != null) {
            return tenantIdField.eq(tenantId);
        } else {
            return DSL.noCondition();
        }
    }
}
```

**Step 2: 验证编译**

Run: `./gradlew :cartisan-data-query:compileJava`
Expected: SUCCESS

**Step 3: 运行测试（红灯验证）**

Run: `./gradlew :cartisan-data-query:test --tests JooqTenantSupportTest`
Expected:
- 有测试通过（noCondition 场景）
- 有测试需要调整（eq 场景的断言方式）

**注意：** jOOQ 的 `Condition` 对象无法直接比较相等性，需要调整测试断言方式。见下方的测试调整步骤。

---

## Task 5: 调整测试断言

**Files:**
- Modify: `cartisan-data-query/src/test/java/com/cartisan/data/query/support/JooqTenantSupportTest.java`

**Step 1: 修正 eq 场景的断言**

由于 jOOQ `Condition` 不支持直接 equals 比较，改用字符串表示验证：

```java
@Test
void givenTenantContext_whenEqTenantId_thenReturnsEqCondition() {
    // Given
    Long expectedTenantId = 123L;
    AtomicReference<Condition> result = new AtomicReference<>();

    // 在租户上下文中执行
    com.cartisan.security.context.TenantContext.runWithTenant(expectedTenantId, () -> {
        // When
        result.set(JooqTenantSupport.eqTenantId(createMockTenantIdField()));
    });

    // Then - 返回的条件应该是等值条件
    // 验证条件不是 noCondition
    assertThat(result.get()).isNotEqualTo(DSL.noCondition());
    // 验证条件字符串包含租户 ID（间接验证是等值条件）
    assertThat(result.get().toString()).contains(String.valueOf(expectedTenantId));
}
```

**Step 2: 运行测试（绿灯验证）**

Run: `./gradlew :cartisan-data-query:test --tests JooqTenantSupportTest`
Expected: ALL TESTS PASS

**Step 3: 全量测试验证**

Run: `./gradlew :cartisan-data-query:test`
Expected: ALL TESTS PASS

**Step 4: 提交实现**

```bash
git add cartisan-data-query/src/main/java/com/cartisan/data/query/support/JooqTenantSupport.java
git add cartisan-data-query/src/test/java/com/cartisan/data/query/support/JooqTenantSupportTest.java
git commit -m "feat(data-query): implement JooqTenantSupport for multi-tenant queries"
```

---

## Task 6: 全量验证与文档

**Step 1: 运行全量检查**

Run: `./gradlew :cartisan-data-query:check`
Expected: SUCCESS（编译 + 测试 + ArchUnit）

**Step 2: 验证依赖范围正确**

检查 `cartisan-security` 是否正确标记为 `compileOnly`：

Run: `./gradlew :cartisan-data-query:dependencies --configuration compileClasspath | grep security`
Expected: 看到 `cartisan-security` 在 compileClasspath 中

**Step 3: 更新模块 package-info（可选）**

若需要，更新 `cartisan-data-query` 模块的 package-info，说明新增的 support 包。

---

## 验收检查清单

- [ ] Gradle 依赖添加成功，`compileOnly(project(":cartisan-security"))`
- [ ] `support` 包创建，包含 `package-info.java`
- [ ] `JooqTenantSupport.java` 实现完成，编译通过
- [ ] 单元测试全部通过（有租户、无租户、null 参数）
- [ ] 全量 `./gradlew :cartisan-data-query:check` 通过
- [ ] 代码符合 Java 编码规范（private 构造函数、final 类）
- [ ] JavaDoc 完整，包含使用示例

---

## 下一步

完成本 Feature 后，继续 F04-04（代码生成配置指南）或 F04-05（集成测试）。
