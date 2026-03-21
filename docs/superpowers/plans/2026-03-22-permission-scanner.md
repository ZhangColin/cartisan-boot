# Permission Scanner 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 cartisan-security 模块增加权限定义扫描能力，支持业务系统自动采集代码中的权限注解

**Architecture:**
- 改造 `@RequirePermission` 注解为单值设计，新增 `name` 和 `scope` 属性
- 新增 `Permission` record 类和 `PermissionScanner` 接口
- 实现 `DefaultPermissionScanner`，利用 Spring 的 `RequestMappingHandlerMapping` 扫描 Controller 方法
- 注册为 Spring Bean，提供内存缓存

**Tech Stack:** Java 21, Spring Boot 3.4, Spring MVC, Sa-Token

---

## 文件结构

### 新增文件
- `cartisan-security/src/main/java/com/cartisan/security/permission/Permission.java` — 权限数据类（record）
- `cartisan-security/src/main/java/com/cartisan/security/permission/PermissionScanner.java` — 扫描器接口
- `cartisan-security/src/main/java/com/cartisan/security/permission/DefaultPermissionScanner.java` — 默认实现
- `cartisan-security/src/test/java/com/cartisan/security/permission/PermissionTest.java` — Permission 测试
- `cartisan-security/src/test/java/com/cartisan/security/permission/DefaultPermissionScannerTest.java` — 扫描器测试

### 修改文件
- `cartisan-security/src/main/java/com/cartisan/security/annotation/RequirePermission.java` — 改造注解
- `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptor.java` — 适配单值注解
- `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java` — 注册 Scanner Bean
- `cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorTest.java` — 更新测试
- `cartisan-security/src/test/java/com/cartisan/security/integration/AuthAnnotationIntegrationTest.java` — 更新集成测试

---

## Task 1: 创建 Permission 数据类

**Files:**
- Create: `cartisan-security/src/main/java/com/cartisan/security/permission/Permission.java`
- Test: `cartisan-security/src/test/java/com/cartisan/security/permission/PermissionTest.java`

- [ ] **Step 1: 创建 Permission record 类**

创建 `cartisan-security/src/main/java/com/cartisan/security/permission/Permission.java`：

```java
package com.cartisan.security.permission;

import java.util.Objects;

/**
 * 权限定义数据类。
 * <p>
 * 由 {@link com.cartisan.security.annotation.RequirePermission} 注解扫描产生。
 * </p>
 *
 * @param code 权限 code，格式：{context}:{module}:{action}
 * @param name 权限显示名称，未填时同 code
 * @param scope 权限作用域，未填时为 null
 */
public record Permission(
    String code,
    String name,
    String scope
) {
    /**
     * 创建 Permission 实例，处理默认值。
     * <p>
     * 空字符串 name → 使用 code；空字符串 scope → 转为 null
     * </p>
     */
    public static Permission of(String code, String name, String scope) {
        String actualName = name == null || name.isBlank() ? code : name;
        String actualScope = (scope == null || scope.isBlank()) ? null : scope;
        return new Permission(code, actualName, actualScope);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
```

- [ ] **Step 2: 编写 Permission 测试**

创建 `cartisan-security/src/test/java/com/cartisan/security/permission/PermissionTest.java`：

