# 团队规则库 (SKILL)

> 定位：团队的"经验手册"——记录所有值得沉淀的踩坑经验和铁律。AI 在开发前会读取它，避免重复犯错。

---

## DDD / 领域建模

### 规则 DDD-001：Java 泛型类型擦除导致的编译问题

**问题**：在接口默认方法中，泛型类型参数 `T` 在运行时被擦除为 `Object`，无法直接调用具体方法。

**错误代码**：
```java
public interface DomainEntity<T, ID> {
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
    ID otherId = ((DomainEntity<T, ID>) other).getId();
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

**对比 DomainEntity**：DomainEntity 需要比较 ID，而 ID 是泛型方法获取的，所以需要额外处理。

---

### 规则 DDD-004：值对象 JPA 映射分类处理

**决策**：不使用单值值对象，直接使用基础类型 + 验证；复杂值对象使用 `@Embeddable`。

**分类处理**：

| 类型 | 处理方式 | 理由 |
|------|---------|------|
| **简单值**（Email、PhoneNumber） | `String` + 构造函数验证 | 验证简单，避免过度设计 |
| **复杂值**（Address、Money） | `@Embeddable` + `@Embedded` | JPA 原生支持，值对象有意义 |

**代码示例**：
```java
@Entity
@Table(name = "users")
public class User {

    @Column(name = "email", length = 255)
    private String email;

    @Embedded
    private Address address;  // 复杂值对象

    // 构造函数中验证
    public User(String email) {
        Assertions.require(EmailUtil.isEmail(email), "邮箱格式无效");
        this.email = email;
    }
}

@Embeddable
public class Address {
    @Column(name = "province")
    private String province;

    @Column(name = "city")
    private String city;

    @Column(name = "detail")
    private String detail;
}
```

**理由**：
1. 简单值用 String + 验证，代码量少，易于理解
2. 避免 AttributeConverter 的样板代码
3. 复杂值对象继续使用 JPA 的 `@Embeddable`

**记忆口诀**：简单值 String 验证，复杂值 Embeddable。

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

### 规则 TOOL-002：ArchUnit 分层规则的例外情况

**问题**：ArchUnit 规则过于严格，导致合理的框架使用被误报。

**domainShouldNotDependOnSpring 例外**：
- **允许**：Repository 接口使用 Spring Data JPA 注解（`@Query`、`@Param`）
- **理由**：这些注解是接口定义的一部分，非实现依赖
- **实现**：规则使用 `.areNotInterfaces()` 排除接口

**controllersShouldOnlyDependOnApplication 例外**：
- **允许**：Controller 方法参数使用 BaseEnum 类型
- **理由**：BaseEnum 参数绑定是框架功能，支持枚举 ↔ Integer 自动转换
- **实现**：规则只禁止 `domain.aggregate` 和 `domain.entity`，不禁止 `domain.enums`

**代码示例**：
```java
// ✅ 允许：Repository 接口使用 Spring 注解
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
}

// ✅ 允许：Controller 使用枚举参数
@RestController
public class UserController {
    @GetMapping("/users")
    public ApiResponse<List<UserDTO>> listByStatus(UserStatus status) {
        // UserStatus 是 BaseEnum，自动从 Integer 转换
        return ApiResponse.success(appService.listByStatus(status));
    }
}

// ❌ 禁止：Controller 直接依赖聚合根
@RestController
public class BadController {
    private final UserAggregate user;  // 违规
}
```

---

### 规则 TOOL-003：PIT Maven 插件版本配置

**现状**：
- 项目使用 Maven；PIT 插件 `org.pitest:pitest-maven:1.15.0` 可正常使用。

**正确配置**（Maven）：
```xml
<!-- cartisan-core/pom.xml -->
<build>
    <plugins>
        <plugin>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-maven</artifactId>
            <version>1.15.0</version>
            <dependencies>
                <dependency>
                    <groupId>org.pitest</groupId>
                    <artifactId>pitest-junit5-plugin</artifactId>
                    <version>1.2.1</version>
                </dependency>
            </dependencies>
            <configuration>
                <targetClasses>
                    <param>com.cartisan.core.domain.*</param>
                </targetClasses>
                <targetTests>
                    <param>com.cartisan.core.domain.*</param>
                    <param>com.cartisan.core.arch.*</param>
                </targetTests>
                <mutationThreshold>70</mutationThreshold>
                <outputFormats>
                    <outputFormat>HTML</outputFormat>
                    <outputFormat>XML</outputFormat>
                </outputFormats>
                <timestampedReports>false</timestampedReports>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

