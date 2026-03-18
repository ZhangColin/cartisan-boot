# Feature: F03-09 @CurrentUser 注解 — 接口契约

> **版本**: v0.1
> **日期**: 2026-03-18

---

## 接口定义

### 1. 注解：@CurrentUser

**位置**: `com.cartisan.security.annotation.CurrentUser`

**类型**: 注解（Annotation）

```java
/**
 * Controller 方法参数注解，用于注入当前登录用户的 ID。
 * <p>
 * 支持两种参数类型，表达不同的登录要求：
 * <ul>
 *   <li>{@code Long} - 必需登录，未登录时抛出 {@link cn.dev33.satoken.exception.NotLoginException}</li>
 *   <li>{@code Optional<Long>} - 可选登录，未登录时返回 {@link Optional#empty()}</li>
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
 * @see CurrentUserMethodArgumentResolver
 * @since 0.3.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
```

#### 注解属性契约

| 属性 | 说明 |
|------|------|
| 无 | 标记注解，通过参数类型区分行为 |

---

### 2. 参数解析器：CurrentUserMethodArgumentResolver

**位置**: `com.cartisan.security.annotation.CurrentUserMethodArgumentResolver`

**类型**: 类（实现 `HandlerMethodArgumentResolver`）

```java
/**
 * Spring MVC 参数解析器，处理 {@link CurrentUser} 注解的参数。
 * <p>
 * 从 {@link SecurityContext} 获取当前用户 ID，根据参数类型决定行为：
 * <ul>
 *   <li>{@code Long} - 未登录时抛 {@code NotLoginException}</li>
 *   <li>{@code Optional<Long>} - 未登录时返回 {@code Optional.empty()}</li>
 * </ul>
 *
 * @since 0.3.0
 */
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 判断参数是否支持解析。
     * <p>
     * 条件：
     * <ol>
     *   <li>参数有 {@code @CurrentUser} 注解</li>
     *   <li>参数类型为 {@code Long} 或 {@code Optional<Long>}</li>
     * </ol>
     *
     * @param parameter 方法参数
     * @return 支持返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter);

    /**
     * 解析参数值。
     * <p>
     * 从 {@code SecurityContext.getCurrentUserId()} 获取用户 ID。
     *
     * @param parameter 方法参数
     * @param mavContainer MVC 容器
     * @param webRequest 请求
     * @param binderFactory 数据绑定工厂
     * @return 参数值（Long 或 Optional<Long>）
     * @throws NotLoginException 参数类型为 Long 且未登录时
     * @throws IllegalStateException 参数类型不支持时
     */
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory);
}
```

#### 方法行为契约

| 方法 | 场景 | 返回值 / 异常 |
|------|------|-------------|
| `supportsParameter()` | 参数有 `@CurrentUser` 且类型为 `Long` | `true` |
| `supportsParameter()` | 参数有 `@CurrentUser` 且类型为 `Optional<Long>` | `true` |
| `supportsParameter()` | 参数无 `@CurrentUser` | `false` |
| `supportsParameter()` | 参数类型不是 `Long`/`Optional<Long>` | `false` |
| `resolveArgument()` | 类型为 `Long` 且已登录 | 返回 userId |
| `resolveArgument()` | 类型为 `Long` 且未登录 | 抛 `NotLoginException` |
| `resolveArgument()` | 类型为 `Optional<Long>` 且已登录 | 返回 `Optional.of(userId)` |
| `resolveArgument()` | 类型为 `Optional<Long>` 且未登录 | 返回 `Optional.empty()` |

---

### 3. 配置类：CurrentUserArgumentResolverConfig

**位置**: `com.cartisan.security.config.CurrentUserArgumentResolverConfig`

**类型**: 配置类（实现 `WebMvcConfigurer`）

```java
/**
 * {@link CurrentUserMethodArgumentResolver} 的自动配置类。
 * <p>
 * 将 Resolver 注册到 Spring MVC 参数解析器链中。
 *
 * @since 0.3.0
 */
@Configuration
@ConditionalOnClass(CurrentUserMethodArgumentResolver.class)
public class CurrentUserArgumentResolverConfig implements WebMvcConfigurer {

    private final CurrentUserMethodArgumentResolver resolver;

    /**
     * 构造器注入 Resolver Bean。
     *
     * @param resolver Resolver 实例（由 @Component 扫描创建）
     */
    public CurrentUserArgumentResolverConfig(CurrentUserMethodArgumentResolver resolver);

    /**
     * 注册 Resolver 到 MVC 容器。
     *
     * @param resolvers 参数解析器列表
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers);
}
```