```java
package com.cartisan.security.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Permission 测试")
class PermissionTest {

    @Test
    @DisplayName("给定完整参数 when_of 则创建 Permission")
    void given_fullArgs_when_of_then_createPermission() {
        Permission permission = Permission.of("admin:user:read", "平台管理/用户/查看", "admin");

        assertThat(permission.code()).isEqualTo("admin:user:read");
        assertThat(permission.name()).isEqualTo("平台管理/用户/查看");
        assertThat(permission.scope()).isEqualTo("admin");
    }

    @Test
    @DisplayName("给定空 name when_of 则使用 code 作为 name")
    void given_emptyName_when_of_then_useCodeAsName() {
        Permission permission = Permission.of("admin:user:read", "", "admin");

        assertThat(permission.code()).isEqualTo("admin:user:read");
        assertThat(permission.name()).isEqualTo("admin:user:read");
        assertThat(permission.scope()).isEqualTo("admin");
    }

    @Test
    @DisplayName("给定空 scope when_of 则转为 null")
    void given_emptyScope_when_of_then_convertToNull() {
        Permission permission = Permission.of("admin:user:read", "查看", "");

        assertThat(permission.code()).isEqualTo("admin:user:read");
        assertThat(permission.name()).isEqualTo("查看");
        assertThat(permission.scope()).isNull();
    }

    @Test
    @DisplayName("给定相同 code when_equals 则相等")
    void given_sameCode_when_equals_then_true() {
        Permission p1 = Permission.of("admin:user:read", "name1", "scope1");
        Permission p2 = Permission.of("admin:user:read", "name2", "scope2");

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }
}
```

- [ ] **Step 3: 运行测试验证**

运行：`./gradlew :cartisan-security:test --tests PermissionTest`

预期：PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/permission/Permission.java \
        cartisan-security/src/test/java/com/cartisan/security/permission/PermissionTest.java
git commit -m "feat(security): add Permission record class

Add Permission data class with factory method to handle default values.
Empty name defaults to code, empty scope converts to null.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: 创建 PermissionScanner 接口

**Files:**
- Create: `cartisan-security/src/main/java/com/cartisan/security/permission/PermissionScanner.java`

- [ ] **Step 1: 创建 PermissionScanner 接口**

创建 `cartisan-security/src/main/java/com/cartisan/security/permission/PermissionScanner.java`：

```java
package com.cartisan.security.permission;

import java.util.List;

/**
 * 权限扫描器接口。
 * <p>
 * 扫描代码中 {@link com.cartisan.security.annotation.RequirePermission} 注解，
 * 返回权限定义列表。
 * </p>
 */
public interface PermissionScanner {

    /**
     * 按 scope 过滤扫描。
     *
     * @param scope 作用域，null 表示只扫描未设置 scope 的权限
     * @return 匹配的权限列表
     */
    List<Permission> scanByScope(String scope);

    /**
     * 扫描全部权限。
     *
     * @return 所有权限列表
     */
    List<Permission> scanAll();
}
```

- [ ] **Step 2: 提交**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/permission/PermissionScanner.java
git commit -m "feat(security): add PermissionScanner interface

Define scanByScope(String) and scanAll() methods.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: 创建 DefaultPermissionScanner 实现

**Files:**
- Create: `cartisan-security/src/main/java/com/cartisan/security/permission/DefaultPermissionScanner.java`
- Test: `cartisan-security/src/test/java/com/cartisan/security/permission/DefaultPermissionScannerTest.java`

- [ ] **Step 1: 编写测试用例（TDD）**

创建 `cartisan-security/src/test/java/com/cartisan/security/permission/DefaultPermissionScannerTest.java`：