### 规则 TOOL-004：ArchUnit 规则类不适用于 PIT 变异测试

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

### 规则 TOOL-005：集成测试需要手动启动测试环境

**背景**：项目不使用 Testcontainers，避免与本地 Docker 冲突。

**解决方案**：手动启动 PostgreSQL 和 Redis 用于测试。

```bash
# 启动 PostgreSQL（测试用）
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=testdb \
  -e POSTGRES_USER=test \
  -e POSTGRES_PASSWORD=test \
  postgres:16-alpine

# 启动 Redis（测试用）
docker run -d -p 6379:6379 redis:7-alpine
```

**环境变量配置（可选）**：
```bash
export TEST_DB_URL=jdbc:postgresql://localhost:5432/testdb
export TEST_DB_USER=test
export TEST_DB_PASSWORD=test
export TEST_REDIS_HOST=localhost
export TEST_REDIS_PORT=6379
```

**检查环境**：
```java
TestEnvironmentChecker checker = new TestEnvironmentChecker();
checker.checkFromEnvironment();
if (checker.hasErrors()) {
    checker.printReport();
}
```

**记忆口诀**：集成测试手动启环境，Testcontainers 不使用。

---

### 规则 TOOL-006：PIT 变异测试是 Phase 5 必跑门禁

**执行方式**：
```bash
# 方式一：使用脚本（推荐）
./scripts/run-pitest.sh cartisan-core

# 方式二：直接调用 Maven
mvn org.pitest:pitest-maven:mutationCoverage -pl cartisan-core
```

**验收标准**：
- 变异杀死率 ≥ 70%
- 报告位置：`cartisan-core/target/pit-reports/index.html`
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
find ~/.m2/repository -name "sa-token-core*.jar" | head -1 | xargs jar tf | grep -i "StpUtil"
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
find ~/.m2/repository -name "sa-token-core*.jar" | head -1 | xargs jar tf | grep -i "Session"
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
mvn javadoc:javadoc -pl module
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

**场景**：在 `DomainEntity<T, ID>` 接口的默认方法中调用 `other.getId()` 时编译错误。

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

### PIT-009 (2026-03-13)：测试模块需要 spring-boot-starter-data-redis

**场景**：`IntegrationTestBase` 编译失败，提示 `StringRedisTemplate` 找不到符号。

**原因**：`StringRedisTemplate` 在 `spring-boot-starter-data-redis` 中，需要显式依赖。

**正确做法**：
```xml
<!-- cartisan-test/pom.xml -->
<dependencies>
    <!-- ... 其他依赖 -->

    <!-- Redis 支持（测试需要 StringRedisTemplate） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
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
1. Maven 依赖管理中，依赖对使用者可见
2. 但需要显式声明依赖才能让本模块代码编译可用
3. Spring Test 相关类需要在 main 代码中使用（`ApiTestAssertions`）

**正确做法**：
```xml
<!-- cartisan-test/pom.xml -->
<dependencies>
    <!-- compile scope 确保本模块 main 代码可用，且暴露给使用者 -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-test</artifactId>
    </dependency>
</dependencies>
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
find ~/.m2/repository -name "spring-test-*.jar" | head -1

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

### 规则 DATA-004：软删除读过滤由框架自动注册（继承/接口实现均生效）

