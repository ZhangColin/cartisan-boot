# 团队规则库 (SKILL)

> 定位：团队的"经验手册"——记录所有值得沉淀的踩坑经验和铁律。AI 在开发前会读取它，避免重复犯错。

---

## DDD / 领域建模

### 规则 DDD-001：Java 泛型类型擦除导致的编译问题

**问题**：在接口默认方法中，泛型类型参数 `T` 在运行时被擦除为 `Object`，无法直接调用具体方法。

**错误代码**：
```java
public interface Entity<T, ID> {
    ID getId();

    default boolean sameIdentityAs(T other) {
        // ❌ 编译错误：other.getId() 找不到符号
        return Objects.equals(this.getId(), other.getId());
    }
}
```

**正确做法**：
```java
@SuppressWarnings("unchecked")
default boolean sameIdentityAs(T other) {
    if (other == null) return false;
    // 需要运行时类型检查 + 强制类型转换
    if (this.getClass() != other.getClass()) return false;
    ID otherId = ((Entity<T, ID>) other).getId();
    return Objects.equals(this.getId(), otherId);
}
```

**记忆口诀**：接口泛型方法里调泛型参数的方法？必须先检查类型再强转。

---

### 规则 DDD-002：ValueObject 的 sameValueAs 可直接委托 equals

**原因**：`equals()` 方法接受 `Object` 类型，不需要处理类型擦除问题。

**推荐做法**：
```java
public interface ValueObject<T> {
    default boolean sameValueAs(T other) {
        if (other == null) return false;
        return this.equals(other);  // ✅ 简洁
    }
}
```

**对比 Entity**：Entity 需要比较 ID，而 ID 是泛型方法获取的，所以需要额外处理。

---

### 规则 DDD-003：领域事件基类应自动生成元数据

**推荐做法**：
- `eventId`：自动生成 UUID
- `occurredAt`：自动设置为 `Instant.now()`
- `aggregateId`：由子类提供并验证非空

**代码**：
```java
public abstract class DomainEvent {
    private final String eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId");
    }
}
```

---

## 工具配置

### 规则 TOOL-001：ArchUnit 检查生产代码时应排除测试依赖

**问题**：ArchUnit 检查 `domain` 包依赖时，会把"测试类引用 domain 类"的关系也算作依赖，导致误报。

**解决方案**：只检查编译后的生产代码，而非整个 classpath。

**代码**：
```java
// ❌ 错误：会包含测试依赖
JavaClasses classes = new ClassFileImporter().importPackages("com.cartisan.core");

// ✅ 正确：只检查生产代码
JavaClasses productionClasses = new ClassFileImporter()
    .importPaths("build/classes/java/main");
```

---

### 规则 TOOL-002：PIT 插件与 Gradle 9 需使用 1.19.x RC 版

**现状**：
- 项目使用 Gradle 9.0；PIT 插件 `info.solidsoft.pitest:1.15.0` 因 `reporting.baseDir` 被移除而报错。
- **1.19.0-rc.1+** 已修复（改用 `baseDirectory`），支持 Gradle 9。

**正确配置**（Kotlin DSL）：
```kotlin
// cartisan-core/build.gradle.kts
plugins {
    id("info.solidsoft.pitest") version "1.19.0-rc.3"
}
pitest {
    targetClasses.set(listOf("com.cartisan.core.domain.*"))
    targetTests.set(listOf("com.cartisan.core.domain.*", "com.cartisan.core.arch.*"))
    mutationThreshold.set(70)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
}
```

**注意**：1.19 目前为 RC，正式版发布后可改为稳定版本号。

---

### 规则 TOOL-003：ArchUnit 规则类不适用于 PIT 变异测试

**问题**：尝试对 ArchUnit 规则类运行 PIT 时报错 "No mutations found"。

**原因**：ArchUnit 规则类是声明式配置，没有业务逻辑可供变异：
```java
public class CartisanProhibitionRules {
    @ArchTest
    static final ArchRule noFieldInjection =
        noFields()
            .should()
            .beAnnotatedWith(Autowired.class)
            .because("Use constructor injection instead of field injection");
}
```
- 规则是 `static final` 字段，初始化后不可变
- `ArchRule` 对象由 ArchUnit 库的 fluent API 构建
- 实际检查逻辑在 ArchUnit 库中，不在我们的代码里

**替代方案**：
- 使用 **Fixtures 双重验证法**：每条规则都有 pass/fail 成对测试
- 合规验证：确保规则不误报
- 违规验证：确保规则能捕获问题

