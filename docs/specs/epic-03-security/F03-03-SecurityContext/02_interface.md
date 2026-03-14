# Feature: F03-03 SecurityContext — 接口契约

> **版本**: v0.1
> **日期**: 2026-03-14

---

## 接口定义

### 公共接口：SecurityContext

**位置**: `com.cartisan.security.context.SecurityContext`

**类型**: `final class`（工具类）

#### 方法签名

```java
/**
 * 当前用户上下文工具类。
 * <p>
 * 提供当前登录用户信息的只读访问，隐藏 Sa-Token 实现细节。
 * 未登录时：getCurrentUserId/getCurrentUsername 返回 null，hasRole/hasPermission 返回 false。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 推荐用法：先检查是否登录
 * if (SecurityContext.isAuthenticated()) {
 *     Long userId = SecurityContext.getCurrentUserId();
 *     // 使用 userId...
 * }
 *
 * // 或者：对返回值做 null 检查
 * Long userId = SecurityContext.getCurrentUserId();
 * if (userId != null) {
 *     // 使用 userId...
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
public final class SecurityContext {

    /**
     * 防止实例化。
     */
    private SecurityContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取当前登录用户的 ID。
     *
     * @return 用户 ID，未登录时返回 {@code null}
     */
    public static Long getCurrentUserId();

    /**
     * 获取当前登录用户的用户名（即登录 ID）。
     * <p>
     * 返回值取决于业务层登录时传入的 loginId。若 loginId 是 username，则返回 username；
     * 若 loginId 是 userId，则返回 userId 的字符串形式。
     * </p>
     *
     * @return 用户名，未登录时返回 {@code null}
     */
    public static String getCurrentUsername();

    /**
     * 判断当前用户是否拥有指定角色。
     *
     * @param role 角色标识（如 "admin"）
     * @return 拥有角色返回 {@code true}，未登录或无角色返回 {@code false}
     * @throws IllegalArgumentException 如果 role 为 null 或空
     */
    public static boolean hasRole(String role);

    /**
     * 判断当前用户是否拥有指定权限。
     *
     * @param permission 权限标识（如 "user:create"）
     * @return 拥有权限返回 {@code true}，未登录或无权限返回 {@code false}
     * @throws IllegalArgumentException 如果 permission 为 null 或空
     */
    public static boolean hasPermission(String permission);

    /**
     * 判断当前用户是否已登录。
     *
     * @return 已登录返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isAuthenticated();
}
```

#### 方法行为契约

| 方法 | 已登录行为 | 未登录行为 | 参数校验 |
|------|-----------|-----------|---------|
| `getCurrentUserId()` | 返回用户 ID（Long） | 返回 `null` | 无参数 |
| `getCurrentUsername()` | 返回登录 ID（String） | 返回 `null` | 无参数 |
| `hasRole(role)` | 返回是否拥有角色 | 返回 `false` | role 为 null/空白时抛 `IllegalArgumentException` |
| `hasPermission(permission)` | 返回是否拥有权限 | 返回 `false` | permission 为 null/空白时抛 `IllegalArgumentException` |
| `isAuthenticated()` | 返回 `true` | 返回 `false` | 无参数 |

---

## 核心流程（伪代码）

### getCurrentUserId()

```java
public static Long getCurrentUserId() {
    if (!StpUtil.isLogin()) {
        return null;  // 未登录返回 null，避免 NotLoginException
    }
    return StpUtil.getLoginIdAsLong();
}
```

### getCurrentUsername()

```java
public static String getCurrentUsername() {
    if (!StpUtil.isLogin()) {
        return null;
    }
    return StpUtil.getLoginIdAsString();  // 返回登录 ID
}
```

### hasRole(String)

```java
public static boolean hasRole(String role) {
    // 参数校验
    if (role == null || role.isBlank()) {
        throw new IllegalArgumentException("Role cannot be null or blank");
    }
    // 直接代理 Sa-Token
    return StpUtil.hasRole(role);  // 未登录时 Sa-Token 返回 false
}
```

### hasPermission(String)

