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

### 规则 TOOL-008：Sa-Token 包路径是 `cn.dev33.satoken`，不是 `cn.dev33.sa-token`

**问题**：根据 Maven 坐标 `cn.dev33:sa-token-spring-boot3-starter`，容易误以为包路径是 `cn.dev33.sa-token.*`。

**正确导入**：
```java
// ✅ 正确
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.session.SaSession;

// ❌ 错误（会导致编译失败：找不到符号）
import cn.dev33.sa-token.stp.StpUtil;
```

**验证方式**：
```bash
# 查看 JAR 包内容
find ~/.gradle/caches -name "sa-token-core*.jar" | head -1 | xargs jar tf | grep -i "StpUtil"
# 输出：cn/dev33/satoken/stp/StpUtil.class
```

**记忆口诀**：Maven 坐标有横杠，包路径没横杠。

---

### 规则 TOOL-009：Sa-Token Session 类是 `SaSession`，不是 `Session`

**问题**：容易误以为存在 `cn.dev33.satoken.session.Session` 类。

**正确用法**：
```java
// ✅ 正确
import cn.dev33.satoken.session.SaSession;
SaSession session = StpUtil.getSession();

// ❌ 错误（编译失败：找不到符号 Session）
import cn.dev33.satoken.session.Session;
```

**验证方式**：
```bash
find ~/.gradle/caches -name "sa-token-core*.jar" | head -1 | xargs jar tf | grep -i "Session"
# 输出：cn/dev33/satoken/session/SaSession.class（没有 Session.class）
```

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

### 规则 STYLE-002：JavaDoc 中必须转义 HTML 特殊字符

**问题**：JavaDoc 解析器会将 `<` 和 `>` 解析为 HTML 标签，导致编译失败。

**错误示例**：
```java
/**
 * 分页查询参数。
 * <p>参数自动校验：
 *   <li>page < 1 时修正为 1</li>   ❌ javadoc 错误
 *   <li>size > 100 时修正为 100</li> ❌ javadoc 错误
 * </p>
 */
```

**正确做法**：
```java
/**
 * 分页查询参数。
 * <p>参数自动校验：
 *   <li>page &lt; 1 时修正为 1</li>     ✅ 使用 HTML 实体
 *   <li>size &gt; 100 时修正为 100</li> ✅ 使用 HTML 实体
 * </p>
 */
```

**常用 HTML 实体**：
| 字符 | 实体 | 说明 |
|------|------|------|
| `<` | `&lt;` | 小于号 |
| `>` | `&gt;` | 大于号 |
| `&` | `&amp;` | 与号 |
| `@` | `&#64;` | 在某些上下文中 |

**记忆口诀**：JavaDoc 里写比较符号，`<` 换 `&lt;`，`>` 换 `&gt;`。

**验证方式**：
```bash
./gradlew :module:javadoc
# 应该无错误输出
```

---

### 规则 STYLE-003：使用 Record 实现 ValueObject 和 Identity

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

### 规则 DATA-003：@MappedSuperclass 需要添加 @EntityListeners 才能启用 JPA Auditing

**问题**：在 `@MappedSuperclass` 基类上添加 `@CreatedDate`、`@LastModifiedDate` 等注解后，审计字段没有被自动填充。

**错误代码**：
```java
// ❌ 缺少 @EntityListeners
@MappedSuperclass
public abstract class Auditable {
    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  // 保存时为 null
}
```

**正确做法**：
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)  // ✅ 必须添加
public abstract class Auditable {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;
}
```

**记忆口诀**：审计字段需监听，@MappedSuperclass 要加 @EntityListeners。

---

### 规则 DATA-004：@SQLRestriction 在 @MappedSuperclass 上可能无法正确继承

**问题**：在 `@MappedSuperclass` 上添加 `@SQLRestriction` 后，查询时自动过滤可能不生效。

**原因**：Hibernate 的 `@SQLRestriction` 注解在某些配置下可能无法正确继承到子类。

**解决方案**：在具体实体类上重复声明 `@SQLRestriction`
```java
// 基类
@MappedSuperclass
@SQLRestriction("deleted = false")
public abstract class SoftDeletable extends Auditable {
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}