**记忆口诀**：声明式配置没法变异，用 fixtures 测代替。

---

### 规则 TOOL-004：@TestConfiguration 不能使用工具类模式

**问题**：`@TestConfiguration` 类如果使用私有构造函数抛出异常，Spring 会尝试实例化它而失败。

**错误代码**：
```java
// ❌ @TestConfiguration + 工具类模式 = Spring 无法实例化
@TestConfiguration(proxyBeanMethods = false)
public final class PostgresTestContainer {
    private PostgresTestContainer() {
        throw new UnsupportedOperationException("Utility class");
    }

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
```

**正确做法**：
```java
// ✅ @TestConfiguration 应该允许 Spring 实例化
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainer {
    // 无构造函数或使用默认构造函数

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
```

**错误表现**：
```
BeanInstantiationException: Error creating bean with name 'postgresTestContainer'
Caused by: UnsupportedOperationException: Utility class
```

**记忆口诀**：`@TestConfiguration` + `@Bean` ≠ 工具类。

---

### 规则 TOOL-005：Testcontainers 与 Docker Desktop 版本兼容性

**症状**：`Could not find a valid Docker environment` 错误，但 Docker CLI 正常工作。

**原因**：Testcontainers 1.20.4 及以下版本与 Docker Engine 29 / Docker Desktop 4.59+ 不兼容。

**解决方案**：
1. 升级到 Testcontainers 1.21.4 或更高版本
2. 确认版本：`gradle/libs.versions.toml` 中 `testcontainers = "1.21.4"`
3. 添加 PostgreSQL JDBC 驱动：
   ```kotlin
   runtimeOnly("org.postgresql:postgresql:42.7.4")
   ```

**验证命令**：
```bash
./gradlew :cartisan-test:test --info | grep "Container is started"
# 应输出：Container postgres:16-alpine started in PT0.6s
```

**相关决策**：见 ADR-029。

---

### 规则 TOOL-006：PIT 变异测试是 Phase 5 必跑门禁

**执行方式**：
```bash
# 方式一：使用脚本（推荐）
./scripts/run-pitest.sh cartisan-core

# 方式二：直接调用 Gradle
./gradlew :cartisan-core:pitest
```

**验收标准**：
- 变异杀死率 ≥ 70%
- 报告位置：`cartisan-core/build/reports/pitest/index.html`
- 存活变异需审查，补充边界测试

**注意**：PIT 较耗时（分钟级），仅在 Phase 5 审查时必跑，编码阶段不需要每次运行。

---

### 规则 TOOL-007：@Component 默认 bean 名称可能与自动配置冲突

**问题**：`@Component` 注解的类，默认 bean 名称是类名首字母小写（如 `RequestContextFilter` → `requestContextFilter`），可能与 Spring Boot 自动配置的 bean 冲突。

**错误表现**：
```
BeanDefinitionOverrideException: Invalid bean definition with name 'requestContextFilter'
... Cannot register bean definition ... since there is already ... bound
```

**错误代码**：
```java
// ❌ 默认 bean 名称是 "requestContextFilter"
@Component
public class RequestContextFilter extends OncePerRequestFilter {
    // ...
}
```

**正确做法**：
```java
// ✅ 显式指定 bean 名称，避免与 Spring Boot 自动配置冲突
@Component("cartisanRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {
    // ...
}
```

**命名建议**：
- 使用模块前缀：`{module}{ClassName}`，如 `cartisanRequestContextFilter`
- 或使用功能前缀：`{feature}{ClassName}`，如 `tenantRequestContextFilter`

**相关**：Spring Boot 自动配置的 `requestContextFilter` 用于 `RequestContextListener`（LocaleResolver / ThemeResolver），与业务自定义的请求上下文 Filter 无关。

**记忆口诀**：`@Component` 显式命名，避自动配置之嫌。

---

## 代码风格

### 规则 STYLE-001：领域接口应包含完整 JavaDoc 和使用示例

**要求**：
- 每个公共接口/类必须有类级别 JavaDoc
- 包含功能描述、使用场景、示例代码
- 示例代码应完整可运行

**模板**：
```java
/**
 * [一句话描述]。
 *
 * <p>[详细描述]。</p>
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * // 完整可运行的示例
 * }</pre>
 *
 * @since 0.1.0
 */
```

---

### 规则 STYLE-002：使用 Record 实现 ValueObject 和 Identity

**推荐做法**：
```java
// ✅ ValueObject
public record Address(String street, String city) implements ValueObject<Address> {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be blank");
        }
    }
}

// ✅ Identity
public record UserId(String value) implements Identity<String> {
    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be blank");
        }
    }
}
```

