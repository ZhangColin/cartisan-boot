# F03-04 TenantContext — 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 实现多租户上下文工具类 TenantContext，使用 ScopedValue 存储，兼容 Virtual Threads

**架构方案：** 静态工具类 + ScopedValue 存储，通过 package-private 方法 `runWithTenant()` 绑定租户上下文

**技术栈：** Java 21+、ScopedValue、JUnit 5、AssertJ

---

## 目标复述

基于 [01_requirement.md](./01_requirement.md) 和 [02_interface.md](./02_interface.md)：

1. 实现 `TenantContext` 工具类，提供租户 ID 存取 API
2. 使用 `ScopedValue` 存储，确保 Virtual Threads 兼容
3. 提供 package-private 的 `runWithTenant()` 供 Filter 调用
4. 编写单元测试覆盖所有公共 API

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| Create | `cartisan-security/src/main/java/com/cartisan/security/context/TenantContext.java` | 租户上下文工具类 |
| Create | `cartisan-security/src/test/java/com/cartisan/security/context/TenantContextTest.java` | 单元测试 |

---

## 核心流程（伪代码）

```pseudocode
// 读取租户 ID
FUNCTION getCurrentTenantId()
    RETURN ScopedValue.getOrDefault(TENANT_ID, null)
END FUNCTION

// 绑定租户上下文
FUNCTION runWithTenant(tenantId, runnable)
    IF tenantId IS NOT NULL THEN
        ScopedValue.where(TENANT_ID, tenantId).run(runnable)
    ELSE
        runnable.run()
    END IF
END FUNCTION
```

---

## 原子任务清单

### Task 1: 创建 TenantContext 类骨架

**Files:**
- Create: `cartisan-security/src/main/java/com/cartisan/security/context/TenantContext.java`

**Step 1: 创建类骨架**

```java
package com.cartisan.security.context;

/**
 * 多租户上下文工具类。
 * <p>
 * 提供当前请求租户 ID 的存取访问，支持从 Header 或 Session 解析租户信息。
 * 使用 ScopedValue 实现，兼容 Virtual Threads。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 判断是否有租户上下文
 * if (TenantContext.hasTenant()) {
 *     Long tenantId = TenantContext.getCurrentTenantId();
 *     // 使用 tenantId...
 * }
 *
 * // 必须有租户的场景
 * Long tenantId = TenantContext.requireTenant();
 * // 使用 tenantId...
 *
 * // 可选租户的场景
 * Long tenantId = TenantContext.getCurrentTenantId();
 * if (tenantId != null) {
 *     // 使用 tenantId...
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
public final class TenantContext {

    /**
     * 租户 ID 的 ScopedValue 键。
     * <p>
     * 使用 ScopedValue 而非 ThreadLocal，确保在 Virtual Threads 环境下
     * 租户上下文能正确传递给子任务。
     * </p>
     */
    static final ScopedValue<Long> TENANT_ID = ScopedValue.newInstance();

    /**
     * 防止实例化。
     */
    private TenantContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // 公共 API 将在后续任务中添加
}
```

**Step 2: 编译验证**

```bash
./gradlew :cartisan-security:compileJava
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/context/TenantContext.java
git commit -m "feat(security): F03-04 创建 TenantContext 类骨架"
```

---

### Task 2: 编写 getCurrentTenantId 测试（红灯）

**Files:**
- Modify: `cartisan-security/src/test/java/com/cartisan/security/context/TenantContextTest.java`

**Step 1: 创建测试类并编写 getCurrentTenantId 测试**

```java
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
```

**Step 2: 编译验证测试代码**

```bash
./gradlew :cartisan-security:compileTestJava
```

Expected: BUILD SUCCESSFUL（测试代码编译通过，但实现类缺少方法）

**Step 3: 运行测试确认红灯**

```bash
./gradlew :cartisan-security:test --tests TenantContextTest
```

Expected: TEST FAILED（方法不存在）

**Step 4: Commit**

```bash
git add cartisan-security/src/test/java/com/cartisan/security/context/TenantContextTest.java
git commit -m "test(security): F03-04 编写 getCurrentTenantId 测试（红灯）"
```

---

### Task 3: 实现 getCurrentTenantId 和 runWithTenant（绿灯）

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/context/TenantContext.java`

**Step 1: 实现 getCurrentTenantId 和 runWithTenant**

在 TenantContext 类中添加以下方法：

```java
/**
 * 获取当前租户 ID。
 * <p>
 * 若当前请求未绑定租户上下文，返回 {@code null}。
 * </p>
 *
 * @return 租户 ID，未设置时返回 {@code null}
 */
public static Long getCurrentTenantId() {
    // 使用 getOrDefault 避免未绑定时抛 NoSuchElementException
    return ScopedValue.getOrDefault(TENANT_ID, null);
}

/**
 * 判断当前请求是否有租户上下文。
 *
 * @return 有租户返回 {@code true}，否则返回 {@code false}
 */
public static boolean hasTenant() {
    return getCurrentTenantId() != null;
}

/**
 * 获取当前租户 ID，若不存在则抛出异常。
 * <p>
 * 用于"必须有租户"的业务场景。
 * </p>
 *
 * @return 租户 ID
 * @throws IllegalStateException 当前无租户上下文
 */
public static Long requireTenant() {
    Long tenantId = getCurrentTenantId();
    if (tenantId == null) {
        throw new IllegalStateException("No tenant context available");
    }
    return tenantId;
}

/**
 * 在指定租户上下文中执行任务。
 * <p>
 * 该方法为 package-private，仅供 TenantContextFilter 调用。
 * </p>
 *
 * @param tenantId 租户 ID，可为 null
 * @param runnable 要执行的任务
 */