```java
package com.cartisan.security.permission;

import com.cartisan.security.annotation.RequirePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefaultPermissionScanner 测试")
class DefaultPermissionScannerTest {

    private DefaultPermissionScanner scanner;
    private RequestMappingHandlerMapping handlerMapping;

    @BeforeEach
    void setUp() {
        handlerMapping = new RequestMappingHandlerMapping();
        handlerMapping.registerMapping(
            RequestMappingInfo.paths("/test1").build(),
            "testController",
            MethodUnderTest.class.getDeclaredMethod("methodWithPermission")
        );
        handlerMapping.registerMapping(
            RequestMappingInfo.paths("/test2").build(),
            "testController",
            MethodUnderTest.class.getDeclaredMethod("methodWithNameAndScope")
        );
        handlerMapping.registerMapping(
            RequestMappingInfo.paths("/test3").build(),
            "testController",
            MethodUnderTest.class.getDeclaredMethod("methodWithoutAnnotation")
        );

        scanner = new DefaultPermissionScanner(handlerMapping);
    }

    @Test
    @DisplayName("首次调用 scanAll when 扫描则返回所有权限")
    void given_firstCall_when_scanAll_then_returnAllPermissions() {
        List<Permission> permissions = scanner.scanAll();

        assertThat(permissions).hasSize(2);
        assertThat(permissions).anyMatch(p ->
            p.code().equals("admin:user:read") &&
            p.name().equals("平台管理 / 用户管理 / 查看") &&
            p.scope().equals("admin")
        );
        assertThat(permissions).anyMatch(p ->
            p.code().equals("admin:user:write") &&
            p.name().equals("admin:user:write") &&
            p.scope().isNull()
        );
    }

    @Test
    @DisplayName("给定 scope when_scanByScope 则返回匹配的权限")
    void given_scope_when_scanByScope_then_returnMatchingPermissions() {
        List<Permission> adminPermissions = scanner.scanByScope("admin");

        assertThat(adminPermissions).hasSize(1);
        assertThat(adminPermissions.get(0).code()).isEqualTo("admin:user:read");
        assertThat(adminPermissions.get(0).scope()).isEqualTo("admin");
    }

    @Test
    @DisplayName("给定 null scope when_scanByScope 则返回无 scope 的权限")
    void given_nullScope_when_scanByScope_then_returnPermissionsWithoutScope() {
        List<Permission> permissions = scanner.scanByScope(null);

        assertThat(permissions).hasSize(1);
        assertThat(permissions.get(0).code()).isEqualTo("admin:user:write");
        assertThat(permissions.get(0).scope()).isNull();
    }

    @Test
    @DisplayName("多次调用 scanAll when 使用缓存")
    void given_multipleCalls_when_scanAll_then_useCache() {
        List<Permission> first = scanner.scanAll();
        List<Permission> second = scanner.scanAll();

        assertThat(first).isSameAs(second);
    }

    // ========== 测试用 Controller ==========

    static class MethodUnderTest {
        @RequirePermission(value = "admin:user:read", name = "平台管理 / 用户管理 / 查看", scope = "admin")
        public void methodWithPermission() {}

        @RequirePermission("admin:user:write")
        public void methodWithNameAndScope() {}

        public void methodWithoutAnnotation() {}
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

运行：`./gradlew :cartisan-security:test --tests DefaultPermissionScannerTest`

预期：FAIL（类不存在）

- [ ] **Step 3: 实现 DefaultPermissionScanner**

创建 `cartisan-security/src/main/java/com/cartisan/security/permission/DefaultPermissionScanner.java`：

```java
package com.cartisan.security.permission;

import com.cartisan.security.annotation.RequirePermission;
import org.springframework.web.servlet.mvc.method.RequestMappingHandlerMapping;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认权限扫描器实现。
 * <p>
 * 利用 Spring 的 {@link RequestMappingHandlerMapping} 扫描所有 HandlerMethod，
 * 提取 {@link RequirePermission} 注解并构造 {@link Permission} 对象。
 * </p>
 */
public class DefaultPermissionScanner implements PermissionScanner {

    private final RequestMappingHandlerMapping handlerMapping;
    private volatile List<Permission> allPermissionsCache;
    private final Map<String, List<Permission>> scopedPermissionsCache = new ConcurrentHashMap<>();