**优点**：
- 零样板代码
- 编译器自动生成正确的 equals/hashCode/toString
- 不可变，线程安全

---

## 测试

### 规则 TEST-001：使用 AssertJ 而非 JUnit 断言

**推荐做法**：
```java
import static org.assertj.core.api.Assertions.assertThat;

// ✅ AssertJ：链式调用，语义清晰
assertThat(actual).isEqualTo(expected);
assertThat(list).hasSize(3).containsExactly("a", "b", "c");

// ❌ JUnit：不支持链式调用
assertEquals(expected, actual);
```

---

### 规则 TEST-002：测试方法命名应遵循 given-when-then 模式

**模板**：
```java
@Test
void given_{条件}_when_{操作}_then_{预期结果}() {
    // Given
    ...

    // When
    ...

    // Then
    ...
}
```

**示例**：
```java
@Test
void givenNullAggregateId_whenCreateEvent_thenThrowsNullPointerException() {
    // Given - 无

    // When & Then
    assertThatThrownBy(() -> new DomainEvent(null))
        .isInstanceOf(NullPointerException.class);
}
```

---

### 规则 TEST-003：Spring Boot Test 依赖分层

**推荐做法**：
```kotlin
// api 配置暴露给业务项目
api("org.springframework.boot:spring-boot-test:3.4.0")           // @TestConfiguration 等
api("org.springframework.boot:spring-boot-starter-test:3.4.0")    // MockMvc 等
api("org.springframework.boot:spring-boot-testcontainers:3.4.0") // @ServiceConnection

// implementation 仅本模块需要
implementation("org.springframework.boot:spring-boot-starter-data-redis:3.4.0")
```

**原因**：
- `spring-boot-test`：提供 `@TestConfiguration`、`@DynamicPropertySource` 等测试注解
- `spring-boot-starter-test`：提供 MockMvc、`@AutoConfigureMockMvc` 等
- `spring-boot-testcontainers`：提供 `@ServiceConnection`
- `spring-boot-starter-data-redis`：测试需要 `StringRedisTemplate`（业务项目可选）

**常见错误**：
```kotlin
// ❌ 缺少 spring-boot-test，@TestConfiguration 无法解析
// ❌ 缺少 spring-boot-starter-test，MockMvc 无法注入
// ❌ 缺少 spring-boot-starter-data-redis，StringRedisTemplate 无法注入
```

---

## 踩坑记录

### PIT-001 (2026-03-13)：Java 类型擦除导致泛型方法编译失败

**场景**：在 `Entity<T, ID>` 接口的默认方法中调用 `other.getId()` 时编译错误。

**原因**：Java 泛型类型擦除，运行时 `T` 变为 `Object`。

**解决**：添加 `getClass()` 检查和强制类型转换（见 DDD-001）。

---

### PIT-002 (2026-03-13)：ArchUnit 检查生产代码时误报测试依赖

**场景**：ArchUnit 报告 domain 包依赖 AssertJ（实际只有测试依赖）。

**原因**：`ClassFileImporter().importPackages()` 会扫描整个 classpath。

**解决**：使用 `importPaths("build/classes/java/main")` 只扫描生产代码（见 TOOL-001）。

---

### PIT-003 (2026-03-13)：PIT 插件与 Gradle 9.0 不兼容 → 已解决

**场景**：配置 `id("info.solidsoft.pitest") version "1.15.0"` 后构建失败。

**错误**：`Could not get unknown property 'baseDir' for extension 'reporting'`

**原因**：Gradle 9.0 移除了 `ReportingExtension.getBaseDir()`，PIT 1.15.0 仍在使用该 API。

**解决**：升级到 **1.19.0-rc.3**（或 1.19.0-rc.1+），该版本已改用 `baseDirectory`，兼容 Gradle 9。见 TOOL-002。

---

### PIT-004 (2026-03-13)：异常构造器中 formatMessage 调用顺序问题

**场景**：`CartisanException` 构造器中先调用 `super(formatMessage(codeMessage, args))`，再检查 `codeMessage` 是否为 null。

**问题**：
1. 如果 `codeMessage` 为 null，`formatMessage()` 内部调用 `codeMessage.message()` 会抛出 NPE
2. 该 NPE 堆栈不清晰，不会显示自定义的 "codeMessage cannot be null" 消息
3. `Objects.requireNonNull()` 检查永远不会执行到

