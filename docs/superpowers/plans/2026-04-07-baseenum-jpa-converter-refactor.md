# BaseEnum JPA Converter 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 BaseEnum JPA Converter，从失效的 @EnumConvert 扫描机制改为标准的内部 Converter 模式

**架构:** 将 UniversalEnumConverter 重构为抽象基类 BaseEnumConverter<T>，每个枚举内部声明 @Converter(autoApply = true) 的静态内部类，Hibernate 自动扫描应用

**技术栈:** Java 21, Spring Boot 3.4.0, Hibernate 6.x, JPA, Maven, JUnit 5, Testcontainers

---

## 文件结构映射

### 修改的文件

| 文件 | 变更类型 | 职责 |
|------|---------|------|
| `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/UniversalEnumConverter.java` | 重命名+重构 | 改为抽象基类 BaseEnumConverter |
| `cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java` | 文档更新 | 更新 JavaDoc 使用示例 |
| `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java` | 删除代码 | 删除 enumConverterScanner Bean |
| `docs/PITFALLS.md` | 文档更新 | 更新 DATA-007 规则 |

### 删除的文件

| 文件 | 原因 |
|------|------|
| `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/annotation/EnumConvert.java` | 不再需要 |
| `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/EnumConverterRegistrar.java` | 不再需要扫描注册 |
| `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/EnumConverterRegistrarTest.java` | 测试已删除的类 |
| `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/EnumIntegrationTest.java` | 伪集成测试 |

### 新增的文件

| 文件 | 职责 |
|------|------|
| `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/BaseEnumConverterTest.java` | 测试抽象基类 |
| `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/BaseEnumJpaIntegrationTest.java` | 真正的 JPA 集成测试 |

---

## Task 1: 重构 UniversalEnumConverter 为抽象基类

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/UniversalEnumConverter.java`
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/BaseEnumConverter.java`

- [ ] **Step 1: 先备份原 UniversalEnumConverter 的测试用例**

查看现有测试，记录需要保留的测试场景：
```bash
cat cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/UniversalEnumConverterTest.java
```

- [ ] **Step 2: 创建新的抽象基类 BaseEnumConverter**

创建文件 `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/BaseEnumConverter.java`:

```java
package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;
import jakarta.persistence.AttributeConverter;

/**
 * BaseEnum JPA 转换器抽象基类。
 * <p>
 * 每个实现 BaseEnum 的枚举应在内部声明一个静态内部类继承此类，
 * 并标注 {@link jakarta.persistence.Converter @Converter(autoApply = true)}。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public enum OrderStatus implements BaseEnum<OrderStatus> {
 *     PENDING(1, "待支付"),
 *     PAID(2, "已支付");
 *
 *     private final Integer code;
 *     private final String name;
 *
 *     OrderStatus(Integer code, String name) {
 *         this.code = code;
 *         this.name = name;
 *     }
 *
 *     @Override public Integer getCode() { return code; }
 *     @Override public String getName() { return name; }
 *
 *     @Converter(autoApply = true)
 *     public static class Converter extends BaseEnumConverter<OrderStatus> {
 *         public Converter() {
 *             super(OrderStatus.class);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @param <E> 枚举类型，必须实现 BaseEnum
 */
public abstract class BaseEnumConverter<E extends Enum<E> & BaseEnum<E>>
        implements AttributeConverter<E, Integer> {

    private final Class<E> enumType;

    /**
     * 创建转换器实例。
     *
     * @param enumType 枚举类型
     */
    protected BaseEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public Integer convertToDatabaseColumn(E attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public E convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return BaseEnum.requireByCode(enumType, dbData);
    }

    /**
     * 获取此转换器支持的枚举类型。
     *
     * @return 枚举类型
     */
    protected Class<E> getEnumType() {
        return enumType;
    }
}
```

- [ ] **Step 3: 删除旧的 UniversalEnumConverter.java**

```bash
rm cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/UniversalEnumConverter.java
```

- [ ] **Step 4: 提交变更**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/
git commit -m "refactor: rename UniversalEnumConverter to BaseEnumConverter

- Rename UniversalEnumConverter to BaseEnumConverter
- Make it abstract with protected constructor
- Add comprehensive JavaDoc with usage example
- Remove old UniversalEnumConverter.java

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 编写 BaseEnumConverter 单元测试

**Files:**
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/BaseEnumConverterTest.java`
- Delete: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/UniversalEnumConverterTest.java`

- [ ] **Step 1: 创建测试枚举**

创建文件 `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/TestOrderStatus.java`:

