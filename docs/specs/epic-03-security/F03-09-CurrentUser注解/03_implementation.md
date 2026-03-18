# Feature: F03-09 @CurrentUser 注解 — 实施计划

> **Epic**: E03 Security
> **复杂度**: S（Small，30-60 行）
> **预估工时**: 0.5d

---

## 目标复述

创建 `@CurrentUser` 注解 + `CurrentUserMethodArgumentResolver`，支持在 Controller 方法中直接注入当前用户 ID：
- 支持两种参数类型：`Long`（必需登录）和 `Optional<Long>`（可选登录）
- 通过独立配置类 `CurrentUserArgumentResolverConfig` 自动注册到 MVC 容器
- 修改 `CartisanSecurityAutoConfiguration` 添加 `@Import`
- 复用 `SecurityExceptionHandler` 处理 `NotLoginException`

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUser.java` | 注解定义（约 20 行） |
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolver.java` | Resolver 实现（约 60 行） |
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/config/CurrentUserArgumentResolverConfig.java` | 配置类（约 30 行） |
| 修改 | `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java` | 添加 @Import（1 行变更） |
| 新增 | `cartisan-security/src/test/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolverTest.java` | 单元测试（约 200 行） |
| 新增 | `cartisan-security/src/test/java/com/cartisan/security/config/CurrentUserArgumentResolverConfigTest.java` | 配置测试（约 30 行） |
| 修改 | `cartisan-security/src/test/java/com/cartisan/security/integration/` | 新增集成测试（约 150 行） |

---

## 原子任务清单

### Step 1: 创建 @CurrentUser 注解

- **文件**: `cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUser.java`
- **内容**:
  - `@Target(ElementType.PARAMETER)`
  - `@Retention(RetentionPolicy.RUNTIME)`
  - `@Documented`
  - 完整 JavaDoc（含使用示例）
- **验证**: 编译通过 `./gradlew :cartisan-security:compileJava`

### Step 2: 创建 CurrentUserMethodArgumentResolver 类骨架

- **文件**: `cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolver.java`
- **内容**:
  - 实现 `HandlerMethodArgumentResolver` 接口
  - 添加 `@Component` 注解
  - 实现空的 `supportsParameter()` 和 `resolveArgument()` 方法
  - 完整类级别 JavaDoc
- **验证**: 编译通过

### Step 3: 编写 Resolver 单元测试（红灯）

- **文件**: `cartisan-security/src/test/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolverTest.java`
- **内容**:
  - 使用 `@ExtendWith(MockitoExtension.class)`
  - `MockedStatic<SecurityContext>` 的 setup/teardown
  - 7 个测试方法（见下方测试用例清单）
  - 测试命名遵循 `given_{条件}_when_{操作}_then_{预期结果}` 模式
- **验证**: 编译通过 + 测试全红灯

### Step 4: 实现 supportsParameter() 方法

- **文件**: `cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolver.java`
- **内容**:
  - 检查 `@CurrentUser` 注解存在
  - 检查参数类型为 `Long.class` 或 `Optional.class`
  - 返回 `true/false`
- **验证**:
  - `./gradlew :cartisan-security:test --tests CurrentUserMethodArgumentResolverTest.given*` 相关测试通过

### Step 5: 实现 resolveArgument() 方法

- **文件**: `cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolver.java`
- **内容**:
  - 调用 `SecurityContext.getCurrentUserId()`
  - 根据 `Long` 类型：null 时抛 `NotLoginException`
  - 根据 `Optional` 类型：返回 `Optional.ofNullable(userId)`
  - 添加 `@SuppressWarnings("unchecked")` 处理 Optional 泛型警告
- **验证**:
  - `./gradlew :cartisan-security:test` 全绿（7/7 通过）

### Step 6: 创建 CurrentUserArgumentResolverConfig 配置类

- **文件**: `cartisan-security/src/main/java/com/cartisan/security/config/CurrentUserArgumentResolverConfig.java`
- **内容**:
  - `@Configuration` + `@ConditionalOnClass`
  - 实现 `WebMvcConfigurer`
  - 构造器注入 `CurrentUserMethodArgumentResolver`
  - `addArgumentResolvers()` 方法注册 Resolver
  - 完整 JavaDoc
- **验证**: 编译通过

### Step 7: 编写配置类测试

- **文件**: `cartisan-security/src/test/java/com/cartisan/security/config/CurrentUserArgumentResolverConfigTest.java`
- **内容**:
  - 验证配置类可正确加载
  - 验证 Resolver 被正确注册到 MVC 容器
- **验证**:
  - `./gradlew :cartisan-security:test` 全绿

### Step 8: 修改 CartisanSecurityAutoConfiguration

- **文件**: `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`
- **内容**:
  - 修改 `@Import` 注解，添加 `CurrentUserArgumentResolverConfig.class`
- **验证**:
  - `./gradlew :cartesian-security:compileJava` 编译通过
  - 确认自动配置类能正确加载

### Step 9: 编写集成测试（创建测试 Controller）

- **文件**: `cartisan-security/src/test/java/com/cartisan/security/integration/CurrentUserIntegrationTest.java`
- **内容**:
  - 创建测试 Controller 使用 `@CurrentUser`
  - 测试已登录场景返回 200
  - 测试未登录 + `Long` 返回 401
  - 测试未登录 + `Optional<Long>` 返回 200
- **验证**:
  - `./gradlew :cartisan-security:integrationTest` 或相关测试任务全绿

### Step 10: 全量验证

- **验证**:
  - `./gradlew :cartisan-security:test` 全绿
  - `./gradlew :cartisan-security:check` 通过（含 ArchUnit）
  - 代码审查通过

---

## 测试用例清单

### 单元测试（CurrentUserMethodArgumentResolverTest）

| ID | 测试方法 | Mock 设置 | 预期结果 |
|----|---------|----------|---------|
| AC1 | `given_authenticatedUser_when_resolveLong_then_returnUserId` | `getCurrentUserId()` → `123L` | 返回 `123L` |
| AC2 | `given_unauthenticatedUser_when_resolveLong_then_throwNotLoginException` | `getCurrentUserId()` → `null` | 抛 `NotLoginException` |
| AC3 | `given_authenticatedUser_when_resolveOptional_then_returnPresent` | `getCurrentUserId()` → `123L` | 返回 `Optional.of(123L)` |
| AC4 | `given_unauthenticatedUser_when_resolveOptional_then_returnEmpty` | `getCurrentUserId()` → `null` | 返回 `Optional.empty()` |
| AC5 | `given_parameterWithoutAnnotation_when_supportsParameter_then_false` | 无 | 返回 `false` |
| AC6 | `given_unsupportedType_when_supportsParameter_then_false` | 参数类型为 `String` | 返回 `false` |
| AC7 | `given_optionalLongWithoutAnnotation_when_supportsParameter_then_false` | 参数为 `Optional<Long>` 但无注解 | 返回 `false` |

### 配置测试（CurrentUserArgumentResolverConfigTest）

| ID | 测试方法 | 预期结果 |
|----|---------|---------|
| AC8 | `given_contextLoaded_when_resolverRegistered_then_success` | Resolver 在 MVC 容器中可找到 |

### 集成测试（CurrentUserIntegrationTest）

| ID | 测试方法 | 请求 | 预期 |
|----|---------|------|------|
| AC9 | `given_authenticatedUser_when_getProfile_then_return200` | 登录后 GET `/api/test/profile` | 200 + 正确 userId |
| AC10 | `given_unauthenticatedUser_when_getProfile_then_return401` | 未登录 GET `/api/test/profile` | 401 |
| AC11 | `given_unauthenticatedUser_when_getPreferences_then_return200` | 未登录 GET `/api/test/preferences` | 200 + 默认数据 |

---

## 实现要点

### CurrentUser.java

```java
package com.cartisan.security.annotation;