**正确做法**：
```java
// ✅ 先检查 null，再使用
protected CartisanException(CodeMessage codeMessage, Object... args) {
    super(formatMessage(
            Objects.requireNonNull(codeMessage, "codeMessage cannot be null"),
            args
    ));
}

// ❌ 后检查 null，永远不会执行到
protected CartisanException(CodeMessage codeMessage, Object... args) {
    super(formatMessage(codeMessage, args));  // NPE here if codeMessage is null
    this.codeMessage = Objects.requireNonNull(codeMessage, "codeMessage cannot be null");
}
```

**记忆口诀**：构造器中 super() 调用需要参数时，参数校验必须嵌套在 super() 调用内部。

---

### PIT-005 (2026-03-13)：MessageFormat 参数不足时不会抛异常

**场景**：测试中假设 `MessageFormat.format("Error {0} at {1}", "onlyOne")` 会抛出 `IllegalArgumentException`。

**实际行为**：MessageFormat 不会抛异常，而是保留未替换的占位符，返回 `"Error onlyOne at {1}"`。

**影响**：异常消息可能包含未替换的占位符，需要调用方确保参数数量正确。

**正确做法**：
```java
// ✅ 测试验证实际行为
@Test
void shouldPreservePlaceholder_whenInsufficientArgs() {
    // When - 只提供一个参数
    exception = new TestCartisanException(codeMessage, "type");
    // Then - 占位符被保留
    assertThat(exception.getMessage()).isEqualTo("Error type at {1}");
}
```

**相关规则**：见 ADR-010 边界行为说明。

---

### PIT-006 (2026-03-13)：@Retention(RUNTIME) 是注解可被反射读取的前提

**场景**：ArchUnit 规则无法读取注解元数据。

**原因**：注解默认保留策略为 `CLASS`，字节码中有但运行时不可见；或误设为 `SOURCE`，仅源码中有。

**正确做法**：
```java
// ✅ 架构注解必须使用 RUNTIME 保留策略
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)  // 必须有
public @interface Aggregate {
}
```

**验证**：通过单元测试验证所有注解的 `@Retention(RUNTIME)` 配置（见 StereotypeAnnotationsTest）。

---

### PIT-007 (2026-03-13)：枚举完整性测试应使用精确匹配

**场景**：新增枚举值后忘记添加对应的 ArchUnit 规则，导致架构约束有漏洞。

**正确做法**：
```java
// ✅ 精确匹配，防止新增值
@Test
void portType_shouldHaveExactlyThreeValues() {
    assertThat(PortType.values())
        .hasSize(3)  // 数量约束
        .containsExactlyInAnyOrder(  // 值约束
            PortType.REPOSITORY,
            PortType.CLIENT,
            PortType.PUBLISHER
        );
}
```

**记忆口诀**：枚举是"契约的一部分"，测试必须精确匹配，不能用 `contains` 或 `hasSize` 单独验证。

---

## 架构注解

### 规则 ANNO-001：多个注解共享的枚举应独立定义

**推荐做法**：
```java
// ✅ PortType 独立枚举，被 @Port 和 @Adapter 共享
public enum PortType {
    REPOSITORY, CLIENT, PUBLISHER
}

@Port(PortType.REPOSITORY)
interface OrderRepository {}

@Adapter(PortType.REPOSITORY)
class JpaOrderRepository implements OrderRepository {}
```

**避免**：
```java
// ❌ 内嵌枚举，Adapter 引用 Port 的内部类型语义别扭
public @interface Port {
    enum Type { REPOSITORY, CLIENT, PUBLISHER }
    Type value();
}

@Port(Type.REPOSITORY)  // 正常
interface OrderRepository {}

@Adapter(Type.REPOSITORY)  // 语义错误：Adapter 为什么要用 Port 的类型？
class JpaOrderRepository implements OrderRepository {}
```

---

### 规则 ANNO-002：@Target(TYPE) 无法区分类和接口，语义由 ArchUnit 强制

**问题**：Java 的 `ElementType.TYPE` 同时覆盖 class、interface、enum、record。

**解决方案**：
1. 注解层：使用 `@Target(TYPE)` 做粗粒度限制
2. JavaDoc：说明"仅用于接口"或"仅用于类"
3. ArchUnit：强制执行精确语义约束

**示例**：
```java
/**
 * 端口注解。
 *
 * <p>标注在端口接口上。仅用于接口，由 ArchUnit 规则强制检查。</p>
 */
@Target(TYPE)  // 粗粒度：TYPE
@Retention(RUNTIME)
public @interface Port {
    PortType value();
}
```

---

## 断言工具 (Assertions)

### 规则 ASRT-001：异常类型语义决定 HTTP 状态码

