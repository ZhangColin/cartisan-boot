# Security 自动配置自包含改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除 cartisan-security 模块所有 `@Component`/`@Service` 注解，改由自动配置 `@Bean` 方法声明，确保业务项目无需扫描 `com.cartisan.security.*` 即可完整获得所有安全组件。

**Architecture:** 所有 Bean 通过 `CartisanSecurityAutoConfiguration`（及其 `@Import` 的配置类）显式声明。使用 `@ConditionalOnMissingBean` 允许业务项目覆盖任意组件。`SecurityInterceptorConfig` 和 `CurrentUserArgumentResolverConfig` 各自声明自身管理的 Bean，并用 `ObjectProvider<T>` 注入以避免与同一配置类声明的 `@Bean` 方法产生循环依赖。

**Tech Stack:** Spring Boot 3.4 AutoConfiguration，`FilterRegistrationBean`，`ObjectProvider<T>`，JUnit 5 + AssertJ

---

## 文件变更总览

| 文件 | 操作 | 说明 |
|------|------|------|
| `config/SecurityInterceptor.java` | 修改 | 移除 `@Component` |
| `config/SecurityInterceptorConfig.java` | 修改 | 移除 `@ConditionalOnBean`，改用 `ObjectProvider` + `@Bean @ConditionalOnMissingBean` 声明 `SecurityInterceptor` |
| `context/TenantContextFilter.java` | 修改 | 移除 `@Component` |
| `config/CartisanSecurityAutoConfiguration.java` | 修改 | 新增 `TenantContextFilter`、`SaTokenAuthenticationService`、`SecurityExceptionHandler` 的 `@Bean` 方法 |
| `authentication/SaTokenAuthenticationService.java` | 修改 | 移除 `@Service` |
| `annotation/CurrentUserMethodArgumentResolver.java` | 修改 | 移除 `@Component` |
| `config/CartisanSecurityAutoConfigurationTest.java` | 修改 | 移除 `classes={}` 中的裸类引用，依赖配置类的 `@Bean` 方法 |
| `config/SecurityInterceptorConfigTest.java` | 修改 | 构造器调用改用 `ObjectProvider` mock（包括 setUp 和内联调用） |
| `integration/IntegrationTestApplication.java` | 修改 | 移除 `com.cartisan.security` 扫描包，改为只扫描测试包 |

> **架构决策记录**：`SecurityInterceptorConfig` 同时声明 `SecurityInterceptor` @Bean 并注入它（用 `ObjectProvider` 打破循环依赖）。F03-07 规格的旧约束段落（"只注入已有的 Bean，不声明新 Bean"）与新增的 FR3 矛盾——**FR3（2026-03-19 修订）为准**，旧约束段落已过时。

---

## Task 1: SecurityInterceptor — 移除 @Component，在配置类中声明 @Bean

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptor.java`
- Modify: `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptorConfig.java`
- Test: `cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorConfigTest.java`

- [ ] **Step 1: 移除 `SecurityInterceptor` 上的 `@Component` 和 `import`**

找到：
```java
import org.springframework.stereotype.Component;
// ...
@Component
public class SecurityInterceptor implements HandlerInterceptor {
```
改为：
```java
public class SecurityInterceptor implements HandlerInterceptor {
```
（删除 `import org.springframework.stereotype.Component;`）

- [ ] **Step 2: 重构 `SecurityInterceptorConfig`：移除 `@ConditionalOnBean`，改用 `ObjectProvider` + `@Bean`**

完整替换 `SecurityInterceptorConfig.java` 内容：
```java
package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC 拦截器配置，将 {@link SecurityInterceptor} 注册到拦截器链。
 * <p>
 * 同时负责声明 {@link SecurityInterceptor} Bean，不依赖 @Component 扫描。
 * 使用 {@link ObjectProvider} 注入以避免与自身 @Bean 方法产生循环依赖。
 * </p>
 */
@Configuration
public class SecurityInterceptorConfig implements WebMvcConfigurer {

    private final ObjectProvider<SecurityInterceptor> interceptorProvider;
    private final CartisanSecurityProperties properties;