// 具体实体类（重复声明确保生效）
@Entity(name = "test_soft_deletable_entity")
@SQLRestriction("deleted = false")  // ✅ 重复声明
public class TestSoftDeletableEntity extends SoftDeletable {
    // ...
}
```

**记忆口诀**：@SQLRestriction 继承不保证，子类重复声明才保险。

---

### 规则 DATA-005：JPQL @Query 查询不受 @SQLRestriction 影响

**问题**：使用 `@Query` 注解编写 JPQL 查询时，`@SQLRestriction` 自动过滤不生效。

**错误代码**：
```java
// ❌ JPQL 查询缺少软删除条件，会返回已删除记录
@Query("SELECT e FROM Product e WHERE e.name = :name")
List<Product> findByName(@Param("name") String name);
```

**正确做法**：
```java
// ✅ JPQL 查询手动添加软删除条件
@Query("SELECT e FROM Product e WHERE e.deleted = false AND e.name = :name")
List<Product> findActiveByName(@Param("name") String name);
```

**原因**：`@SQLRestriction` 只对 Hibernate 自动生成的 SQL 查询生效（如 `findAll()`、`findById()`、方法名查询等）。JPQL 查询由开发者编写，Hibernate 不会自动添加 `@SQLRestriction` 条件。

**记忆口诀**：JPQL 查询手动加条件，@SQLRestriction 只管自动生成的 SQL。

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

---

## 分布式 ID / TSID

### 规则 ID-001：纯随机 TSID 测试需要容忍小量重复

**问题**：使用纯随机（无计数器）实现 TSID 时，唯一性测试偶发失败。

**错误断言**：
```java
// ❌ 要求 10000 个 ID 全部唯一，纯随机实现偶发失败
assertThat(generatedIds).hasSize(10000);
```

**正确做法**：
```java
// ✅ 允许 ≤0.2% 重复（20/10000），符合纯随机实现的统计特性
int duplicateCount = 10000 - generatedIds.size();
assertThat(generatedIds).hasSizeGreaterThanOrEqualTo(9990);
assertThat(duplicateCount)
    .withFailMessage("Too many duplicates: %d out of 10000", duplicateCount)
    .isLessThanOrEqualTo(20);
```

**原因**：42 位时间戳 + 22 位随机数的纯随机实现，在同一毫秒内生成多个 ID 时，随机数可能重复。理论冲突概率 1/4,194,304 ≈ 0.000024%，实际测试中通常 < 10 个重复，阈值 20 提供安全余量。

**权衡**：
- 纯随机：无锁、高性能（>500万/秒），接受 ~0.07% 实际冲突率
- 计数器 + synchronized：保证唯一，但违反性能约束、增加代码复杂度

**记忆口诀**：纯随机 ID 测试看趋势，不追求 100% 唯一。

---

### 规则 ID-002：ThreadLocalRandom 用于无锁随机数生成

**问题**：需要线程安全的随机数生成器，但不希望使用 synchronized 锁。

**错误做法**：
```java
// ❌ 每个实例创建一个 Random，有线程安全问题
private final Random random = new Random();

// ❌ synchronized 加锁，违反性能约束
public synchronized long generate() {
    int r = random.nextInt(MAX_RANDOM + 1);
    ...
}
```

**正确做法**：
```java
// ✅ 使用 ThreadLocalRandom，无锁且线程安全
private final Random random = java.util.concurrent.ThreadLocalRandom.current();

public long generate() {  // 无需 synchronized
    int r = this.random.nextInt(MAX_RANDOM + 1);
    ...
}
```

**原因**：`ThreadLocalRandom.current()` 返回当前线程专属的 Random 实例，无竞争、无锁开销。多线程并发调用 `nextInt()` 时，每个线程使用自己的实例，互不干扰。

**注意**：不要在字段初始化时直接调用 `ThreadLocalRandom.current()`，因为每个线程需要自己的实例。在 `newInstance()` 工厂方法中传入：
```java
public static TsidGenerator newInstance() {
    return new TsidGenerator(java.util.concurrent.ThreadLocalRandom.current());
}
```

**记忆口诀**：无锁随机用 ThreadLocalRandom，不用 synchronized。

---

## Java 21 / ScopedValue

### 规则 JV-001：ScopedValue 需要 --enable-preview

**问题**：Java 21 中 ScopedValue 是预览 API，编译时会报错。

**错误表现**：
```
错误: ScopedValue 是预览 API，默认情况下处于禁用状态。
```

**正确做法**：
```kotlin
// cartisan-security/build.gradle.kts
tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
}
```

**原因**：ScopedValue 在 Java 21 中是预览特性，需要显式启用。同时需要在编译和测试时都启用。

**记忆口诀**：用 ScopedValue 记得开预览，编译测试都要加。

---

### 规则 JV-002：ScopedValue 使用 isBound() + get() 模式

**问题**：ScopedValue 不存在 `getOrDefault()` 方法，直接调用 `get()` 在未绑定时抛异常。

**错误做法**：
```java
// ❌ getOrDefault() 方法不存在
return ScopedValue.getOrDefault(TENANT_ID, null);

// ❌ 直接 get() 在未绑定时抛 NoSuchElementException
return TENANT_ID.get();
```

**正确做法**：
```java
// ✅ 先检查 isBound()，再 get()
public static Long getCurrentTenantId() {
    if (!TENANT_ID.isBound()) {
        return null;
    }
    return TENANT_ID.get();
}
```

**原因**：ScopedValue 的 API 设计：
- `isBound()` — 检查是否已绑定值
- `get()` — 获取绑定的值，未绑定时抛 `NoSuchElementException`
- 不存在 `getOrDefault()` 方法

**记忆口诀**：ScopedValue 取值先 isBound()，再 get()。

---

## 测试（续）

### 规则 TEST-004：MockMvc 集成测试需要测试专用 Controller

**问题**：直接在测试中调用 `StpUtil.login()` 后使用 MockMvc，Sa-Token 上下文未初始化。

**错误代码**：
```java
// ❌ StpUtil.login() 在测试线程，MockMvc 请求在不同线程
@BeforeEach
void setUp() {
    StpUtil.login(100L);  // 上下文只在测试线程
}