    public DefaultPermissionScanner(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public List<Permission> scanByScope(String scope) {
        if (scope == null) {
            return scanAll().stream()
                .filter(p -> p.scope() == null)
                .toList();
        }
        return scanAll().stream()
            .filter(p -> scope.equals(p.scope()))
            .toList();
    }

    @Override
    public List<Permission> scanAll() {
        if (allPermissionsCache != null) {
            return allPermissionsCache;
        }

        synchronized (this) {
            if (allPermissionsCache != null) {
                return allPermissionsCache;
            }

            Set<Permission> permissions = new HashSet<>();
            handlerMapping.getHandlerMethods().forEach((info, handlerMethod) -> {
                RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
                if (annotation != null) {
                    Permission permission = Permission.of(
                        annotation.value(),
                        annotation.name(),
                        annotation.scope()
                    );
                    permissions.add(permission);
                }
            });

            allPermissionsCache = List.copyOf(permissions);
            return allPermissionsCache;
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

运行：`./gradlew :cartisan-security:test --tests DefaultPermissionScannerTest`

预期：PASS

- [ ] **Step 5: 提交**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/permission/DefaultPermissionScanner.java \
        cartisan-security/src/test/java/com/cartisan/security/permission/DefaultPermissionScannerTest.java
git commit -m "feat(security): add DefaultPermissionScanner implementation

Scan HandlerMethods for @RequirePermission annotations.
Use in-memory cache with double-checked locking.
Empty name defaults to code, empty scope converts to null.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: 改造 @RequirePermission 注解

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/annotation/RequirePermission.java`

- [ ] **Step 1: 备份并修改注解**

修改 `cartisan-security/src/main/java/com/cartisan/security/annotation/RequirePermission.java`：

```java
package com.cartisan.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要指定权限才能访问的接口。
 * <p>
 * 仅作用于方法级别。
 * 单值设计，每个注解声明一个权限。
 * <p>
 * 示例：
 * <pre>{@code
 * @RequirePermission(
 *     value = "admin:user:read",
 *     name = "平台管理 / 用户管理 / 查看",
 *     scope = "admin"
 * )
 * @GetMapping("/users")
 * public List<User> list() { ... }
 * }</pre>
 *
 * @see RequireAuth
 * @see RequireRole
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 权限 code，格式：{context}:{module}:{action}
     * <p>示例：admin:user:read</p>
     *
     * @return 权限 code
     */
    String value();

    /**
     * 权限显示名称，用于界面展示。
     * <p>未填时使用 code 作为 name</p>
     *
     * @return 显示名称
     */
    String name() default "";

    /**
     * 权限作用域，用于区分不同系统/范围。
     * <p>未填时（空字符串）扫描时转为 null，表示全局权限</p>
     *
     * @return 作用域
     */
    String scope() default "";
}
```

- [ ] **Step 2: 提交**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/annotation/RequirePermission.java
git commit -m "feat(security): refactor @RequirePermission to single value

- Change value from String[] to String (single value)
- Add name attribute for display
- Add scope attribute for system isolation
- Remove ElementType.TYPE, only support METHOD level

Breaking change: multi-value OR logic no longer supported.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: 更新 SecurityInterceptor 适配单值注解

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptor.java`
- Modify: `cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorTest.java`

- [ ] **Step 1: 更新 SecurityInterceptor**

修改 `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptor.java` 第 56-59 行：

```java
// 旧代码：
RequirePermission requirePermission = findAnnotation(method, beanType, RequirePermission.class);
if (requirePermission != null) {
    StpUtil.checkPermissionOr(requirePermission.value());
}

// 新代码：
RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
if (requirePermission != null) {
    StpUtil.checkPermission(requirePermission.value());
}
```

同时移除不再需要的 `findAnnotation` 方法（第 73-83 行），因为类级别注解已不再支持。

- [ ] **Step 2: 更新 SecurityInterceptorTest**

修改 `cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorTest.java`：

1. 删除类级别注解相关测试：
   - `given_classRequireAuth_when_preHandle_then_callCheckLogin()`
   - `given_classAndMethodRequireAuth_when_methodHasFalse_then_doNotCheckLogin()`
   - `given_classRequireAuthAndMethodRequireRole_when_preHandle_then_callBoth()`
   - `TestClassRequireAuth` 内部类

2. 删除多值权限相关测试：
   - `given_requirePermissionMultiple_when_preHandle_then_callCheckPermissionOrWithAll()`

3. 更新单值权限测试：
   ```java
   @Test
   void given_requirePermission_when_preHandle_then_callCheckPermission() throws Exception {
       // Given: 方法有 @RequirePermission("user:create")
       Method method = TestController.class.getMethod("requirePermissionMethod");
       HandlerMethod handler = new HandlerMethod(new TestController(), method);

       // When
       boolean result = interceptor.preHandle(request, response, handler);

       // Then
       assertThat(result).isTrue();
       mockedStpUtil.verify(() -> StpUtil.checkPermission("user:create"));
   }
   ```

4. 更新 `TestController` 中的 `requirePermissionMethod`：
   ```java
   @RequirePermission("user:create")  // 移除数组语法
   public void requirePermissionMethod() {}
   ```

   移除 `requirePermissionMultipleMethod` 方法。

- [ ] **Step 3: 运行测试验证**

运行：`./gradlew :cartisan-security:test --tests SecurityInterceptorTest`

预期：PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptor.java \
        cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorTest.java
git commit -m "feat(security): adapt SecurityInterceptor to single-value @RequirePermission

- Use method.getAnnotation() directly, no class-level support
- Change checkPermissionOr() to checkPermission()
- Remove findAnnotation() helper method
- Update tests to match new behavior

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: 更新集成测试

**Files:**
- Modify: `cartisan-security/src/test/java/com/cartisan/security/integration/AuthAnnotationIntegrationTest.java`
- Modify: `cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestAuthController.java`

- [ ] **Step 1: 更新 TestAuthController**

修改 `cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestAuthController.java` 第 82-86 行：

```java
// 旧代码：
@GetMapping("/require-permission")
@RequirePermission("user:create")
public ApiResponse<String> requirePermission() {
    return ApiResponse.ok("permission granted");
}

// 保持不变（已经是单值）
```

确认 `@RequirePermission` 使用的是单值语法（已经是，无需修改）。

- [ ] **Step 2: 运行集成测试**

运行：`./gradlew :cartisan-security:integrationTest`

预期：PASS

- [ ] **Step 3: 提交（如有改动）**

```bash
git add cartisan-security/src/test/java/com/cartisan/security/integration/
git commit -m "test(security): verify integration tests pass with single-value @RequirePermission

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: 注册 PermissionScanner Bean

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`

- [ ] **Step 1: 在 AutoConfiguration 中注册 Bean**

在 `CartisanSecurityAutoConfiguration` 中添加 `permissionScanner()` 方法（在 `authenticationService()` 方法之后）：

```java
/**
 * 注册 {@link com.cartisan.security.permission.PermissionScanner} Bean。
 * <p>
 * 业务系统注入此 Bean 以扫描代码中的权限定义。
 * </p>
 */
@Bean
@ConditionalOnMissingBean(PermissionScanner.class)
public PermissionScanner permissionScanner(
    RequestMappingHandlerMapping requestMappingHandlerMapping) {
    return new DefaultPermissionScanner(requestMappingHandlerMapping);
}
```

添加 import：
```java
import com.cartisan.security.permission.DefaultPermissionScanner;
import com.cartisan.security.permission.PermissionScanner;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
```

- [ ] **Step 2: 编写 AutoConfiguration 测试**

修改 `cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityAutoConfigurationTest.java`，添加测试：

```java
@Test
@DisplayName("PermissionScanner Bean should be registered")
void given_autoConfig_when_contextLoads_then_permissionScannerBeanExists() {
    assertThat(context.getBean(PermissionScanner.class)).isNotNull();
}
```

- [ ] **Step 3: 运行测试验证**

运行：`./gradlew :cartisan-security:test --tests CartisanSecurityAutoConfigurationTest`

预期：PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java \
        cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityAutoConfigurationTest.java
git commit -m "feat(security): register PermissionScanner as Spring Bean

Add permissionScanner() method to CartisanSecurityAutoConfiguration.
Auto-register DefaultPermissionScanner with RequestMappingHandlerMapping.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: 添加 PermissionScanner 集成测试

**Files:**
- Create: `cartisan-security/src/test/java/com/cartisan/security/integration/PermissionScannerIntegrationTest.java`
- Modify: `cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestAuthController.java`

- [ ] **Step 1: 在 TestAuthController 添加测试端点**

在 `cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestAuthController.java` 添加：

```java
@RequirePermission(
    value = "test:admin:user:read",
    name = "测试 / 管理员 / 用户查看",
    scope = "test"
)
@GetMapping("/permission-with-metadata")
public ApiResponse<String> permissionWithMetadata() {
    return ApiResponse.ok("permission with metadata");
}

@RequirePermission("test:simple:action")
@GetMapping("/permission-simple")
public ApiResponse<String> permissionSimple() {
    return ApiResponse.ok("simple permission");
}
```

- [ ] **Step 2: 编写集成测试**

创建 `cartisan-security/src/test/java/com/cartisan/security/integration/PermissionScannerIntegrationTest.java`：

```java
package com.cartisan.security.integration;

import com.cartisan.security.permission.Permission;
import com.cartisan.security.permission.PermissionScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PermissionScanner 集成测试")
class PermissionScannerIntegrationTest extends AbstractSecurityIntegrationTest {

    @Autowired
    private PermissionScanner permissionScanner;

    @Test
    @DisplayName("scanAll when 扫描则返回所有权限")
    void given_controllersWithPermissions_when_scanAll_then_returnAll() {
        List<Permission> permissions = permissionScanner.scanAll();

        assertThat(permissions).isNotEmpty();
        assertThat(permissions).anyMatch(p ->
            p.code().equals("test:admin:user:read") &&
            p.name().equals("测试 / 管理员 / 用户查看") &&
            p.scope().equals("test")
        );
        assertThat(permissions).anyMatch(p ->
            p.code().equals("test:simple:action") &&
            p.name().equals("test:simple:action") &&
            p.scope().isNull()
        );
    }

    @Test
    @DisplayName("scanByScope when 给定 scope 则返回匹配权限")
    void given_controllersWithPermissions_when_scanByScope_then_returnMatching() {
        List<Permission> testPermissions = permissionScanner.scanByScope("test");

        assertThat(testPermissions).hasSize(1);
        assertThat(testPermissions.get(0).code()).isEqualTo("test:admin:user:read");
    }

    @Test
    @DisplayName("scanByScope when 给定 null 则返回无 scope 权限")
    void given_controllersWithPermissions_when_scanByScopeNull_then_returnWithoutScope() {
        List<Permission> permissions = permissionScanner.scanByScope(null);

        assertThat(permissions).isNotEmpty();
        assertThat(permissions).anyMatch(p -> p.code().equals("test:simple:action"));
        assertThat(permissions).noneMatch(p -> "test".equals(p.scope()));
    }
}
```

- [ ] **Step 3: 运行集成测试**

运行：`./gradlew :cartisan-security:integrationTest --tests PermissionScannerIntegrationTest`

预期：PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-security/src/test/java/com/cartisan/security/integration/
git commit -m "test(security): add PermissionScanner integration test

Verify scanAll() and scanByScope() work correctly in Spring context.
Add test endpoints in TestAuthController with various metadata.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: 运行全量测试验证

- [ ] **Step 1: 运行 cartisan-security 全量测试**

运行：`./gradlew :cartisan-security:test`

预期：全部 PASS

- [ ] **Step 2: 运行集成测试**

运行：`./gradlew :cartisan-security:integrationTest`

预期：全部 PASS

- [ ] **Step 3: 如有问题则修复并重新提交**

---

## Task 10: 更新文档

**Files:**
- Modify: `docs/guide/cartisan-boot-使用手册.md`

- [ ] **Step 1: 更新使用手册**

在使用手册中添加 `@RequirePermission` 的新用法说明和 `PermissionScanner` 使用示例。

- [ ] **Step 2: 提交**

```bash
git add docs/guide/cartisan-boot-使用手册.md
git commit -m "docs: update user manual for new @RequirePermission and PermissionScanner

Document single-value annotation, name/scope attributes, and scanning feature.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```