    public SecurityInterceptorConfig(ObjectProvider<SecurityInterceptor> interceptorProvider,
                                     CartisanSecurityProperties properties) {
        this.interceptorProvider = interceptorProvider;
        this.properties = properties;
    }

    /**
     * 声明 {@link SecurityInterceptor} Bean。
     * <p>
     * 若业务项目已提供自定义实现，则此方法不执行（{@code @ConditionalOnMissingBean}）。
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityInterceptor securityInterceptor() {
        return new SecurityInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> pathPatterns = properties.getPathPatterns();
        List<String> excludePathPatterns = properties.getExcludePathPatterns();

        registry.addInterceptor(interceptorProvider.getObject())
            .addPathPatterns(pathPatterns.toArray(new String[0]))
            .excludePathPatterns(excludePathPatterns.toArray(new String[0]));
    }
}
```

- [ ] **Step 3: 更新 `SecurityInterceptorConfigTest`，改用 ObjectProvider mock**

将 `setUp()` 及 line 36 的内联构造调用，全部改为 mock `ObjectProvider`：
```java
import org.springframework.beans.factory.ObjectProvider;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@BeforeEach
void setUp() {
    mockInterceptor = new SecurityInterceptor();
    properties = new CartisanSecurityProperties();
    ObjectProvider<SecurityInterceptor> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(mockInterceptor);
    config = new SecurityInterceptorConfig(provider, properties);
}

@Test
void given_customPathPatterns_when_getProperties_then_returnsCustom() {
    // 给定
    properties.setPathPatterns(List.of("/api/**", "/admin/**"));
    properties.setExcludePathPatterns(List.of("/api/public/**"));

    // 当 + 那么
    ObjectProvider<SecurityInterceptor> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(mockInterceptor);
    config = new SecurityInterceptorConfig(provider, properties);  // ← 原 line 36，改为 ObjectProvider

    assertThat(config).isNotNull();
}
```

- [ ] **Step 4: 更新 `CartisanSecurityAutoConfigurationTest`，移除裸 `SecurityInterceptor.class`**

`SecurityInterceptor` 现在由 `SecurityInterceptorConfig` 的 `@Bean securityInterceptor()` 方法创建，不需要再单独列入 `classes = {}`。
找到：
```java
@SpringBootTest(classes = {
        SecurityInterceptor.class,
        SecurityInterceptorConfig.class,
        CurrentUserMethodArgumentResolver.class,
        CurrentUserArgumentResolverConfig.class,
        CartisanSecurityAutoConfiguration.class
})
```
改为：
```java
@SpringBootTest(classes = {
        SecurityInterceptorConfig.class,
        CurrentUserMethodArgumentResolver.class,
        CurrentUserArgumentResolverConfig.class,
        CartisanSecurityAutoConfiguration.class
})
```

- [ ] **Step 5: 运行 security 模块测试**

```bash
./gradlew :cartisan-security:test
```
期望：BUILD SUCCESSFUL，所有测试通过

- [ ] **Step 6: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptor.java \
        cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptorConfig.java \
        cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorConfigTest.java \
        cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityAutoConfigurationTest.java
git commit -m "refactor(security): SecurityInterceptor 改为由 SecurityInterceptorConfig @Bean 声明"
```

---

## Task 2: TenantContextFilter — 移除 @Component，改为 FilterRegistrationBean

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/context/TenantContextFilter.java`
- Modify: `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`

- [ ] **Step 1: 移除 `TenantContextFilter` 上的 `@Component` 和 `import`**

找到：
```java
import org.springframework.stereotype.Component;
// ...
@Component("cartisanTenantContextFilter")
public final class TenantContextFilter implements Filter, Ordered {
```
改为：
```java
public final class TenantContextFilter implements Filter, Ordered {
```
（删除 `import org.springframework.stereotype.Component;`）

- [ ] **Step 2: 在 `CartisanSecurityAutoConfiguration` 新增 `TenantContextFilter` 的 `FilterRegistrationBean`**

在 `CartisanSecurityAutoConfiguration` 中新增（`import` 需补充 `TenantContextFilter`、`ConditionalOnMissingBean`）：
```java
import com.cartisan.security.context.TenantContextFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
// ...

/**
 * 注册 {@link TenantContextFilter}。
 * <p>
 * 解析 X-Tenant-Id Header 或 Sa-Token Session 中的租户 ID，绑定到 {@link com.cartisan.security.context.TenantContext}。
 * order = HIGHEST_PRECEDENCE+10，在 saTokenContextFilter（HIGHEST_PRECEDENCE+5）之后执行。
 */
@Bean
@ConditionalOnMissingBean(name = "tenantContextFilter")
public FilterRegistrationBean<TenantContextFilter> tenantContextFilter() {
    FilterRegistrationBean<TenantContextFilter> registration =
        new FilterRegistrationBean<>(new TenantContextFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
}
```

- [ ] **Step 3: 运行 security 模块测试**

```bash
./gradlew :cartisan-security:test
```
期望：BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/context/TenantContextFilter.java \
        cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java
git commit -m "refactor(security): TenantContextFilter 改为由 CartisanSecurityAutoConfiguration FilterRegistrationBean 注册"
```

---

## Task 3: SaTokenAuthenticationService — 移除 @Service，声明 @Bean

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/authentication/SaTokenAuthenticationService.java`
- Modify: `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`

- [ ] **Step 1: 移除 `SaTokenAuthenticationService` 上的 `@Service` 和 `import`**

找到：
```java
import org.springframework.stereotype.Service;
// ...
@Service
public class SaTokenAuthenticationService implements AuthenticationService {
```
改为：
```java
public class SaTokenAuthenticationService implements AuthenticationService {
```
（删除 `import org.springframework.stereotype.Service;`）

- [ ] **Step 2: 在 `CartisanSecurityAutoConfiguration` 新增 `AuthenticationService` Bean**

```java
import com.cartisan.security.authentication.AuthenticationService;
import com.cartisan.security.authentication.SaTokenAuthenticationService;
// ...

/**
 * 注册默认 {@link AuthenticationService} 实现。
 * <p>
 * 若业务项目已提供自定义 {@link AuthenticationService}，则此方法不执行。
 */
@Bean
@ConditionalOnMissingBean(AuthenticationService.class)
public AuthenticationService authenticationService() {
    return new SaTokenAuthenticationService();
}
```

- [ ] **Step 3: 运行 security 模块测试**

```bash
./gradlew :cartisan-security:test
```
期望：BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/authentication/SaTokenAuthenticationService.java \
        cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java
git commit -m "refactor(security): SaTokenAuthenticationService 改为由 CartisanSecurityAutoConfiguration @Bean 声明"
```

---

## Task 4: SecurityExceptionHandler — 声明 @Bean

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`

> `SecurityExceptionHandler` 保留 `@ControllerAdvice` 注解（Spring MVC 通过 Bean 上的注解识别，不依赖扫描）。只需将其改为 `@Bean` 声明而非 `@ControllerAdvice` 扫描触发。

- [ ] **Step 1: 在 `CartisanSecurityAutoConfiguration` 新增 `SecurityExceptionHandler` Bean**

```java
import com.cartisan.security.config.SecurityExceptionHandler;
// ...

/**
 * 注册 Sa-Token 异常处理器。
 * <p>
 * {@link SecurityExceptionHandler} 上的 {@code @ControllerAdvice} 由 Spring MVC
 * 在 Bean 注册后自动识别，无需依赖组件扫描。
 */
@Bean
@ConditionalOnMissingBean(SecurityExceptionHandler.class)
public SecurityExceptionHandler securityExceptionHandler() {
    return new SecurityExceptionHandler();
}
```

- [ ] **Step 2: 运行 security 模块测试**

```bash
./gradlew :cartisan-security:test
```
期望：BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java
git commit -m "refactor(security): SecurityExceptionHandler 改为由 CartisanSecurityAutoConfiguration @Bean 声明"
```

---

## Task 5: CurrentUserMethodArgumentResolver — 移除 @Component

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolver.java`

> `CurrentUserArgumentResolverConfig` 已有 `@Bean @ConditionalOnMissingBean` 声明此 Bean。只需移除类上的 `@Component`。

- [ ] **Step 1: 移除 `CurrentUserMethodArgumentResolver` 上的 `@Component` 和 `import`**

找到：
```java
import org.springframework.stereotype.Component;
// ...
@Component
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {
```
改为：
```java
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {
```
（删除 `import org.springframework.stereotype.Component;`）

- [ ] **Step 2: 更新 `CartisanSecurityAutoConfigurationTest`，移除裸 `CurrentUserMethodArgumentResolver.class`**

`CurrentUserMethodArgumentResolver` 现在由 `CurrentUserArgumentResolverConfig` 的 `@Bean` 方法创建，不需要再单独列入 `classes = {}`（否则会绕过 `@ConditionalOnMissingBean` 守卫）。
找到：
```java
@SpringBootTest(classes = {
        SecurityInterceptorConfig.class,
        CurrentUserMethodArgumentResolver.class,
        CurrentUserArgumentResolverConfig.class,
        CartisanSecurityAutoConfiguration.class
})
```
改为：
```java
@SpringBootTest(classes = {
        SecurityInterceptorConfig.class,
        CurrentUserArgumentResolverConfig.class,
        CartisanSecurityAutoConfiguration.class
})
```

- [ ] **Step 3: 运行 security 模块测试**

```bash
./gradlew :cartisan-security:test
```
期望：BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolver.java \
        cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityAutoConfigurationTest.java
git commit -m "refactor(security): CurrentUserMethodArgumentResolver 移除 @Component，由 CurrentUserArgumentResolverConfig @Bean 声明"
```

---

## Task 6: 更新 IntegrationTestApplication，证明无组件扫描场景下自动配置完整生效

**Files:**
- Modify: `cartisan-security/src/test/java/com/cartisan/security/integration/IntegrationTestApplication.java`

> 这是验证性改动：移除对 `com.cartisan.security` 的显式扫描，模拟真实业务项目的使用方式（只扫描自身包）。自动配置应完全接管所有 Bean 的创建。

- [ ] **Step 0（预检）: 确认 `SaTokenTestConfig` 不依赖扫描才能生效的 Bean**

读取 `SaTokenTestConfig.java`，确认：
1. 无 `@Bean` 方法——它只有一个静态 helper 方法 `initSaTokenContext()`
2. 无 `@Autowired` / 构造器注入任何来自 `com.cartisan.security.*` 的 Bean
3. 确认无需额外改动即可继续执行 Step 1

预期：`SaTokenTestConfig` 只提供静态工具方法，不声明任何 Bean，无依赖问题。

- [ ] **Step 1: 更新 `IntegrationTestApplication` 只扫描测试包**

找到：
```java
@SpringBootApplication(scanBasePackages = {
    "com.cartisan.security",
    "com.cartisan.security.integration"
})
```
改为：
```java
@SpringBootApplication(scanBasePackages = "com.cartisan.security.integration")
```

- [ ] **Step 2: 运行全量集成测试，验证所有测试通过**

```bash
./gradlew :cartisan-security:test
```
期望：BUILD SUCCESSFUL，所有集成测试（`AuthAnnotationIntegrationTest`、`TenantContextIntegrationTest` 等）全部通过

若有测试失败，说明某个 Bean 未被自动配置正确声明，需检查 Task 1-5 的 `@Bean` 方法。

- [ ] **Step 3: Commit**

```bash
git add cartisan-security/src/test/java/com/cartisan/security/integration/IntegrationTestApplication.java
git commit -m "test(security): IntegrationTestApplication 移除 com.cartisan.security 扫描，验证自动配置自包含"
```

---

## Task 7: 全量测试 + 发布

- [ ] **Step 1: 运行全量测试**

```bash
./gradlew test
```
期望：BUILD SUCCESSFUL，所有模块测试通过

- [ ] **Step 2: 发布到本地 Maven 仓库**

```bash
./gradlew publishToMavenLocal
```
期望：BUILD SUCCESSFUL

- [ ] **Step 3: 最终 commit（如有遗漏文件）**

```bash
git status  # 确认工作区干净
```
