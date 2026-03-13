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