#### 类行为契约

| 行为 | 说明 |
|------|------|
| 条件装配 | 仅当 `CurrentUserMethodArgumentResolver` 在 classpath 时生效 |
| Bean 注入 | 通过构造器注入已有的 Resolver Bean（由 `@Component` 扫描） |
| 注册顺序 | 添加到解析器链末尾（Spring 默认顺序） |

---

## 核心流程（伪代码）

### supportsParameter()

```java
@Override
public boolean supportsParameter(MethodParameter parameter) {
    // 1. 检查是否有 @CurrentUser 注解
    if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
        return false;
    }

    // 2. 检查参数类型
    Class<?> paramType = parameter.getParameterType();
    return paramType == Long.class || paramType == Optional.class;
}
```

### resolveArgument()

```java
@Override
public Object resolveArgument(MethodParameter parameter, ...) {
    // 1. 从 SecurityContext 获取用户 ID
    Long userId = SecurityContext.getCurrentUserId();

    // 2. 根据参数类型决定行为
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

    // 3. 不应该到达这里（supportsParameter 已过滤）
    throw new IllegalStateException("Unsupported parameter type: " + parameter.getParameterType());
}
```

### addArgumentResolvers()

```java
@Override
public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(resolver);
}
```

---

## 依赖的外部接口

### Spring MVC: HandlerMethodArgumentResolver

| 方法 | 说明 |
|------|------|
| `supportsParameter(MethodParameter)` | 判断是否支持解析该参数 |
| `resolveArgument(...)` | 解析参数值 |

### Spring MVC: WebMvcConfigurer

| 方法 | 说明 |
|------|------|
| `addArgumentResolvers(List)` | 添加自定义参数解析器 |

### cartisan-security: SecurityContext

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `getCurrentUserId()` | `Long` | 已登录返回用户 ID，未登录返回 `null` |

### Sa-Token: NotLoginException

| 异常 | HTTP 状态 | 说明 |
|------|----------|------|
| `NotLoginException` | 401 | 用户未登录异常，由 `SecurityExceptionHandler` 统一处理 |

---

## 与现有组件的集成

### 1. CartisanSecurityAutoConfiguration 修改

**变更内容**：添加 `@Import(CurrentUserArgumentResolverConfig.class)`

```java
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(StpUtil.class)
@EnableConfigurationProperties(CartisanSecurityProperties.class)
@Import({
    SecurityInterceptorConfig.class,
    CurrentUserArgumentResolverConfig.class  // 新增
})
public class CartisanSecurityAutoConfiguration {
}
```

### 2. CurrentUserMethodArgumentResolver Bean 定义

**方式**：添加 `@Component` 注解，由组件扫描自动发现

```java
@Component
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {
    // ...
}
```

### 3. 执行时序图

```
┌─────────────────────────────────────────────────────────────┐
│                       请求到达                               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  Filter 链                                                  │
│  → TenantContextFilter（解析租户）                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  Interceptor 链                                             │
│  → SecurityInterceptor（@RequireAuth 检查）                  │
│     - 有 @RequireAuth → 检查登录 → 未登录抛 NotLoginException │
│     - 无 @RequireAuth → 放行                                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  参数解析                                                    │
│  → CurrentUserMethodArgumentResolver                        │
│     - @CurrentUser Long userId → 未登录抛 NotLoginException  │
│     - @CurrentUser Optional<Long> → 未登录返回 Optional.empty│
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  Controller 方法执行                                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  SecurityExceptionHandler                                   │
│  - 捕获 NotLoginException → 转为 401 响应                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 测试策略

### 单元测试：CurrentUserMethodArgumentResolverTest

**Mock 策略**：使用 `MockedStatic<SecurityContext>` mock `SecurityContext`

```java
@ExtendWith(MockitoExtension.class)
class CurrentUserMethodArgumentResolverTest {

    private CurrentUserMethodArgumentResolver resolver;
    private MockedStatic<SecurityContext> mockedSecurityContext;

