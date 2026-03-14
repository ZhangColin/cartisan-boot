# Feature: F03-04 TenantContext — 测试规格

> **完成日期：** 2026-03-14
> **状态：** 已完成

---

## 测试策略

### 测试分层

| 测试层 | 工具 | 覆盖内容 |
|--------|------|---------|
| 单元测试 | JUnit 5 + AssertJ | TenantContext 所有公共 API |
| 架构测试 | ArchUnit | 代码结构合规性 |

### 测试范围

- **测试类：** `TenantContextTest`
- **测试方法数：** 8 个
- **覆盖率目标：** 100%（行覆盖）

---

## 测试用例清单

### TC1: givenNoTenant_whenGetCurrentTenantId_thenReturnsNull

**AC 对应：** AC1

**步骤：**
1. Given: 无租户上下文
2. When: 调用 `getCurrentTenantId()`
3. Then: 返回 `null`

**实现：**
```java
@Test
void givenNoTenant_whenGetCurrentTenantId_thenReturnsNull() {
    Long tenantId = TenantContext.getCurrentTenantId();
    assertThat(tenantId).isNull();
}
```

---

### TC2: givenTenant_whenGetCurrentTenantId_thenReturnsTenantId

**AC 对应：** AC2

**步骤：**
1. Given: 使用 `runWithTenant(123L, ...)` 绑定租户 ID
2. When: 在作用域内调用 `getCurrentTenantId()`
3. Then: 返回 `123L`

**实现：**
```java
@Test
void givenTenant_whenGetCurrentTenantId_thenReturnsTenantId() {
    Long expectedTenantId = 123L;
    AtomicReference<Long> actualTenantId = new AtomicReference<>();

    TenantContext.runWithTenant(expectedTenantId, () -> {
        actualTenantId.set(TenantContext.getCurrentTenantId());
    });

    assertThat(actualTenantId.get()).isEqualTo(expectedTenantId);
}
```

---

### TC3: givenNullTenant_whenGetCurrentTenantId_thenReturnsNull

**AC 对应：** AC7

**步骤：**
1. Given: 使用 `runWithTenant(null, ...)` 绑定 null
2. When: 在作用域内调用 `getCurrentTenantId()`
3. Then: 返回 `null`

**实现：**
```java
@Test
void givenNullTenant_whenGetCurrentTenantId_thenReturnsNull() {
    AtomicReference<Long> actualTenantId = new AtomicReference<>();

    TenantContext.runWithTenant(null, () -> {
        actualTenantId.set(TenantContext.getCurrentTenantId());
    });

    assertThat(actualTenantId.get()).isNull();
}
```

---

### TC4: givenTenant_whenScopeEnds_thenTenantIsCleared

**AC 对应：** AC6

**步骤：**
1. Given: 使用 `runWithTenant(111L, ...)` 绑定租户 ID
2. When: 作用域结束后调用 `getCurrentTenantId()`
3. Then: 返回 `null`（作用域内返回 `111L`，作用域外返回 `null`）

**实现：**
```java
@Test
void givenTenant_whenScopeEnds_thenTenantIsCleared() {
    AtomicReference<Long> tenantIdInScope = new AtomicReference<>();
    TenantContext.runWithTenant(111L, () -> {
        tenantIdInScope.set(TenantContext.getCurrentTenantId());
    });

    assertThat(tenantIdInScope.get()).isEqualTo(111L);
    assertThat(TenantContext.getCurrentTenantId()).isNull();
}
```

---

### TC5: givenNoTenant_whenHasTenant_thenReturnsFalse

**AC 对应：** AC3

**步骤：**
1. Given: 无租户上下文
2. When: 调用 `hasTenant()`
3. Then: 返回 `false`

**实现：**
```java
@Test
void givenNoTenant_whenHasTenant_thenReturnsFalse() {
    boolean hasTenant = TenantContext.hasTenant();
    assertThat(hasTenant).isFalse();
}
```

---