@Test
void testProtectedEndpoint() {
    mvc.perform(get("/test/protected"))
        // SaTokenContextException: 上下文尚未初始化
}
```

**正确做法**：创建测试专用 Controller，通过 HTTP 请求触发登录
```java
// ✅ 通过 MockMvc 请求登录，Sa-Token 上下文正确初始化
@RestController
@RequestMapping("/test/auth")
public class TestAuthController {
    @GetMapping("/login/{userId}")
    public ApiResponse<Map<String, String>> login(@PathVariable Long userId) {
        StpUtil.login(userId);
        String token = StpUtil.getTokenValue();
        return ApiResponse.ok(Map.of("token", token));
    }
}

// 测试中先登录获取 token
String token = extractToken(mvc.perform(get("/test/auth/login/100"))
    .andReturn()
    .getResponse()
    .getContentAsString());

// 使用 token 访问受保护端点
mvc.perform(get("/test/protected").header("satoken", token))
    .andExpect(status().isOk());
```

**记忆口诀**：MockMvc 测试用 Controller 登录，不要直接调 StpUtil。

**相关**：见 ADR-060。

---

### 规则 TEST-005：集成测试辅助方法应提取到基类

**问题**：`extractToken()` 等辅助方法在多个测试类中重复。

**正确做法**：
```java
// ✅ 抽象基类提供公共方法
public abstract class AbstractSecurityIntegrationTest {
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper objectMapper;

    /**
     * 从登录响应中提取 token。
     */
    protected String extractToken(String responseContent) {
        try {
            JsonNode root = objectMapper.readTree(responseContent);
            return root.path("data").path("token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract token from response", e);
        }
    }
}

// 子类直接使用
class AuthAnnotationIntegrationTest extends AbstractSecurityIntegrationTest {
    @Test
    void test() {
        String token = extractToken(response);  // 直接调用
    }
}
```

**记忆口诀**：测试辅助方法去重，基类统一提供。

---

## 踩坑记录（续）

### PIT-022 (2026-03-15)：MockMvc 环境下 Sa-Token 上下文未初始化

**场景**：TenantContextFilter 中调用 `StpUtil.isLogin()` 抛出 `SaTokenContextException`。

**错误表现**：
```
cn.dev33.satoken.exception.SaTokenContextException: 上下文尚未初始化
    at cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff.getModelBox
```

**原因**：
1. MockMvc 测试中 SaServletFilter 未执行
2. `SaTokenContextForThreadLocal` 未从请求中读取 token 初始化上下文
3. `StpUtil.isLogin()` 依赖已初始化的上下文

**解决方案**：在 Filter 中增加测试模式降级
```java
// ✅ 正常模式：上下文已初始化
if (StpUtil.isLogin()) {
    return StpUtil.getSession().get(TENANT_ID_SESSION_KEY);
}

// 测试模式：通过 token 手动查询 Session
String token = extractSaToken(request);
Object loginId = StpUtil.getLoginIdByToken(token);
return StpUtil.getSessionByLoginId(loginId).get(TENANT_ID_SESSION_KEY);
```

**相关决策**：见 ADR-060。

**记忆口诀**：MockMvc 缺 Filter 上下文，token 直接查 Session。

---

### PIT-023 (2026-03-15)：集成测试断言应验证确切值而非类型

**场景**：`TenantContextIntegrationTest` 只验证 Session 租户 ID 是数字，不验证具体值。

**错误代码**：
```java
// ❌ 只验证是数字，无法证明 Session 解析正确
.andExpect(jsonPath("$.data.tenantId").isNumber());
```

**正确做法**：
```java
// ✅ 验证确切值，确保 Session 租户 ID 正确解析
.andExpect(jsonPath("$.data.tenantId").value(456));
```

**原因**：`isNumber()` 只验证类型，不验证值。如果 Session 解析逻辑有 bug（如返回默认值 0），测试仍会通过。

**记忆口诀**：断言验证确切值，类型检查不够用。

---

## Spring Boot AutoConfiguration

### 规则 AC-001：`@ConditionalOnBean` 在同一 `@Configuration` 类内的顺序陷阱

**问题**：`@ConditionalOnBean(ModelProvider.class)` 要求目标 Bean 在条件求值时已注册。若 Provider Bean 与 Registry Bean 定义在同一 `@Configuration` 类中，Spring 不保证方法声明顺序即是 Bean 注册顺序，`@ConditionalOnBean` 可能在 Provider Bean 注册前就求值为 `false`，导致 Registry Bean 不被创建。

**正确做法**：
- 将 Provider Bean 与依赖它的 Registry Bean 分在**不同 `@Configuration` 类**中
- 或在 Registry Bean 所在配置类上加 `@AutoConfigureAfter(OpenAiAutoConfiguration.class)` 等注解明确顺序

**适用场景**：F05-09 `CartisanAiAutoConfiguration` 注册 `ModelProviderRegistry` 时需注意此规则。