    @BeforeEach
    void setUp() {
        resolver = new CurrentUserMethodArgumentResolver();
        mockedSecurityContext = mockStatic(SecurityContext.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContext.close();
    }
}
```

### 测试用例映射

| AC | 测试方法 | Mock 设置 |
|----|---------|----------|
| AC1 | `given_authenticatedUser_when_resolveLong_then_returnUserId` | `getCurrentUserId()` → `123L` |
| AC2 | `given_unauthenticatedUser_when_resolveLong_then_throwNotLoginException` | `getCurrentUserId()` → `null` |
| AC3 | `given_authenticatedUser_when_resolveOptional_then_returnPresent` | `getCurrentUserId()` → `123L` |
| AC4 | `given_unauthenticatedUser_when_resolveOptional_then_returnEmpty` | `getCurrentUserId()` → `null` |
| AC5 | `given_parameterWithoutAnnotation_when_supportsParameter_then_false` | 无 |
| AC6 | `given_unsupportedType_when_supportsParameter_then_false` | 无 |
| AC7 | `given_contextLoaded_when_resolverRegistered_then_success` | Spring 上下文测试 |

### 集成测试：CurrentUserIntegrationTest

**测试方式**：使用 `@SpringBootTest` + MockMvc

```java
@SpringBootTest
@AutoConfigureMockMvc
class CurrentUserIntegrationTest extends AbstractSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void given_authenticatedUser_when_getProfile_then_return200() throws Exception {
        // 1. 登录获取 token
        String token = login("test-user");

        // 2. 使用 token 访问接口
        mockMvc.perform(get("/api/users/profile")
                .header("satoken", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1));
    }
}
```

---

## 技术决策

### 决策 1：为什么使用独立的配置类而非扩展现有配置类

**问题**：`CurrentUserArgumentResolverConfig` 应该独立还是扩展现有的 `SecurityInterceptorConfig`？

**方案**：创建独立的配置类 `CurrentUserArgumentResolverConfig`。

**理由**：
1. 单一职责：拦截器配置与参数解析器配置分离
2. 条件装配更灵活：可独立控制是否启用
3. 易于维护：未来添加更多 Resolver 时不会让 `SecurityInterceptorConfig` 臃肿

### 决策 2：为什么使用 @Component 而非 @Bean 手动注册

**问题**：Resolver Bean 应该通过 `@Component` 扫描还是 `@Bean` 手动注册？

**方案**：使用 `@Component` 注解，由组件扫描自动发现。

**理由**：
1. 与 `SecurityInterceptor` 的注册方式一致（参见 ADR-058：注入已有 Bean）
2. 配置类通过构造器注入已有的 Bean，符合依赖注入原则
3. 便于测试：可直接 `new` 创建实例进行单元测试

### 决策 3：为什么不支持 @CurrentUser String username

**问题**：是否支持注入用户名？

**方案**：不支持，仅支持 `Long` userId。

**理由**：
1. YAGNI 原则：当前需求只有 userId
2. 语义清晰：userId 是主键，username 可能重复或变更
3. 业务层需要 username 时可通过 userId 查询
4. 保持薄抽象，避免功能蔓延

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUser.java` | 注解定义 |
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolver.java` | Resolver 实现 |
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/config/CurrentUserArgumentResolverConfig.java` | 配置类 |
| 修改 | `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java` | 添加 @Import |
| 新增 | `cartisan-security/src/test/java/com/cartisan/security/annotation/CurrentUserMethodArgumentResolverTest.java` | 单元测试 |
| 新增 | `cartisan-security/src/test/java/com/cartisan/security/config/CurrentUserArgumentResolverConfigTest.java` | 配置测试 |
| 修改 | `cartisan-security/src/test/java/com/cartisan/security/integration/` | 新增集成测试 |

**无其他变更**：
- 无配置文件变更（application.yml）
- 无数据库变更
- 无 Sa-Token 配置变更

---

## 参考文档

- 需求规格: [01_requirement.md](./01_requirement.md)
- F03-03 SecurityContext: [02_interface.md](../F03-03-SecurityContext/02_interface.md)
- F03-07 AutoConfiguration: [02_interface.md](../F03-07-AutoConfiguration/02_interface.md)
- Spring MVC 文档: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller-ann-arguments.html
- SKILL.md: [SKILL.md](../../../skills/SKILL.md)