**原则**：断言方法抛出的异常类型应反映"这是谁的问题"，而非"哪个层级抛出的"。

| 断言方法 | 异常类型 | 语义 | HTTP | 运维 |
|---------|---------|------|------|------|
| `require()` | `DomainException` | 调用者责任 = 业务规则违反 | 4xx | 正常日志 |
| `ensure()` | `IllegalStateException` | 实现者责任 = 代码 bug | 500 | Bug 告警 |
| `requirePresent()` | `DomainException` | 资源不存在 | 404/4xx | 正常日志 |

**错误示例**：
```java
// ❌ 后置条件失败伪装成业务异常
public void addItem(OrderItem item) {
    this.items.add(item);
    ensure(this.items.contains(item),
        new DomainException(BaseCodeMessage.INTERNAL_ERROR));  // 误导性
}
```

**正确示例**：
```java
// ✅ 后置条件失败明确表示代码 bug
public void addItem(OrderItem item) {
    this.items.add(item);
    ensure(this.items.contains(item), "item should be present after add");
}
```

---

### 规则 ASRT-002：工具类私有构造函数应抛出异常而非返回 null

**推荐做法**：
```java
// ✅ 防止反射实例化
private Assertions() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
}
```

**避免**：
```java
// ❌ 返回 null 无法阻止反射调用
private Assertions() {
    // 空构造函数
}
```

**测试验证**：
```java
@Test
void should_throw_exception_when_attempting_instantiation_via_reflection() throws Exception {
    Constructor<Assertions> constructor = Assertions.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance)
        .hasCauseExactlyInstanceOf(UnsupportedOperationException.class);
}
```

---

## 踩坑记录（续）

### PIT-008 (2026-03-13)：ReflectionnewInstance 抛出 InvocationTargetException

**场景**：通过反射调用 `Constructor.newInstance()` 时，实际抛出 `InvocationTargetException` 而非构造函数内部的异常。

**原因**：`Constructor.newInstance()` 会将构造函数抛出的异常包装在 `InvocationTargetException` 中，需要通过 `.getCause()` 获取原始异常。

**正确做法**：
```java
// ✅ 使用 hasCauseExactlyInstanceOf 检查根本原因
assertThatThrownBy(constructor::newInstance)
    .hasCauseExactlyInstanceOf(UnsupportedOperationException.class)
    .satisfies(ex -> {
        Throwable cause = ex.getCause();
        assertThat(cause.getMessage()).contains("Utility class");
    });
```

**记忆口诀**：反射构造异常被包装，用 `getCause()` 取真身。

---

### PIT-009 (2026-03-13)：Docker Desktop Socket 配置问题

**场景**：Testcontainers 在 macOS Docker Desktop 环境下报错 "Could not find a valid Docker environment"。

**原因**：Testcontainers 默认查找 `/var/run/docker.sock`，但 Docker Desktop 使用不同 socket 路径。

**解决方案**：
1. 确保 Docker Desktop 正在运行
2. 检查 `docker ps` 命令是否正常
3. 如仍失败，检查 Ryuk 容器是否被阻止（Docker Desktop 4.25+ 需要配置）
4. 或设置环境变量：`export DOCKER_HOST=unix:///var/run/docker.sock`

**注意**：这是 Docker Desktop 配置问题，不是代码问题。代码在生产环境 Linux Docker 下可正常工作。

---

### PIT-010 (2026-03-13)：测试模块需要 spring-boot-starter-data-redis

**场景**：`IntegrationTestBase` 编译失败，提示 `StringRedisTemplate` 找不到符号。

**原因**：`StringRedisTemplate` 在 `spring-boot-starter-data-redis` 中，需要显式依赖。

**正确做法**：
```kotlin
// cartisan-test/build.gradle.kts
dependencies {
    // ... 其他依赖

    // Redis 支持（测试需要 StringRedisTemplate）
    implementation("org.springframework.boot:spring-boot-starter-data-redis:3.4.0")
}
```

**记忆口诀**：要用的类型就要引入对应的 starter。

---

### PIT-011 (2026-03-14)：Spring Test api 配置不保证本模块可用

**场景**：`cartisan-test` 模块的 main 代码使用 Spring Test 的类（如 `RequestPostProcessor`）时编译失败。

**错误**：
```
错误: 找不到符号
  位置: 类 org.springframework.test.web.servlet.RequestPostProcessor
```

**原因**：
1. Gradle 的 `api` 配置会将依赖暴露给使用者
2. 但 `api` 不会让依赖对本模块的 main 代码编译可用
3. Spring Test 相关类需要在 main 代码中使用（`ApiTestAssertions`）