```java
package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;

enum TestOrderStatus implements BaseEnum<TestOrderStatus> {
    PENDING(1, "待处理"),
    COMPLETED(2, "已完成");

    private final Integer code;
    private final String name;

    TestOrderStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * JPA Converter for TestOrderStatus.
     */
    @jakarta.persistence.Converter(autoApply = true)
    public static class Converter extends BaseEnumConverter<TestOrderStatus> {
        public Converter() {
            super(TestOrderStatus.class);
        }
    }
}
```

- [ ] **Step 2: 编写测试类**

创建文件 `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/BaseEnumConverterTest.java`:

```java
package com.cartisan.data.jpa.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BaseEnumConverter 单元测试")
class BaseEnumConverterTest {

    @Test
    @DisplayName("应该将枚举转换为数据库整数值")
    void shouldConvertEnumToDatabaseColumn() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThat(converter.convertToDatabaseColumn(TestOrderStatus.PENDING))
            .isEqualTo(1);
        assertThat(converter.convertToDatabaseColumn(TestOrderStatus.COMPLETED))
            .isEqualTo(2);
    }

    @Test
    @DisplayName("应该将 null 枚举转换为 null")
    void shouldConvertNullEnumToNull() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThat(converter.convertToDatabaseColumn(null))
            .isNull();
    }

    @Test
    @DisplayName("应该将数据库整数值转换为枚举")
    void shouldConvertDatabaseColumnToEnum() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThat(converter.convertToEntityAttribute(1))
            .isEqualTo(TestOrderStatus.PENDING);
        assertThat(converter.convertToEntityAttribute(2))
            .isEqualTo(TestOrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("应该将 null 数据库值转换为 null")
    void shouldConvertNullDatabaseValueToNull() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThat(converter.convertToEntityAttribute(null))
            .isNull();
    }

    @Test
    @DisplayName("应该为无效 code 抛出异常")
    void shouldThrowException_forInvalidCode() {
        TestOrderStatus.Converter converter = new TestOrderStatus.Converter();

        assertThatThrownBy(() -> converter.convertToEntityAttribute(999))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown code: 999");
    }
}
```

- [ ] **Step 3: 运行测试验证通过**

```bash
cd cartisan-data-jpa
mvn test -Dtest=BaseEnumConverterTest -q
```

预期输出: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 4: 删除旧的测试文件**

```bash
rm cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/UniversalEnumConverterTest.java
rm cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/EnumConverterRegistrarTest.java
```

- [ ] **Step 5: 提交变更**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/
git commit -m "test: add BaseEnumConverter unit tests

- Add TestOrderStatus enum with internal Converter
- Add comprehensive unit tests for BaseEnumConverter
- Delete obsolete UniversalEnumConverterTest
- Delete obsolete EnumConverterRegistrarTest

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 删除 @EnumConvert 注解和相关类

**Files:**
- Delete: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/annotation/EnumConvert.java`
- Delete: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/EnumConverterRegistrar.java`
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java`

- [ ] **Step 1: 删除 @EnumConvert 注解**

```bash
rm cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/annotation/EnumConvert.java
rmdir cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/annotation/ 2>/dev/null || true
```

- [ ] **Step 2: 删除 EnumConverterRegistrar**

```bash
rm cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/EnumConverterRegistrar.java
```

- [ ] **Step 3: 从 CartisanDataJpaAutoConfiguration 删除 enumConverterScanner**

编辑文件 `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java`:

删除第 3 行的 import:
```java
import com.cartisan.data.jpa.converter.EnumConverterRegistrar;
```

删除第 99-112 行的 `enumConverterScanner` Bean:
```java
    /**
     * 扫描枚举转换器类型。
     * ...
     */
    @Bean
    public ApplicationListener<ContextRefreshedEvent> enumConverterScanner(EntityManagerFactory entityManagerFactory) {
        return event -> {
            EnumConverterRegistrar registrar = new EnumConverterRegistrar();
            Set<Class<?>> enumTypes = registrar.scanEnumTypes(entityManagerFactory);

            if (!enumTypes.isEmpty()) {
                log.info("Discovered {} enum type(s) with @EnumConvert annotation", enumTypes.size());
                for (Class<?> enumType : enumTypes) {
                    log.debug("  - {}", enumType.getSimpleName());
                }
            }
        };
    }