**问题**：`@SQLRestriction` 声明在 `@MappedSuperclass`（如 `AuditableSoftDeletable`）上时，Hibernate **不会**把它继承到具体实体子类——子类的所有读路径都会返回已软删记录。这是 Hibernate 的已知行为（restriction 不跨 `@MappedSuperclass` 继承）。

**解决方案（框架已内置）**：`cartisan-data-jpa` 通过 Hibernate `AdditionalMappingContributor`（`SoftDeleteRestrictionContributor`，经 Java ServiceLoader SPI 自动发现）在元模型构建期为「实现 `SoftDeletable` 且未显式声明 restriction」的实体统一注册等价于 `@SQLRestriction("deleted = false")` 的过滤。

- 继承 `AuditableSoftDeletable`（自身不声明任何注解）→ 自动过滤 ✓
- 直接实现 `SoftDeletable` 接口（不经 `AuditableSoftDeletable`）→ 自动过滤 ✓
- 实体自身显式声明 `@SQLRestriction` → 按其表达式过滤，框架**不覆盖、不叠加**

> 历史上的"子类重复声明 `@SQLRestriction`"变通方案现已无需使用（重复声明不会出错，Contributor 会跳过已声明的实体）。

**约定**：实现 `SoftDeletable` 的实体须将软删标记映射为列 `deleted`。该约定由 `SoftDeletableRestrictionContributor` 在元模型构建期**启动期 fail-fast** 校验——缺失 `deleted` 持久化列时直接抛 `MappingException`（错误消息指明违约的实体类），应用启动即失败，而非运行期才因列不存在抛 SQL 异常。继承 `AuditableSoftDeletable` 即自动满足此约定。

**记忆口诀**：实现 SoftDeletable 即自动读过滤，无需子类重复声明；缺 `deleted` 列则启动期失败。

---

### 规则 DATA-005：@SQLRestriction 作用于 find/派生查询/JPQL，但不作用于原生 SQL 与批量 DML