**正确做法**：
```kotlin
// cartisan-test/build.gradle.kts
dependencies {
    // api 配置暴露给业务项目
    api("org.springframework:spring-test:6.2.0")

    // implementation 确保本模块 main 代码可用
    implementation("org.springframework:spring-test:6.2.0")
}
```

**记忆口诀**：api 暴露给他人，implementation 自己用。main 代码依赖要加 implementation。

---

### PIT-012 (2026-03-14)：RequestPostProcessor 的正确导入路径

**场景**：导入 `RequestPostProcessor` 时编译失败，提示"找不到符号"。

**常见错误**：
```java
// ❌ 错误假设：直接在 org.springframework.test.web.servlet 下
import org.springframework.test.web.servlet.RequestPostProcessor;
```

**正确导入**：
```java
// ✅ 正确路径：位于 request 子包
import org.springframework.test.web.servlet.request.RequestPostProcessor;
```

**验证方法**：检查 JAR 包内容
```bash
# 1. 找到 spring-test JAR
find ~/.gradle/caches -name "spring-test-*.jar" | head -1

# 2. 查看类路径
jar tf <jar-path> | grep RequestPostProcessor
# 输出：org/springframework/test/web/servlet/request/RequestPostProcessor.class
```

**记忆口诀**：RequestPostProcessor 在 request 子包，多一层目录。

---

### PIT-013 (2026-03-14)：@WebMvcTest 需要 SpringBootConfiguration

**场景**：使用 `@WebMvcTest` 测试 `ApiTestAssertions` 时，Spring 上下文加载失败。

**错误**：
```
Unable to find a @SpringBootConfiguration
```

**原因**：`@WebMvcTest` 需要一个配置类来启动 Spring 应用上下文。

**正确做法**：
```java
// ✅ TestConfiguration 添加 @SpringBootConfiguration
@SpringBootConfiguration
@ImportAutoConfiguration({
    JacksonAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    RedisAutoConfiguration.class
})
@ComponentScan(basePackages = "com.cartisan.test.base")
public class TestConfiguration {
}

// ✅ 测试类排除不需要的自动配置
@WebMvcTest(controllers = TestController.class,
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        RedisAutoConfiguration.class
    })
public class ApiTestAssertionsTest {
    // ...
}
```

**记忆口诀**：`@WebMvcTest` 需要 `@SpringBootConfiguration`，排除不需要的依赖加快启动。

---

### PIT-014 (2026-03-14)：ResultMatcher lambda 需要返回 request

**场景**：`RequestPostProcessor` lambda 中 `request.addHeader()` 后没有返回语句，编译失败。

**错误**：
```java
// ❌ 编译错误：void 无法转换为 MockHttpServletRequest
public static RequestPostProcessor withToken(String token) {
    return request -> request.addHeader("Authorization", "Bearer " + token);
}
```

**正确做法**：
```java
// ✅ 返回 request
public static RequestPostProcessor withToken(String token) {
    return request -> {
        request.addHeader("Authorization", "Bearer " + token);
        return request;  // 必须返回
    };
}
```

**原因**：`RequestPostProcessor` 函数式接口要求返回 `MockHttpServletRequest`。

**记忆口诀**：RequestPostProcessor lambda 最后要 return request。

---

### PIT-015 (2026-03-14)：Math.abs(nextLong()) 导致 Long.MIN_VALUE 溢出

**场景**：使用 `Math.abs(random.nextLong())` 生成非负长整数时，偶发返回负数。

**原因**：`Long.MIN_VALUE` 的绝对值超出 `Long.MAX_VALUE` 范围，`Math.abs(Long.MIN_VALUE)` 返回 `Long.MIN_VALUE`（负数）。

**错误代码**：
```java
// ❌ 当 nextLong() 返回 Long.MIN_VALUE 时溢出
public static long randomLong() {
    return Math.abs(FixtureSeeds.currentRandom().nextLong());
}
```

**正确做法**：
```java
// ✅ 直接使用有界方法，避免溢出
public static long randomLong() {
    return FixtureSeeds.currentRandom().nextLong(Long.MAX_VALUE);
}
```

**记忆口诀**：取绝对值要当心 MIN_VALUE 溢出，用 nextLong(bound) 更安全。

---

### PIT-016 (2026-03-14)：nextDouble() * max 边界值可能返回 0

**场景**：`randomAmount()` 要求返回 `(0, max]` 范围，但使用 `nextDouble() * max` 时可能返回 `0.0`。