```

删除第 11 行不再使用的 import:
```java
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
```

- [ ] **Step 4: 验证编译通过**

```bash
cd cartisan-data-jpa
mvn compile -q
```

预期输出: 无错误

- [ ] **Step 5: 提交变更**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/
git commit -m "refactor: remove @EnumConvert annotation and scanning logic

- Delete @EnumConvert annotation
- Delete EnumConverterRegistrar class
- Remove enumConverterScanner bean from CartisanDataJpaAutoConfiguration
- Clean up unused imports

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 编写真正的 JPA 集成测试

**Files:**
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/BaseEnumJpaIntegrationTest.java`
- Delete: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/EnumIntegrationTest.java`

- [ ] **Step 1: 创建测试用枚举**

创建文件 `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/TestAdminUserStatus.java`:

```java
package com.cartisan.data.jpa.integration;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.data.jpa.converter.BaseEnumConverter;
import jakarta.persistence.Converter;

/**
 * 测试用管理员状态枚举。
 */
public enum TestAdminUserStatus implements BaseEnum<TestAdminUserStatus> {
    ACTIVE(1, "激活"),
    DISABLED(0, "禁用");

    private final Integer code;
    private final String name;

    TestAdminUserStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Converter(autoApply = true)
    public static class Converter extends BaseEnumConverter<TestAdminUserStatus> {
        public Converter() {
            super(TestAdminUserStatus.class);
        }
    }
}
```

- [ ] **Step 2: 创建测试实体**

创建文件 `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/TestAdminUser.java`:

```java
package com.cartisan.data.jpa.integration;

import jakarta.persistence.*;

/**
 * 测试用管理员实体。
 */
@Entity
@Table(name = "test_admin_users")
public class TestAdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "status")
    // 注意：无需 @Convert 或 @Enumerated 注解
    private TestAdminUserStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public TestAdminUserStatus getStatus() {
        return status;
    }

    public void setStatus(TestAdminUserStatus status) {
        this.status = status;
    }
}
```

- [ ] **Step 3: 创建 Repository**

创建文件 `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/TestAdminUserRepository.java`:

```java
package com.cartisan.data.jpa.integration;