import java.lang.annotation.*;

/**
 * Controller 方法参数注解，用于注入当前登录用户的 ID。
 * <p>
 * 支持两种参数类型，表达不同的登录要求：
 * <ul>
 *   <li>{@code Long} - 必需登录，未登录时抛出 {@code NotLoginException}</li>
 *   <li>{@code Optional<Long>} - 可选登录，未登录时返回 {@code Optional.empty()}</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 必需登录
 * @GetMapping("/profile")
 * public ApiResponse<UserProfile> getProfile(@CurrentUser Long userId) {
 *     return ApiResponse.ok(userService.getProfile(userId));
 * }
 *
 * // 可选登录
 * @GetMapping("/preferences")
 * public ApiResponse<Preferences> getPreferences(@CurrentUser Optional<Long> userId) {
 *     if (userId.isPresent()) {
 *         return ApiResponse.ok(preferencesService.getForUser(userId.get()));
 *     }
 *     return ApiResponse.ok(preferencesService.getDefault());
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
```

### CurrentUserMethodArgumentResolver.java（骨架）

```java
package com.cartisan.security.annotation;

import com.cartisan.security.context.SecurityContext;
import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

/**
 * Spring MVC 参数解析器，处理 {@link CurrentUser} 注解的参数。
 *
 * @since 0.3.0
 */