static void runWithTenant(Long tenantId, Runnable runnable) {
    if (tenantId != null) {
        ScopedValue.where(TENANT_ID, tenantId).run(runnable);
    } else {
        runnable.run();
    }
}
```

**Step 2: 运行测试确认绿灯**

```bash
./gradlew :cartisan-security:test --tests TenantContextTest
```

Expected: TEST PASSED

**Step 3: 运行全部测试确保无破坏**

```bash
./gradlew :cartisan-security:test
```

Expected: ALL TESTS PASSED

**Step 4: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/context/TenantContext.java
git commit -m "feat(security): F03-04 实现 TenantContext 公共 API"
```

---

### Task 4: 编写 hasTenant 测试（红灯）

**Files:**
- Modify: `cartisan-security/src/test/java/com/cartisan/security/context/TenantContextTest.java`

**Step 1: 添加 hasTenant 测试方法**

在 TenantContextTest 类中添加：

```java
@Test
void givenNoTenant_whenHasTenant_thenReturnsFalse() {
    // Given - 无租户上下文

    // When
    boolean hasTenant = TenantContext.hasTenant();

    // Then
    assertThat(hasTenant).isFalse();
}

@Test
void givenTenant_whenHasTenant_thenReturnsTrue() {
    // Given
    AtomicReference<Boolean> result = new AtomicReference<>();

    TenantContext.runWithTenant(456L, () -> {
        // When
        result.set(TenantContext.hasTenant());
    });

    // Then
    assertThat(result.get()).isTrue();
}
```

**Step 2: 运行测试**

```bash
./gradlew :cartisan-security:test --tests TenantContextTest.givenNoTenant_whenHasTenant_thenReturnsFalse
./gradlew :cartisan-security:test --tests TenantContextTest.givenTenant_whenHasTenant_thenReturnsTrue
```

Expected: TEST PASSED（hasTenant 已在 Task 3 中实现）

**Step 3: Commit（如果有新增）**

```bash
git add cartisan-security/src/test/java/com/cartisan/security/context/TenantContextTest.java
git commit -m "test(security): F03-04 添加 hasTenant 测试"
```

---

### Task 5: 编写 requireTenant 测试（红灯）

**Files:**
- Modify: `cartisan-security/src/test/java/com/cartisan/security/context/TenantContextTest.java`

**Step 1: 添加 requireTenant 测试方法**

在 TenantContextTest 类中添加：

```java
@Test
void givenNoTenant_whenRequireTenant_thenThrowsIllegalStateException() {
    // Given - 无租户上下文

    // When & Then
    assertThatThrownBy(TenantContext::requireTenant)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No tenant context");
}

@Test
void givenTenant_whenRequireTenant_thenReturnsTenantId() {
    // Given
    Long expectedTenantId = 789L;
    AtomicReference<Long> actualTenantId = new AtomicReference<>();

    TenantContext.runWithTenant(expectedTenantId, () -> {
        // When
        actualTenantId.set(TenantContext.requireTenant());
    });

    // Then
    assertThat(actualTenantId.get()).isEqualTo(expectedTenantId);
}
```

**Step 2: 运行测试**

```bash
./gradlew :cartisan-security:test --tests TenantContextTest.givenNoTenant_whenRequireTenant_thenThrowsIllegalStateException
./gradlew :cartisan-security:test --tests TenantContextTest.givenTenant_whenRequireTenant_thenReturnsTenantId
```

Expected: TEST PASSED（requireTenant 已在 Task 3 中实现）

**Step 3: Commit（如果有新增）**

```bash
git add cartisan-security/src/test/java/com/cartisan/security/context/TenantContextTest.java
git commit -m "test(security): F03-04 添加 requireTenant 测试"
```

---

### Task 6: 验证全部测试和构建

**Step 1: 运行模块全部测试**

```bash
./gradlew :cartisan-security:test
```

Expected: ALL TESTS PASSED

**Step 2: 运行 ArchUnit 检查**

```bash
./gradlew :cartisan-security:check
```

Expected: ARCHUNIT CHECKS PASSED

**Step 3: 运行整个项目构建**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

**Step 4: 查看测试报告**

```bash
open cartisan-security/build/reports/tests/test/index.html
```

确认：所有测试通过，覆盖率符合预期

---

## 实施检查清单

### 代码完成度

- [ ] TenantContext 类创建完成
- [ ] getCurrentTenantId() 实现并测试通过
- [ ] hasTenant() 实现并测试通过
- [ ] requireTenant() 实现并测试通过
- [ ] runWithTenant() 实现并测试通过

### 测试覆盖

- [ ] 无租户场景测试通过
- [ ] 有租户场景测试通过
- [ ] null 租户场景测试通过
- [ ] 作用域清理场景测试通过

### 构建验证

- [ ] `:cartisan-security:compileJava` 通过
- [ ] `:cartisan-security:test` 全部通过
- [ ] `:cartisan-security:check`（ArchUnit）通过
- [ ] `build` 整体构建通过

---

## 与 F03-05 的接口约定

TenantContext 为 F03-05 TenantContextFilter 提供以下 package-private API：

```java
// Filter 调用方式
TenantContext.runWithTenant(tenantId, () -> {
    filterChain.doFilter(request, response);
});
```

Filter 的职责：
1. 从 Header `X-Tenant-Id` 读取租户 ID
2. 若 Header 不存在，从 Sa-Token Session 读取 `tenantId`
3. 调用 `runWithTenant()` 绑定租户上下文
4. 作用域结束自动清理，无需手动 clear()

---

## 参考

- [01_requirement.md](./01_requirement.md) — 需求规格
- [02_interface.md](./02_interface.md) — 接口契约
- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- SKILL.md: [docs/skills/SKILL.md](../../../../skills/SKILL.md)