**原因**：`Random.nextDouble()` 返回 `[0.0, 1.0)`，最小值可以是 `0.0`，导致 `0.0 * max = 0.0`。

**错误代码**：
```java
// ❌ 可能返回 0.0，违反 "(0, max]" 约束
public static BigDecimal randomAmount() {
    double value = FixtureSeeds.currentRandom().nextDouble() * max;
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
}
```

**正确做法**：
```java
// ✅ 使用 min + random() * (max - min) 确保下界
public static BigDecimal randomAmount() {
    double max = DEFAULT_AMOUNT_MAX.doubleValue();
    double value = 0.01 + FixtureSeeds.currentRandom().nextDouble() * (max - 0.01);
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
}
```

**记忆口诀**：随机数要排除 0，用 min + random() * (max - min)。

---

### PIT-017 (2026-03-14)：Spring Security AccessDeniedException 依赖问题

**场景**：在 `cartisan-web` 中需要处理权限拒绝异常，返回 403 状态码。

**问题**：Spring Security 的 `AccessDeniedException` 位于 `spring-security-web` 模块，但 cartisan-web 不应强制依赖 Spring Security。

**临时方案**：使用 `IllegalArgumentException` 并检查消息内容：
```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ApiResponse<Void>> handleAccessDenied(IllegalArgumentException ex) {
    if (ex.getMessage() != null && ex.getMessage().contains("Access denied")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(BaseCodeMessage.FORBIDDEN));
    }
    // 其他 IllegalArgumentException 返回 400
    return ResponseEntity.badRequest()
            .body(ApiResponse.error(400, ex.getMessage()));
}
```

**后续计划**：在 cartisan-security 模块中实现真正的 `AccessDeniedException` 处理器。

**记忆口诀**：权限异常临时用 IllegalArgumentException 模拟，消息含 "Access denied" 返回 403。

---

### PIT-018 (2026-03-14)：@Validated 必须放在类上触发 @RequestParam 校验

**场景**：测试 `@RequestParam @Email String email` 时，`ConstraintViolationException` 没有被触发。

**原因**：`@Validated` 注解必须放在 Controller 类上，Spring 才会校验方法参数。

**错误代码**：
```java
// ❌ @Validated 缺失，参数校验不生效
@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/validate-request-param")
    public void validateRequestParam(@RequestParam @Email String email) {
        // 不会触发校验
    }
}
```

**正确做法**：
```java
// ✅ 添加 @Validated
@Validated  // 必须有
@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/validate-request-param")
    public void validateRequestParam(@RequestParam @Email String email) {
        // Spring 会先校验，校验失败抛出 ConstraintViolationException
    }
}
```

**记忆口诀**：@RequestParam 校验要生效，类上必须加 @Validated。

---

### PIT-019 (2026-03-14)：测试 405 异常需用不支持的 HTTP 方法

**场景**：测试 `HttpRequestMethodNotSupportedException` 时，期望返回 405 但实际返回 200。

**原因**：TestController 的 endpoint 使用了 `@PostMapping`，测试也用 `POST`，方法匹配成功。

**错误代码**：
```java
// ❌ 方法匹配，不会触发异常
@PostMapping("/method-not-allowed")
public void methodNotAllowed() {
}

// 测试
mockMvc.perform(post("/test/method-not-allowed"))  // 返回 200
```

**正确做法**：
```java
// ✅ endpoint 只支持 GET
@GetMapping("/method-not-allowed")
public void methodNotAllowed() {
}

// 测试用 POST 触发 405
mockMvc.perform(post("/test/method-not-allowed"))
    .andExpect(status().isMethodNotAllowed());
```

**记忆口诀**：测 405 异常，endpoint 和测试要用不同 HTTP 方法。

---

### PIT-020 (2026-03-14)：@RequestBody String 不会触发 JSON 解析异常

**场景**：测试 `HttpMessageNotReadableException` 时，畸形 JSON 仍然返回 200。

**原因**：`@RequestBody String` 会将请求体作为原始字符串接收，不进行 JSON 解析。

**错误代码**：
```java
// ❌ String 接收原始内容，不解析 JSON
@PostMapping("/malformed-json")
public void malformedJson(@RequestBody String body) {
}
```

**正确做法**：
```java
// ✅ 使用 Object 或具体类型，触发 JSON 解析
@PostMapping("/malformed-json")
public void malformedJson(@RequestBody Object body) {
    // JSON 解析失败会抛出 HttpMessageNotReadableException
}
```