import com.cartisan.data.jpa.repository.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestAdminUserRepository extends BaseRepository<TestAdminUser, Long> {
}
```

- [ ] **Step 4: 编写集成测试**

创建文件 `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/BaseEnumJpaIntegrationTest.java`:

```java
package com.cartisan.data.jpa.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("BaseEnum JPA 集成测试")
class BaseEnumJpaIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TestAdminUserRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("应该保存并正确读取枚举的 code 值")
    void shouldSaveAndLoadEnumWithCorrectCode() {
        // Given: 创建一个管理员，状态为 ACTIVE (code=1)
        TestAdminUser user = new TestAdminUser();
        user.setUsername("admin");
        user.setStatus(TestAdminUserStatus.ACTIVE);

        // When: 保存到数据库
        repository.save(user);
        TestAdminUser saved = repository.findById(user.getId()).orElseThrow();

        // Then: 读取的状态应该是 ACTIVE
        assertThat(saved.getStatus()).isEqualTo(TestAdminUserStatus.ACTIVE);

        // 验证：数据库中存储的是 code 值 1，不是 ordinal 0
        // 通过原生查询验证
        Integer statusInDb = (Integer) entityManager.getEntityManager()
            .createNativeQuery("SELECT status FROM test_admin_users WHERE id = ?")
            .setParameter(1, user.getId())
            .getSingleResult();

        assertThat(statusInDb).isEqualTo(1); // ACTIVE.getCode()
    }

    @Test
    @DisplayName("应该正确处理 DISABLED 状态 (code=0, ordinal=1)")
    void shouldHandleDisabledStatusCorrectly() {
        // Given: DISABLED(0, "禁用")
        // ordinal = 1（第二个声明）
        // code = 0

        TestAdminUser user = new TestAdminUser();
        user.setUsername("admin2");
        user.setStatus(TestAdminUserStatus.DISABLED);

        // When
        repository.save(user);
        TestAdminUser saved = repository.findById(user.getId()).orElseThrow();

        // Then
        assertThat(saved.getStatus()).isEqualTo(TestAdminUserStatus.DISABLED);

        // 验证数据库存储的是 code=0，不是 ordinal=1
        Integer statusInDb = (Integer) entityManager.getEntityManager()
            .createNativeQuery("SELECT status FROM test_admin_users WHERE id = ?")
            .setParameter(1, user.getId())
            .getSingleResult();

        assertThat(statusInDb).isEqualTo(0); // DISABLED.getCode()
    }

    @Test
    @DisplayName("应该正确处理 null 状态")
    void shouldHandleNullStatus() {
        // Given
        TestAdminUser user = new TestAdminUser();
        user.setUsername("admin3");
        user.setStatus(null);

        // When
        repository.save(user);
        TestAdminUser saved = repository.findById(user.getId()).orElseThrow();

        // Then
        assertThat(saved.getStatus()).isNull();
    }

    @Test
    @DisplayName("应该能查询指定状态的用户")
    void shouldFindByStatus() {
        // Given
        TestAdminUser user1 = new TestAdminUser();
        user1.setUsername("admin1");
        user1.setStatus(TestAdminUserStatus.ACTIVE);

        TestAdminUser user2 = new TestAdminUser();
        user2.setUsername("admin2");
        user2.setStatus(TestAdminUserStatus.DISABLED);

        TestAdminUser user3 = new TestAdminUser();
        user3.setUsername("admin3");
        user3.setStatus(TestAdminUserStatus.ACTIVE);

        repository.save(user1);
        repository.save(user2);
        repository.save(user3);

        // When
        var activeUsers = repository.findAll().stream()
            .filter(u -> u.getStatus() == TestAdminUserStatus.ACTIVE)
            .toList();

        // Then
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers)
            .allMatch(u -> u.getStatus() == TestAdminUserStatus.ACTIVE);
    }
}
```

- [ ] **Step 5: 删除伪集成测试**

```bash
rm cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/EnumIntegrationTest.java
```

- [ ] **Step 6: 运行集成测试**

```bash
cd cartisan-data-jpa
mvn test -Dtest=BaseEnumJpaIntegrationTest -q
```

预期输出: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 7: 提交变更**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/
git commit -m "test: add real JPA integration test for BaseEnum

- Add TestAdminUserStatus enum with internal Converter
- Add TestAdminUser entity (no @Convert annotation)
- Add TestAdminUserRepository
- Add comprehensive integration tests:
  - Verify database stores code value, not ordinal
  - Test DISABLED (code=0, ordinal=1) edge case
  - Test null status handling
  - Test querying by enum status
- Delete pseudo-integration test EnumIntegrationTest

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 更新 BaseEnum 文档

**Files:**
- Modify: `cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java`

- [ ] **Step 1: 更新 BaseEnum JavaDoc**

编辑文件 `cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java`:

在类级别 JavaDoc 中更新 JPA 使用说明：

```java
/**
 * 业务枚举基类。
 * <p>
 * 业务枚举实现此接口后，框架自动完成：
 * <ul>
 *   <li>JPA：int ↔ enum 转换（需在枚举内声明 Converter 类）</li>
 *   <li>Jackson：enum ↔ int 序列化</li>
 *   <li>Spring MVC：String → enum 参数绑定（@RequestParam、@PathVariable）</li>
 * </ul>
 *
 * <h3>JPA 持久化示例</h3>
 * <pre>{@code
 * public enum UserStatus implements BaseEnum<UserStatus> {
 *     ACTIVE(1, "激活"),
 *     INACTIVE(0, "未激活");
 *
 *     private final Integer code;
 *     private final String name;
 *
 *     UserStatus(Integer code, String name) {
 *         this.code = code;
 *         this.name = name;
 *     }
 *
 *     @Override
 *     public Integer getCode() { return code; }
 *
 *     @Override
 *     public String getName() { return name; }
 *
 *     // JPA Converter - 必须声明为 public static class
 *     @jakarta.persistence.Converter(autoApply = true)
 *     public static class Converter
 *             extends com.cartisan.data.jpa.converter.BaseEnumConverter<UserStatus> {
 *         public Converter() {
 *             super(UserStatus.class);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>实体类使用时无需任何注解：</p>
 * <pre>{@code
 * @Entity
 * public class User {
 *     @Id
 *     private Long id;
 *
 *     // 无需 @Convert 或 @Enumerated 注解
 *     private UserStatus status;
 * }
 * }</pre>
 *
 * @param <T> 枚举类型
 */
public interface BaseEnum<T extends Enum<T> & BaseEnum<T>> {
    // ...
}
```

- [ ] **Step 2: 提交变更**

```bash
git add cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java
git commit -m "docs: update BaseEnum JavaDoc with JPA usage example

- Add comprehensive JPA persistence example
- Show internal Converter class declaration
- Emphasize @Converter(autoApply = true)
- Clarify no annotation needed on entity fields

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 6: 更新 PITFALLS.md 编码规范

**Files:**
- Modify: `docs/PITFALLS.md`

- [ ] **Step 1: 找到 DATA-007 规则**

```bash
grep -n "DATA-007" docs/PITFALLS.md
```

- [ ] **Step 2: 替换 DATA-007 规则内容**

编辑 `docs/PITFALLS.md`，找到 DATA-007 规则（约 1430-1457 行），替换为：