```java
public static boolean hasPermission(String permission) {
    // 参数校验
    if (permission == null || permission.isBlank()) {
        throw new IllegalArgumentException("Permission cannot be null or blank");
    }
    // 直接代理 Sa-Token
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

## 依赖的外部接口

### Sa-Token: StpUtil

| 方法 | 返回类型 | 未登录行为 |
|------|---------|-----------|
| `isLogin()` | `boolean` | 返回 `false` |
| `getLoginIdAsLong()` | `long` | 抛 `NotLoginException` |
| `getLoginIdAsString()` | `String` | 抛 `NotLoginException` |
| `hasRole(String)` | `boolean` | 返回 `false` |
| `hasPermission(String)` | `boolean` | 返回 `false` |

**注意**：`getLoginIdAsXxx()` 在未登录时会抛异常，因此 `getCurrentUserId()` / `getCurrentUsername()` 需要先检查 `isLogin()`。

---

## 测试策略

### Mock 策略

使用 `MockedStatic<StpUtil>` mock Sa-Token：

```java
@ExtendWith(MockitoExtension.class)
class SecurityContextTest {

    private MockedStatic<StpUtil> mockedStpUtil;

    @BeforeEach
    void setUp() {
        mockedStpUtil = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        mockedStpUtil.close();
    }
}
```

### 测试用例映射

| AC | 测试方法 | Mock 设置 |
|----|---------|----------|
| AC1 | `given_userLoggedIn_when_getCurrentUserId_then_returnUserId` | `StpUtil.isLogin()` → `true`; `getLoginIdAsLong()` → `123L` |
| AC2 | `given_userNotLoggedIn_when_getCurrentUserId_then_returnNull` | `StpUtil.isLogin()` → `false` |
| AC3 | `given_userLoggedIn_when_getCurrentUsername_then_returnUsername` | `StpUtil.isLogin()` → `true`; `getLoginIdAsString()` → `"alice"` |
| AC4 | `given_userNotLoggedIn_when_getCurrentUsername_then_returnNull` | `StpUtil.isLogin()` → `false` |
| AC5 | `given_userHasRole_when_hasRole_then_returnTrue` | `StpUtil.hasRole("admin")` → `true` |
| AC6 | `given_userHasNoRole_when_hasRole_then_returnFalse` | `StpUtil.hasRole("admin")` → `false` |
| AC7 | `given_userHasPermission_when_hasPermission_then_returnTrue` | `StpUtil.hasPermission("user:create")` → `true` |
| AC8 | `given_userHasNoPermission_when_hasPermission_then_returnFalse` | `StpUtil.hasPermission("user:create")` → `false` |
| AC9 | `given_userLoggedIn_when_isAuthenticated_then_returnTrue` | `StpUtil.isLogin()` → `true` |
| AC10 | `given_userNotLoggedIn_when_isAuthenticated_then_returnFalse` | `StpUtil.isLogin()` → `false` |
| AC11 | `given_reflectionInstantiate_when_throwUnsupportedOperationException` | 反射调用构造函数 |

---

## 技术决策

### 决策 1：getCurrentUsername() 返回值含义

**问题**：Sa-Token 的 `getLoginIdAsString()` 返回的是登录 ID，不一定是用户名。

**方案**：与 Sa-Token 保持一致，返回 `getLoginIdAsString()`。业务项目登录时可以传入 username 作为 loginId。

**理由**：
1. Sa-Token 的设计就是用 loginId 作为用户标识
2. 避免额外从 Session 获取 username 的复杂度
3. 业务项目可自行决定 loginId 是 username 还是 userId

### 决策 2：参数校验策略

**问题**：`hasRole()` / `hasPermission()` 是否需要对参数进行校验？

**方案**：参数为 null 或空白时抛 `IllegalArgumentException`。

**理由**：
1. 快速失败（Fail Fast）
2. 与 `Objects.requireNonNull()` 风格一致
3. Sa-Token 内部也会对 null 参数做校验，我们提前校验提供更清晰的错误消息

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/context/SecurityContext.java` | 工具类实现 |
| 新增 | `cartisan-security/src/test/java/com/cartisan/security/context/SecurityContextTest.java` | 单元测试 |

**无其他变更**：
- 无配置文件变更
- 无数据库变更
- 无 AutoConfiguration（F03-07 统一处理）

---

## 参考文档

- 需求规格: [01_requirement.md](./01_requirement.md)
- Sa-Token 文档: https://sa-token.cc/doc.html#/use/id-source
- SKILL.md: [SKILL.md](../../../skills/SKILL.md)