@Component
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // TODO: 实现
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        // TODO: 实现
        return null;
    }
}
```

### CurrentUserMethodArgumentResolver.java（完整实现）

```java
@Override
public boolean supportsParameter(MethodParameter parameter) {
    // 检查 @CurrentUser 注解
    if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
        return false;
    }

    // 检查参数类型
    Class<?> paramType = parameter.getParameterType();
    return paramType == Long.class || paramType == Optional.class;
}

@Override
@SuppressWarnings("unchecked")
public Object resolveArgument(MethodParameter parameter,
                              ModelAndViewContainer mavContainer,
                              NativeWebRequest webRequest,
                              WebDataBinderFactory binderFactory) {
    // 从 SecurityContext 获取用户 ID
    Long userId = SecurityContext.getCurrentUserId();

    if (parameter.getParameterType() == Long.class) {
        // 必需登录：未登录时抛异常
        if (userId == null) {
            throw new NotLoginException();
        }
        return userId;
    }

    if (parameter.getParameterType() == Optional.class) {
        // 可选登录：未登录时返回 empty
        return Optional.ofNullable(userId);
    }

    // 不应该到达这里
    throw new IllegalStateException("Unsupported parameter type: " + parameter.getParameterType());
}
```

### CurrentUserArgumentResolverConfig.java

```java
package com.cartisan.security.config;

import com.cartisan.security.annotation.CurrentUserMethodArgumentResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * {@link CurrentUserMethodArgumentResolver} 的自动配置类。
 *
 * @since 0.3.0
 */
@Configuration
@ConditionalOnClass(CurrentUserMethodArgumentResolver.class)
public class CurrentUserArgumentResolverConfig implements WebMvcConfigurer {

    private final CurrentUserMethodArgumentResolver resolver;

    public CurrentUserArgumentResolverConfig(CurrentUserMethodArgumentResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }
}
```

### CartisanSecurityAutoConfiguration.java 修改

```java
// 修改前
@Import(SecurityInterceptorConfig.class)

// 修改后
@Import({
    SecurityInterceptorConfig.class,
    CurrentUserArgumentResolverConfig.class
})
```

---

## 验证命令

```bash
# 编译
./gradlew :cartisan-security:compileJava

# 单元测试
./gradlew :cartisan-security:test

# 全量检查
./gradlew :cartisan-security:check

# 仅运行 Resolver 测试
./gradlew :cartisan-security:test --tests CurrentUserMethodArgumentResolverTest

# 仅运行配置测试
./gradlew :cartisan-security:test --tests CurrentUserArgumentResolverConfigTest

# 仅运行集成测试
./gradlew :cartisan-security:test --tests CurrentUserIntegrationTest
```

---

## 参考文档

- 需求规格: [01_requirement.md](./01_requirement.md)
- 接口契约: [02_interface.md](./02_interface.md)
- F03-03 SecurityContext: [03_implementation.md](../F03-03-SecurityContext/03_implementation.md)
- SKILL.md: [SKILL.md](../../../skills/SKILL.md)