**澄清**（更正早期错误结论）：`@SQLRestriction` 是 SQL 级片段，Hibernate 在生成 SQL 时追加，因此对**所有走 Hibernate SQL 生成的读路径**都生效——包括 `findById`/`findAll`/派生查询（方法名查询）/Specification/**显式 JPQL/HQL**。早期"JPQL 不受影响"的结论是错的（已被 `SoftDeleteRestrictionSemanticsTest` 覆盖验证）。

**不受作用的路径**：

- **原生 SQL 查询**（`nativeQuery = true`）：直接执行原始 SQL，Hibernate 不追加 restriction。
- **批量 UPDATE/DELETE**（`@Query` 的 `UPDATE`/`DELETE`，或 `BaseRepositoryImpl.deleteAll()` 的批量更新）：不走实体加载，restriction 不作用。

**正确做法**：
```java
// ✅ JPQL 查询自动应用 deleted = false（无需手动加条件）
@Query("SELECT e FROM Product e WHERE e.name = :name")
List<Product> findByName(@Param("name") String name);

// ⚠️ 原生 SQL 不会自动过滤，需手动加条件
@Query(value = "SELECT * FROM product WHERE deleted = false AND name = :name", nativeQuery = true)
List<Product> findActiveByNameNative(@Param("name") String name);
```

**记忆口诀**：JPQL 自动过滤，原生 SQL 与批量 DML 需手动。

---

### 规则 DATA-006：自动软删除通过 instanceof 判断类型

**问题**：`BaseRepositoryImpl` 需要判断实体是否支持软删除。

**正确做法**：
```java
// ✅ 使用 instanceof 模式匹配
@Override
public void delete(T entity) {
    if (entity instanceof SoftDeletable softDeletable) {
        softDeletable.markAsDeleted();
        save(entity);  // 复用 save() 的事件发布逻辑
    } else {
        super.delete(entity);  // 非软删除实体，物理删除
    }
}
```

**行为**：
- 软删除实体：调用 `markAsDeleted()` + `save()`（复用事件发布）
- 非软删除实体：调用 `super.delete()` 物理删除
- `deleteById()` 先 `findById()`，再委托给 `delete()`
- `deleteAll(Iterable)` 分组处理，软删除 UPDATE，其他 DELETE

**记忆口诀**：软删除 instanceof 判断，markAsDeleted + save；物理删除 super.delete。

---

### 规则 DATA-007：枚举持久化使用内部 JpaConverter

**问题**：枚举默认使用 `ordinal()` 存储到数据库，增删枚举值会导致已有数据错乱。

**正确做法**：
```java
// ✅ 在枚举内声明内部 JpaConverter 类
public enum UserStatus implements BaseEnum<UserStatus> {
    ACTIVE(1, "激活"),
    INACTIVE(0, "未激活");

    private final Integer code;
    private final String name;

    UserStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }
    @Override
    public String getName() { return name; }

    // JPA Converter - 3 行代码解决持久化
    // 注意：类名使用 JpaConverter 而非 Converter，避免与 @Converter 注解冲突
    @Converter(autoApply = true)
    public static class JpaConverter extends BaseEnumConverter<UserStatus> {
        public JpaConverter() {
            super(UserStatus.class);
        }
    }
}

// ✅ 实体类无需任何注解
@Entity
public class User {
    private UserStatus status;  // 自动应用 UserStatus.JpaConverter
}
```

**错误做法**：
```java
// ❌ 为每个枚举类型手动创建独立 Converter 文件
@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, Integer> {
    // 大量重复代码...
}

// ❌ 使用 Converter 作为内部类名（与 @Converter 注解冲突）
public enum UserStatus implements BaseEnum<UserStatus> {
    // ...
    @Converter(autoApply = true)
    public static class Converter extends BaseEnumConverter<UserStatus> {  // 编译错误！
        // ...
    }
}
```

**记忆口诀**：枚举持久化用内部 JpaConverter，实体字段零注解。

---

### 规则 DATA-008：枚举 code 值必须唯一且稳定

**问题**：使用枚举 `ordinal()` 作为存储值，增删枚举值会导致已有数据错乱。

**正确做法**：
```java
// ✅ 使用稳定的 code 值
public enum UserStatus implements BaseEnum<UserStatus> {
    ACTIVE(1, "激活"),
    INACTIVE(0, "未激活"),
    PENDING(2, "待审核");  // 新增枚举不影响已有数据

    private final Integer code;
    private final String name;
}
```

**错误做法**：
```java
// ❌ 使用 ordinal()，顺序变化导致数据错乱
public enum UserStatus {
    INACTIVE,  // ordinal 0
    ACTIVE,    // ordinal 1
    PENDING    // ordinal 2 - 如果在中间插入，后续值都变化
}
```

**记忆口诀**：枚举存储用 code，不用 ordinal。

---

### 规则 DATA-009：BaseEnum Jackson 反序列化使用 ContextualDeserializer

**问题**：Jackson 反序列化时无法自动识别目标枚举类型。

**解决方案**：
- `BaseEnumDeserializer` 实现 `ContextualDeserializer` 接口
- 通过 `createContextual()` 方法获取目标字段的枚举类型
- 使用 `BaseEnum.parseByCode()` 查找对应枚举

**记忆口诀**：BaseEnum 反序列化用 ContextualDeserializer 获取目标类型。

---

### 规则 DATA-010：BaseEnum 参数绑定只支持 Integer code

**问题**：在 `@RequestParam`、`@PathVariable` 中使用枚举时，传递 name 而非 code 导致转换失败。

**正确做法**：
```java
// ✅ 使用 Integer code
@GetMapping("/users")
public List<UserDTO> getUsers(@RequestParam UserStatus status) {
    // 请求: GET /users?status=1  → status = UserStatus.ACTIVE
}

// ✅ 可选参数支持 null
@GetMapping("/users")
public List<UserDTO> getUsers(@RequestParam(required = false) UserStatus status) {
    // 请求: GET /users  → status = null
}
```

**错误做法**：
```java
// ❌ 不要传递 name 格式
// 请求: GET /users?status=ACTIVE  → 400 Bad Request
// 错误: "Enum value must be Integer code, not string: ACTIVE"
```

**异常处理**：
- **无效 code**（如 `?status=999`）：返回 400 Bad Request，错误信息 `"Invalid enum code: 999 for UserStatus"`
- **非数字字符串**（如 `?status=ACTIVE`）：返回 400 Bad Request，错误信息 `"Enum value must be Integer code, not string: ACTIVE"`
- **null/空字符串**：返回 null，由 `@NotNull` 等业务校验处理

**记忆口诀**：BaseEnum 参数绑定只认 code，name 格式不支持。

**原理**：`BaseEnumConverter` 实现了 Spring MVC 的 `ConverterFactory<String, BaseEnum<?>>`，通过 `CartisanWebAutoConfiguration` 自动注册，零配置生效。

---

### 规则 DATA-011

**问题**：Jackson 反序列化时无法自动识别目标枚举类型。

**解决方案**：
- `BaseEnumDeserializer` 实现 `ContextualDeserializer` 接口
- 通过 `createContextual()` 方法获取目标字段的枚举类型
- 使用 `BaseEnum.parseByCode()` 查找对应枚举

**记忆口诀**：BaseEnum 反序列化用 ContextualDeserializer 获取目标类型。

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
```xml
<!-- cartisan-security/pom.xml -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <argLine>--enable-preview</argLine>
            </configuration>
        </plugin>
    </plugins>
</build>
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

### 规则 JV-003：业务项目使用 cartisan-security 需启用预览特性

**问题**：业务项目引入 `cartisan-security` 依赖后，编译时使用 `TenantContext` 等类报错。

**错误表现**：
```
[ERROR] /path/to/OrderService.java:[3,38] 找不到符号
  符号:   类 ScopedValue
  位置: 类 com.cartisan.security.context.TenantContext

或者

错误: TenantContext 是预览 API，默认情况下处于禁用状态。
```

**原因**：编译时依赖传递
```
业务代码编译 → 引用 TenantContext → TenantContext 使用 ScopedValue
             ↓
        需要 --enable-preview
```

**正确做法**：在业务项目 `pom.xml` 中配置

```xml
<build>
    <plugins>
        <!-- 编译时启用预览特性 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>21</source>
                <target>21</target>
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
            </configuration>
        </plugin>

        <!-- 测试运行时启用预览特性 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <argLine>--enable-preview --add-opens java.base/java.lang=ALL-UNNAMED</argLine>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**哪些模块需要此配置**：
- ✅ `cartisan-security`：需要（使用 ScopedValue 实现多租户）
- ❌ 其他 cartisan-boot 模块：不需要

**替代方案**：如果不想启用预览特性，可以不使用 `cartisan-security`，自行实现简单的 ThreadLocal 方式租户上下文。

**记忆口诀**：用 cartisan-security 记得开预览，编译测试都要加。

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

---

## Web 基础设施

### 规则 WEB-001：@PreventResubmit 需要 Redis 环境

**问题**：`@PreventResubmit` 基于 Redis 分布式锁实现，无 Redis 时不生效。

**正确做法**：
- 确保 Redis 可用
- `ResubmitAspect` 条件装配：`@ConditionalOnBean(ResubmitLock.class)`
- `ResubmitLock` 条件装配：`@ConditionalOnClass(PreventResubmit.class)`

**记忆口诀**：防重提交需 Redis，无锁不生效。

---

### 规则 WEB-003：TreeNodeBuilder 需要 ID 类型转换

**问题**：TreeNode 的 ID 泛型可能与数据库类型不一致。

**正确做法**：
```java
// 使用 Function 转换 ID 类型
TreeNodeBuilder.build(
    nodes,
    id -> String.valueOf(id),      // Long → String
    parentId -> String.valueOf(parentId),
    "0"                             // 根节点父 ID
);
```

**记忆口诀**：TreeNode ID 类型可能不一致，用 Function 转换。

---

### 规则 WEB-004：@Condition 注解 BigDecimal 类型限制

**问题**：`ConditionSpecifications` 对 BigDecimal 使用 `path.as(Comparable.class)` 导致 Hibernate 无法推断类型。

**正确做法**：
```java
// ❌ 不推荐：BigDecimal 的大小比较可能有问题
@Condition(propName = "price", type = ConditionType.GREATER_EQUAL)
BigDecimal minPrice;

// ✅ 推荐：使用 Integer 或 Long
@Condition(propName = "stock", type = ConditionType.GREATER_EQUAL)
Integer minStock;
```

**记忆口诀**：@Condition 查询用 Integer/Long，BigDecimal 类型推断有问题。

---

### 规则 WEB-005：RequestLogFilter 自动排除特定路径

**问题**：swagger、druid、actuator 等路径的日志会大量输出，干扰问题排查。

**正确做法**：
```java
private static final Set<String> EXCLUDE_PATHS = Set.of(
    "/swagger-ui",
    "/v3/api-docs",
    "/swagger-resources",
    "/druid",
    "/actuator"
);
```

**记忆口诀**：日志排除工具路径，减少干扰。

---

### 规则 WEB-006：MDC requestId 自动清理

**问题**：异步线程需要手动传递 MDC 值，否则日志中丢失 requestId。

**正确做法**：
```java
// 同步场景：RequestContextFilter 自动清理，无需手动处理
log.info("requestId 自动存在于 MDC");

// 异步场景：需要手动传递
String requestId = MDC.get("requestId");
CompletableFuture.runAsync(() -> {
    MDC.put("requestId", requestId);  // 手动传递
    log.info("异步任务也有 requestId");
    MDC.clear();  // 手动清理
});
```

**记忆口诀**：同步自动清理，异步手动传递 MDC。

---

### 规则 WEB-007：DomainMapper 默认方法返回空集合

**问题**：`convertList` 和 `convertSet` 在输入为 null 或空时返回空集合，不是 null。

**正确做法**：
```java
List<User> users = userMapper.convertList(null);  // 返回空列表，不是 null
users.isEmpty();  // true，不会 NPE
```

**记忆口诀**：DomainMapper 批量转换返回空集合，非 null。

---

### 规则 WEB-008：RedisKey 过期时间单位是秒

**问题**：误以为 RedisKey.of() 的过期时间单位是毫秒。

**正确做法**：
```java
// ✅ 正确：单位是秒
private static final RedisKey KEY = RedisKey.of("user:cache", 3600);  // 1 小时

// ❌ 错误：误以为是毫秒
private static final RedisKey KEY = RedisKey.of("user:cache", 3600_000);  // 实际是 1000 小时！
```

**记忆口诀**：RedisKey 过期时间用秒，不是毫秒。

---

## Maven / 构建配置

### 规则 BUILD-001：BOM 模块使用 pom packaging 类型

**问题**：Maven BOM 需要使用 `pom` 打包类型，而不是 `jar`。

**正确做法**：
```xml
<!-- cartisan-dependencies/pom.xml -->
<project>
    <packaging>pom</packaging>
    ...
</project>
```

**记忆口诀**：BOM 模块用 pom 打包类型。

**记忆口诀**：subprojects 中用 project.dependencies.apply，不用 dependencies.add()。

---

### 规则 BUILD-003：非 JPA 模块的测试需要排除数据源自动配置

**问题**：`cartisan-security`、`cartisan-web` 等模块没有数据源依赖，但 `cartisan-dependencies` BOM 引入了 Druid，导致测试时 Druid 自动配置尝试创建数据源而失败。

**错误现象**：
```
ClassNotFoundException: org.springframework.jdbc.core.ConnectionCallback
```

**正确做法**：
```properties
# cartisan-security/src/test/resources/application.properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
  com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure
```

**记忆口诀**：无数据源模块测试需排除 Druid 自动配置。

---

### 规则 BUILD-004：AutoConfiguration 不应依赖 @ComponentScan

**问题**：AutoConfiguration 类中的 `@Bean` 方法依赖 `@Component` 注解的类，导致子模块测试时 Bean 找不到。

**错误表现**：
```
Unsatisfied dependency expressed through method 'enumScanner' parameter 0:
No qualifying bean of type 'com.cartisan.web.enums.EnumRegistry' available
```

**错误代码**：
```java
// ❌ EnumRegistry 使用 @Component，依赖 @ComponentScan
@Component
public class EnumRegistry {
    // ...
}

@AutoConfiguration
public class CartisanWebAutoConfiguration {
    @Bean
    public EnumScanner enumScanner(EnumRegistry enumRegistry) {
        // ❌ 子模块的 @ComponentScan 不扫描 cartisan.web 包
        return new EnumScanner(enumRegistry);
    }
}
```

**正确做法**：
```java
// ✅ 移除 @Component，改为 @Bean 声明
public class EnumRegistry {
    // ...
}

@AutoConfiguration
public class CartisanWebAutoConfiguration {
    @Bean
    public EnumRegistry enumRegistry() {
        return new EnumRegistry();  // ✅ 自动配置独立声明所有 Bean
    }

    @Bean
    public EnumScanner enumScanner(EnumRegistry enumRegistry) {
        return new EnumScanner(enumRegistry);
    }
}
```

**原因**：
- AutoConfiguration 应"引入即用"，不依赖 `@ComponentScan`
- 子模块的测试类有自己的 `@ComponentScan`，不会扫描框架模块的包
- 所有依赖的 Bean 都应在 AutoConfiguration 中用 `@Bean` 显式声明

**记忆口诀**：AutoConfiguration 自给自足，不依赖 @ComponentScan。

---

### 规则 BUILD-005：测试依赖统一使用 spring-boot-starter-test

**问题**：各模块重复声明 JUnit、AssertJ、Mockito 依赖，导致版本冲突和维护成本高。

**错误做法**：
```xml
<!-- ❌ 重复声明，容易遗漏 logback -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
</dependency>
```

**正确做法**：
```xml
<!-- ✅ 统一使用 spring-boot-starter-test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**优点**：
- 自动包含 JUnit、AssertJ、Mockito、Logback
- 版本由 Spring Boot 统一管理，无冲突
- 消除 SLF4J 警告（logback-classic 已包含）

**记忆口诀**：测试依赖一个 starter 搞定。

---

### 规则 BUILD-006：Mockito 在 JDK 21+ 需配置为 Java Agent

**问题**：Mockito 动态加载 Java Agent 产生警告，未来 JDK 版本将禁止。

**警告信息**：
```
Mockito is currently self-attaching to enable the inline-mock-maker.
WARNING: A Java agent has been loaded dynamically
WARNING: Dynamic loading of agents will be disallowed in a future release
```

**正确做法**：
```xml
<!-- pom.xml（根 POM 统一配置） -->
<properties>
    <byte-buddy-agent.version>1.15.10</byte-buddy-agent.version>
</properties>

<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>
                        --enable-preview
                        --add-opens java.base/java.lang=ALL-UNNAMED
                        -javaagent:${settings.localRepository}/net/bytebuddy/byte-buddy-agent/${byte-buddy-agent.version}/byte-buddy-agent-${byte-buddy-agent.version}.jar
                    </argLine>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

**原因**：
- Mockito 使用 inline-mock-maker 需要 Java Agent
- JDK 21+ 限制动态加载 Agent，需在启动时指定
- byte-buddy-agent 版本需与 Mockito 依赖一致

**记忆口诀**：Mockito Agent 提前配置，JDK 21+ 动态加载禁。

---

### 规则 BUILD-007：Maven 配置统一在根 POM 管理

**原则**：所有模块共享的配置应集中在根 POM 的 `pluginManagement` 中，子模块无需自定义。

**正确做法**：
```xml
<!-- ✅ 根 POM 统一配置 -->
<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <parameters>true</parameters>
                    <compilerArgs>
                        <arg>--enable-preview</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>--enable-preview ...</argLine>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

**子模块**：
```xml
<!-- ❌ 移除所有自定义配置 -->
<build>
    <plugins>
        <!-- ✅ 只需声明需要特殊处理的插件 -->
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <!-- 仅配置注解处理器路径 -->
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**优点**：
- 避免子模块配置冲突
- 统一版本管理
- 降低维护成本

**记忆口诀**：共享配置根 POM 管，子模块只配特殊项。

---

### 规则 ID-003：TSID 纯随机实现的碰撞概率计算

**问题**：22 位随机数在同一毫秒内生成大量 ID 时，碰撞数超出预期。

**理论计算**（生日悖论）：
```
碰撞数 ≈ n² / (2 × 空间大小)
       = 10000² / (2 × 4194304)
       ≈ 11.9 个
```

**正确断言**：
```java
// ✅ 允许 ≤0.3% 重复（30/10000），包含安全余量
int duplicateCount = 10000 - generatedIds.size();
assertThat(generatedIds).hasSizeGreaterThanOrEqualTo(9970);
assertThat(duplicateCount)
    .withFailMessage("Too many duplicates: %d out of 10000", duplicateCount)
    .isLessThanOrEqualTo(30);
```

**设计权衡**：
- 纯随机：无锁、高性能（>500万/秒），接受 ~0.12% 实际冲突率
- 计数器 + synchronized：保证唯一，但违反性能约束

**适用场景**：
- 极高频场景（如同一毫秒 >10000 个 ID）：考虑使用雪花算法
- 普通场景（<10000/毫秒）：纯随机实现足够

**记忆口诀**：TSID 碰撞算概率，预期 n²/2m，阈值放宽 0.3%。

---

## 服务间通信（cartisan-openapi）

### 规则 OPENAPI-001：ScopedValue 不可 rebind，RequestContext 写入必须在 Filter 层

**问题**：在 Spring MVC Interceptor 中尝试通过 `RequestContext.run(enriched, ...)` 写入 caller 信息，发现无法生效——`RequestContext.getCallerAppId()` 返回 null。

**原因**：ScopedValue 一旦绑定就不能在当前作用域重新绑定。RequestContextFilter 已经绑定了 RequestContext，后续 Interceptor 运行在同一作用域中，无法创建新绑定。

**错误做法**：
```java
// ❌ Interceptor 中无法 rebind RequestContext
public class SignatureVerificationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        RequestContext enriched = current.withCaller(appId, appName);
        // 以下代码不会生效——ScopedValue 不可 rebind
        RequestContext.CONTEXT = enriched;  // 编译错误！ScopedValue 是 final
        request.setAttribute("callerAppId", appId);  // 只能退而求其次
    }
}
```

**正确做法**：将 RequestContext 写入逻辑移到 Filter 层，使用 `ScopedValue.where().run()` 创建新绑定：
```java
// ✅ Filter 层可以用 RequestContext.run() 创建新绑定
public class SignatureVerificationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        // 验签成功后
        RequestContext enriched = current.withCaller(appId, appName);
        RequestContext.run(enriched, () -> chain.doFilter(request, response));
    }
}
```

**记忆口诀**：ScopedValue 不可 rebind，写 RequestContext 要在 Filter。

---

### 规则 OPENAPI-002：HTTP 客户端不检查状态码导致难以诊断的错误

**问题**：`OpenApiClient` 的 `post()` 和 `get()` 方法不检查 `response.statusCode()`，4xx/5xx 响应直接尝试 JSON 反序列化，导致难以理解的 Jackson 解析错误。

**错误表现**：
```
com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot deserialize value of type ...
```
（实际原因是服务端返回了 HTML 错误页面，不是 JSON）

**正确做法**：
```java
// ✅ 先检查状态码，再反序列化
HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
if (response.statusCode() >= 400) {
    throw new OpenApiClientException(response.statusCode(), response.body());
}
return objectMapper.readValue(response.body(), responseType);
```

**记忆口诀**：HTTP 客户端先查状态码，再反序列化。

---
