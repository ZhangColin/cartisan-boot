# Feature: F03-03 SecurityContext — 实施计划

> **Epic**: E03 Security
> **复杂度**: S（Small，30-60 行）
> **预估工时**: 0.5d

---

## 目标复述

创建 `SecurityContext` 工具类，作为 Sa-Token `StpUtil` 的只读薄抽象层：
- 提供 5 个静态方法：`getCurrentUserId()`, `getCurrentUsername()`, `hasRole()`, `hasPermission()`, `isAuthenticated()`
- 未登录时：getter 方法返回 `null`，检查方法返回 `false`
- 参数校验：`hasRole/hasPermission` 的 null/空白参数抛 `IllegalArgumentException`
- 与 `SecurityInterceptor` 测试风格一致（使用 `MockedStatic` mock StpUtil）

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/context/SecurityContext.java` | 工具类实现（约 80 行） |
| 新增 | `cartisan-security/src/test/java/com/cartisan/security/context/SecurityContextTest.java` | 单元测试（约 150 行） |

**无其他变更**：无配置文件、无数据库、无 AutoConfiguration。

---

## 原子任务清单

### Step 1: 创建 SecurityContext 工具类 ✅

- **文件**: `cartisan-security/src/main/java/com/cartisan/security/context/SecurityContext.java`
- **内容**:
  - `final class` 声明
  - 私有构造函数抛出 `UnsupportedOperationException`
  - 5 个静态方法（`getCurrentUserId`, `getCurrentUsername`, `hasRole`, `hasPermission`, `isAuthenticated`）
  - 完整 JavaDoc（含类级别和每个方法的文档）
- **验证**: ✅ 编译通过 `./gradlew :cartisan-security:compileJava`

### Step 2: 编写单元测试（红灯） ✅

- **文件**: `cartisan-security/src/test/java/com/cartisan/security/context/SecurityContextTest.java`
- **内容**:
  - 使用 `@ExtendWith(MockitoExtension.class)`
  - `MockedStatic<StpUtil>` 的 setup/teardown
  - 15 个测试方法（见下方测试用例清单）
  - 测试命名遵循 `given_{条件}_when_{操作}_then_{预期结果}` 模式
- **验证**: ✅ 编译通过 + 测试全红灯（实现类尚不存在）

### Step 3: 实现 SecurityContext 方法（绿灯） ✅

- **文件**: `cartisan-security/src/main/java/com/cartisan/security/context/SecurityContext.java`
- **内容**: 实现 5 个方法，使测试全部通过
- **验证**:
  - ✅ `./gradlew :cartisan-security:test` 全绿（15/15 通过）
  - ✅ `./gradlew :cartisan-security:check` 通过

---

## 测试用例清单

### 主流程测试（10 条，对应 AC1-AC10）

| ID | 测试方法 | Mock 设置 | 预期结果 |
|----|---------|----------|---------|
| AC1 | `given_userLoggedIn_when_getCurrentUserId_then_returnUserId` | `isLogin()` → `true`; `getLoginIdAsLong()` → `123L` | 返回 `123L` |
| AC2 | `given_userNotLoggedIn_when_getCurrentUserId_then_returnNull` | `isLogin()` → `false` | 返回 `null` |
| AC3 | `given_userLoggedIn_when_getCurrentUsername_then_returnUsername` | `isLogin()` → `true`; `getLoginIdAsString()` → `"alice"` | 返回 `"alice"` |
| AC4 | `given_userNotLoggedIn_when_getCurrentUsername_then_returnNull` | `isLogin()` → `false` | 返回 `null` |
| AC5 | `given_userHasRole_when_hasRole_then_returnTrue` | `hasRole("admin")` → `true` | 返回 `true` |
| AC6 | `given_userHasNoRole_when_hasRole_then_returnFalse` | `hasRole("admin")` → `false` | 返回 `false` |
| AC7 | `given_userHasPermission_when_hasPermission_then_returnTrue` | `hasPermission("user:create")` → `true` | 返回 `true` |
| AC8 | `given_userHasNoPermission_when_hasPermission_then_returnFalse` | `hasPermission("user:create")` → `false` | 返回 `false` |
| AC9 | `given_userLoggedIn_when_isAuthenticated_then_returnTrue` | `isLogin()` → `true` | 返回 `true` |
| AC10 | `given_userNotLoggedIn_when_isAuthenticated_then_returnFalse` | `isLogin()` → `false` | 返回 `false` |

### 边界场景测试（5 条）

| ID | 测试方法 | 预期结果 |
|----|---------|---------|
| AC11 | `given_reflectionInstantiate_when_throwUnsupportedOperationException` | 抛 `UnsupportedOperationException` |
| 边界1 | `given_roleIsNull_when_hasRole_then_throwIllegalArgumentException` | 抛 `IllegalArgumentException` |
| 边界2 | `given_roleIsBlank_when_hasRole_then_throwIllegalArgumentException` | 抛 `IllegalArgumentException` |
| 边界3 | `given_permissionIsNull_when_hasPermission_then_throwIllegalArgumentException` | 抛 `IllegalArgumentException` |
| 边界4 | `given_permissionIsBlank_when_hasPermission_then_throwIllegalArgumentException` | 抛 `IllegalArgumentException` |

---

## 实现要点

### getCurrentUserId()

```java
public static Long getCurrentUserId() {
    if (!StpUtil.isLogin()) {
        return null;  // 未登录返回 null，避免 NotLoginException
    }
    return StpUtil.getLoginIdAsLong();  // 自动装箱为 Long
}
```

### getCurrentUsername()

```java
public static String getCurrentUsername() {
    if (!StpUtil.isLogin()) {
        return null;
    }
    return StpUtil.getLoginIdAsString();
}
```

### hasRole(String)

```java
public static boolean hasRole(String role) {
    if (role == null || role.isBlank()) {
        throw new IllegalArgumentException("Role cannot be null or blank");
    }
    return StpUtil.hasRole(role);
}
```

### hasPermission(String)

```java
public static boolean hasPermission(String permission) {
    if (permission == null || permission.isBlank()) {
        throw new IllegalArgumentException("Permission cannot be null or blank");
    }
    return StpUtil.hasPermission(permission);
}
```

### isAuthenticated()

```java
public static boolean isAuthenticated() {
    return StpUtil.isLogin();
}
```

---

## 验证命令

```bash
# 编译
./gradlew :cartisan-security:compileJava

# 测试
./gradlew :cartisan-security:test

# 全量检查
./gradlew :cartisan-security:check
```

---

## 参考文档

- 需求规格: [01_requirement.md](./01_requirement.md)
- 接口契约: [02_interface.md](./02_interface.md)
- SKILL.md: [SKILL.md](../../../skills/SKILL.md)