```markdown
### 规则 DATA-007：枚举持久化使用内部 Converter

**问题**：枚举默认使用 `ordinal()` 存储到数据库，增删枚举值会导致已有数据错乱。

**正确做法**：
```java
// ✅ 在枚举内声明内部 Converter 类
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
    @Converter(autoApply = true)
    public static class Converter extends BaseEnumConverter<UserStatus> {
        public Converter() {
            super(UserStatus.class);
        }
    }
}

// ✅ 实体类无需任何注解
@Entity
public class User {
    private UserStatus status;  // 自动应用 UserStatus.Converter
}
```

**错误做法**：
```java
// ❌ 为每个枚举类型手动创建独立 Converter 文件
@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, Integer> {
    // 大量重复代码...
}
```

**记忆口诀**：枚举持久化用内部 Converter，实体字段零注解。
```

- [ ] **Step 3: 提交变更**

```bash
git add docs/PITFALLS.md
git commit -m "docs: update DATA-007 rule in PITFALLS.md

- Replace @EnumConvert approach with internal Converter pattern
- Add code example showing enum with internal Converter class
- Emphasize zero annotation on entity fields
- Update memory aid

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 7: 运行完整测试套件

**Files:**
- None (validation)

- [x] **Step 1: 运行 cartisan-data-jpa 所有测试**

```bash
cd cartisan-data-jpa
mvn test
```

预期输出: `BUILD SUCCESS`

实际结果: ✅ BUILD SUCCESS - Tests run: 135, Failures: 0, Errors: 0, Skipped: 0

- [x] **Step 2: 运行 cartisan-core 所有测试**

```bash
cd cartisan-core
mvn test
```

预期输出: `BUILD SUCCESS`

实际结果: ✅ BUILD SUCCESS - Tests run: 105, Failures: 0, Errors: 0, Skipped: 0

- [x] **Step 3: 验证项目整体编译**

```bash
cd /Users/zhangcolin/workspace/cartisan-boot
mvn clean compile -DskipTests
```

预期输出: `BUILD SUCCESS`

实际结果: ✅ BUILD SUCCESS - All 10 modules compiled successfully

- [x] **Step 4: 提交验证结果**

```bash
git add docs/superpowers/plans/2026-04-07-baseenum-jpa-converter-refactor.md
git commit -m "docs: mark implementation plan as completed

All tasks completed:
- BaseEnumConverter abstract class created
- Unit tests passing
- Integration tests passing
- Documentation updated
- Full test suite passing

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## 验收标准

### 功能验收

- [x] `BaseEnumConverter` 抽象基类正常工作
- [x] 枚举内部 Converter 被 Hibernate 自动扫描应用
- [x] 数据库存储 code 值，而非 ordinal
- [x] 正确处理 code=0 的边缘情况
- [x] 正确处理 null 值

### 测试验收

- [x] 单元测试覆盖所有转换场景
- [x] 集成测试验证 JPA 持久化
- [x] 所有测试通过（mvn test）
- [x] 删除了伪集成测试

### 文档验收

- [x] `BaseEnum` JavaDoc 更新
- [x] `PITFALLS.md` DATA-007 规则更新
- [x] 设计文档完整
- [x] 实施计划完整

### 代码质量

- [x] 无 @EnumConvert 注解残留
- [x] 无 EnumConverterRegistrar 残留
- [x] 无扫描逻辑残留
- [x] 代码符合项目规范

---

## 相关文档

- [设计文档](../specs/2026-04-07-baseenum-jpa-converter-refactor-design.md)
- [PITFALLS.md](../../../PITFALLS.md)
- [BaseEnum.java](../../../cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java)

---

## 附录：完整的迁移示例

### 旧代码（不工作）

```java
// ❌ 旧方案（已删除）
@Entity
public class Order {
    @EnumConvert(OrderStatus.class)
    private OrderStatus status;
}

public enum OrderStatus implements BaseEnum<OrderStatus> {
    PENDING(1, "待支付"),
    PAID(2, "已支付");
    // ...
}
```

### 新代码（正常工作）

```java
// ✅ 新方案
@Entity
public class Order {
    // 无需注解！
    private OrderStatus status;
}

public enum OrderStatus implements BaseEnum<OrderStatus> {
    PENDING(1, "待支付"),
    PAID(2, "已支付");

    private final Integer code;
    private final String name;

    OrderStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }
    @Override
    public String getName() { return name; }

    @Converter(autoApply = true)
    public static class Converter extends BaseEnumConverter<OrderStatus> {
        public Converter() {
            super(OrderStatus.class);
        }
    }
}
```