**记忆口诀**：测 JSON 解析异常，@RequestBody 不要用 String。

---

### PIT-021 (2026-03-14)：Testcontainers 与 Docker Engine 29 不兼容

**场景**：Testcontainers 集成测试报错 "Could not find a valid Docker environment"，但 `docker ps` 命令正常工作。

**原因**：Testcontainers 1.20.x 与 Docker Engine 29 / Docker Desktop 4.59+ 不兼容。

**解决方案**：
```toml
# gradle/libs.versions.toml
[versions]
testcontainers = "1.21.4"  # 从 1.20.4 升级
```

```kotlin
# cartisan-test/build.gradle.kts
// 添加 PostgreSQL JDBC 驱动（Testcontainers 需要实际驱动连接数据库）
runtimeOnly("org.postgresql:postgresql:42.7.4")
```

**验证**：
```bash
./gradlew :cartisan-test:test --info | grep "Container is started"
# ✅ 成功：Container postgres:16-alpine started in PT0.6s
# ❌ 失败：Could not find a valid Docker environment
```

**相关决策**：见 ADR-029。

**记忆口诀**：Docker 报错但 CLI 正常？升级 Testcontainers 到 1.21.4+。


---

## Spring Data JPA / 数据访问

### 规则 DATA-001：JPA save() 后必须用原始 entity 发布事件

**问题**：`SimpleJpaRepository.save()` 返回的可能是一个新实例（如延迟加载代理），不是原始传入的实体。

**错误代码**：
```java
@Override
public <S extends T> S save(S entity) {
    S savedEntity = super.save(entity);
    publishDomainEvents(savedEntity);  // ❌ savedEntity 上的事件是空的！
    return savedEntity;
}
```

**正确做法**：
```java
@Override
public <S extends T> S save(S entity) {
    S savedEntity = super.save(entity);
    publishDomainEvents(entity);  // ✅ 使用原始 entity
    return savedEntity;
}
```

**调试证据**：
```
savedEntity.events.size() = 0  // JPA 返回的新实例
entity.events.size() = 1       // 原始实例才有事件
```

**记忆口诀**：JPA save 返回值 ≠ 原始参数，后处理必须用原参数。

---

### 规则 DATA-002：Spring Data JPA 创建的 Repository 不是 Spring Bean

**问题**：Repository 接口的实现类由 Spring Data JPA 在运行时动态生成，不在 Spring 容器中。

**现象**：
```java
public class BaseRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> {
    // ❌ 无法自动注入——这个构造方法 Spring 不会调用
    @Autowired
    public BaseRepositoryImpl(...) {
        // this.domainEventPublisher 永远是 null
    }
}
```

**正确做法**：使用静态持有者模式
```java
// 1. 创建静态持有者
public final class DomainEventPublisherHolder {
    private static volatile DomainEventPublisher publisher;
    public static void setPublisher(DomainEventPublisher p) { publisher = p; }
    public static DomainEventPublisher getPublisher() { return publisher; }
}

// 2. 在 AutoConfiguration 中设置
@Bean
public Runnable configureDomainEventPublisherHolder(DomainEventPublisher publisher) {
    return () -> DomainEventPublisherHolder.setPublisher(publisher);
}

// 3. Repository 中使用
var publisher = DomainEventPublisherHolder.getPublisher();
```

**替代方案（不推荐）**：自定义 JpaRepositoryFactoryBean + Factory（过于复杂）

**记忆口诀**：Repository 不是 Bean，依赖注入用静态持有者。

---

## Spring Boot / 自动配置

### 规则 BOOT-001：使用 JpaRepositoryFactoryEntryCustomizer 自动配置 repositoryBaseClass

**问题**：默认情况下，每个使用 `@EnableJpaRepositories` 的地方都需要手动指定 `repositoryBaseClass`。

**传统做法（繁琐）**：
```java
@EnableJpaRepositories(
    basePackages = "com.cartisan.**.repository",
    repositoryBaseClass = BaseRepositoryImpl.class  // ❌ 每处都要写
)
```

**正确做法**：在 AutoConfiguration 中全局配置
```java
@Bean
public JpaRepositoryFactoryEntryCustomizer repositoryFactoryEntryCustomizer() {
    return (JpaRepositoryFactoryBean<?, ?, ?> factoryBean) -> {
        factoryBean.setRepositoryBaseClass(BaseRepositoryImpl.class);
    };
}
```

**效果**：所有 Repository 自动使用 `BaseRepositoryImpl` 作为基类，无需手动配置。

**记忆口诀**：全局配置用 Customizer，不要散落各处。