### TC6: givenTenant_whenHasTenant_thenReturnsTrue

**AC 对应：** AC3

**步骤：**
1. Given: 使用 `runWithTenant(456L, ...)` 绑定租户 ID
2. When: 在作用域内调用 `hasTenant()`
3. Then: 返回 `true`

**实现：**
```java
@Test
void givenTenant_whenHasTenant_thenReturnsTrue() {
    AtomicReference<Boolean> result = new AtomicReference<>();

    TenantContext.runWithTenant(456L, () -> {
        result.set(TenantContext.hasTenant());
    });

    assertThat(result.get()).isTrue();
}
```

---

### TC7: givenNoTenant_whenRequireTenant_thenThrowsIllegalStateException

**AC 对应：** AC4

**步骤：**
1. Given: 无租户上下文
2. When: 调用 `requireTenant()`
3. Then: 抛出 `IllegalStateException`

**实现：**
```java
@Test
void givenNoTenant_whenRequireTenant_thenThrowsIllegalStateException() {
    assertThatThrownBy(TenantContext::requireTenant)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No tenant context");
}
```

---

### TC8: givenTenant_whenRequireTenant_thenReturnsTenantId

**AC 对应：** AC5

**步骤：**
1. Given: 使用 `runWithTenant(789L, ...)` 绑定租户 ID
2. When: 在作用域内调用 `requireTenant()`
3. Then: 返回 `789L`

**实现：**
```java
@Test
void givenTenant_whenRequireTenant_thenReturnsTenantId() {
    Long expectedTenantId = 789L;
    AtomicReference<Long> actualTenantId = new AtomicReference<>();

    TenantContext.runWithTenant(expectedTenantId, () -> {
        actualTenantId.set(TenantContext.requireTenant());
    });

    assertThat(actualTenantId.get()).isEqualTo(expectedTenantId);
}
```

---

## AC 覆盖矩阵

| AC | 测试用例 | 状态 |
|----|---------|------|
| AC1: 无租户返回 null | TC1 | ✅ |
| AC2: 有租户返回租户 ID | TC2 | ✅ |
| AC3: hasTenant 正确反映状态 | TC5, TC6 | ✅ |
| AC4: requireTenant 无租户抛异常 | TC7 | ✅ |
| AC5: requireTenant 有租户返回 ID | TC8 | ✅ |
| AC6: 作用域结束自动清理 | TC4 | ✅ |
| AC7: null 租户支持 | TC3 | ✅ |

---

## 测试执行结果

### 2026-03-14 执行记录

```bash
./gradlew :cartisan-security:test --tests TenantContextTest
```

**结果：** BUILD SUCCESSFUL
**测试通过：** 8/8

---

## 技术说明

### AtomicReference 使用

测试使用 `AtomicReference<Long>` 从 `runWithTenant()` 作用域内捕获值，这是测试 ScopedValue 的标准模式。

**原因：** ScopedValue 的 `where(...).run(...)` 方法要求在 lambda 内执行代码，lambda 内的局部变量无法直接传递到外部。使用 `AtomicReference` 作为可变容器可以在 lambda 内设置值，在外部读取。

### 与 Epic 设计的差异

Epic Backlog 原计划使用 `ScopedValue.getOrDefault(TENANT_ID, null)`，但实际 Java 21 的 ScopedValue API 不存在 `getOrDefault` 方法。

**实际实现：** 使用 `if (!TENANT_ID.isBound()) return null; return TENANT_ID.get();`

**评估：** 实际实现更优，语义更清晰。

---

## 后续测试需求

### F03-05 TenantContextFilter 集成测试

- 测试 Filter 从 Header 解析租户 ID
- 测试 Filter 从 Session 解析租户 ID
- 测试 Header 优先于 Session
- 测试作用域结束后租户清理

### F03-08 端到端集成测试

- 测试与 SecurityContext 协同工作
- 测试与权限注解协同工作
- 测试与 MVC 拦截器协同工作
