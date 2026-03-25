# 枚举增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 业务枚举只需定义 code/name，框架自动完成 int ↔ enum 转换（数据库、前端交互）

**架构:**
- `cartisan-core.domain.BaseEnum`：基础枚举接口，提供 code/name 和静态解析方法
- `cartisan-data-jpa`：@EnumConvert 注解 + UniversalEnumConverter + 自动配置
- `cartisan-web`：Jackson 序列化/反序列化配置

**Tech Stack:** Java 21, Spring Boot 3.4, JPA, Jackson, MapStruct

---

## 文件结构

### 新增文件

| 模块 | 路径 | 职责 |
|------|------|------|
| cartisan-core | `domain/BaseEnum.java` | 基础枚举接口 |
| cartisan-data-jpa | `annotation/EnumConvert.java` | 标识需要转换的枚举字段 |
| cartisan-data-jpa | `converter/UniversalEnumConverter.java` | 通用 JPA 转换器 |
| cartisan-data-jpa | `converter/EnumConverterRegistrar.java` | Converter 注册逻辑 |
| cartisan-web | `config/BaseEnumSerializer.java` | Jackson 序列化器 |
| cartisan-web | `config/BaseEnumDeserializer.java` | Jackson 反序列化器 |

### 修改文件

| 模块 | 路径 | 修改内容 |
|------|------|----------|
| cartisan-data-jpa | `config/CartisanDataJpaAutoConfiguration.java` | 新增 Converter 注册 Bean |
| cartisan-web | `config/JacksonConfiguration.java` | 新增枚举序列化/反序列化配置 |

---

## Task 1: 创建 BaseEnum 接口

**Files:**
- Create: `cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java`
- Test: `cartisan-core/src/test/java/com/cartisan/core/domain/BaseEnumTest.java`

- [ ] **Step 1: 创建 BaseEnum 接口**

```java
package com.cartisan.core.domain;

/**
 * 基础枚举接口。
 * <p>
 * 业务枚举实现此接口后，框架自动完成：
 * <ul>
 *   <li>JPA：int ↔ enum 转换</li>
 *   <li>Jackson：enum ↔ int 序列化</li>
 * </ul>
 */
public interface BaseEnum<T extends Enum<T> & BaseEnum<T>> {

    /**
     * 获取编码值（存数据库、传前端）。
     */
    Integer getCode();

    /**
     * 获取名称（显示用）。
     */
    String getName();

    /**
     * 根据 code 解析枚举。
     *
     * @param cls  枚举类型
     * @param code 编码值
     * @return 枚举值，不存在返回 null
     */
    static <T extends Enum<T> & BaseEnum<T>> T parseByCode(Class<T> cls, Integer code) {
        if (code == null) {
            return null;
        }
        for (T t : cls.getEnumConstants()) {
            if (t.getCode().equals(code)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 根据 code 解析枚举，不存在抛异常。
     *
     * @param cls  枚举类型
     * @param code 编码值
     * @return 枚举值
     * @throws IllegalArgumentException code 无效
     */
    static <T extends Enum<T> & BaseEnum<T>> T requireByCode(Class<T> cls, Integer code) {
        T result = parseByCode(cls, code);
        if (result == null) {
            throw new IllegalArgumentException(
                "Unknown code: " + code + " for " + cls.getSimpleName());
        }
        return result;
    }
}
```

- [ ] **Step 2: 编写测试**

```java
package com.cartisan.core.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

// 测试用枚举
enum TestStatus implements BaseEnum<TestStatus> {
    ACTIVE(1, "启用"),
    DISABLED(0, "禁用");

    private final Integer code;
    private final String name;

    TestStatus(Integer code, String name) {
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
}

class BaseEnumTest {

    @Test
    void shouldParseByCode_whenCodeValid() {
        assertThat(BaseEnum.parseByCode(TestStatus.class, 1))
            .isEqualTo(TestStatus.ACTIVE);
    }

    @Test
    void shouldReturnNull_whenCodeInvalid() {
        assertThat(BaseEnum.parseByCode(TestStatus.class, 999))
            .isNull();
    }

    @Test
    void shouldReturnNull_whenCodeIsNull() {
        assertThat(BaseEnum.parseByCode(TestStatus.class, null))
            .isNull();
    }

    @Test
    void shouldRequireByCode_whenCodeValid() {
        assertThat(BaseEnum.requireByCode(TestStatus.class, 0))
            .isEqualTo(TestStatus.DISABLED);
    }

    @Test
    void shouldThrowException_whenRequireByCodeInvalid() {
        assertThatThrownBy(() -> BaseEnum.requireByCode(TestStatus.class, 999))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown code: 999");
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
cd /Users/zhangcolin/workspace/cartisan-boot
./gradlew :cartisan-core:test --tests BaseEnumTest
```

Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java \
        cartisan-core/src/test/java/com/cartisan/core/domain/BaseEnumTest.java
git commit -m "feat(core): add BaseEnum interface

- Add getCode() / getName() methods
- Add static parseByCode() / requireByCode() methods

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: 创建 @EnumConvert 注解

**Files:**
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/annotation/EnumConvert.java`
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/annotation/package-info.java`

- [ ] **Step 1: 创建 annotation 包和注解**

```java
package com.cartisan.data.jpa.annotation;

import com.cartisan.core.domain.BaseEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识需要枚举转换的字段。
 * <p>
 * 配合 {@link com.cartisan.data.jpa.converter.UniversalEnumConverter} 使用，
 * 自动完成枚举与数据库 int 值的转换。
 *
 * <pre>{@code
 * @Entity
 * public class User {
 *     @EnumConvert(Status.class)
 *     @Column(name = "status")
 *     private Status status;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumConvert {
    /**
     * 枚举类型，必须实现 {@link BaseEnum}。
     */
    Class<? extends Enum<?>> value();
}
```

```java
/**
 * JPA 注解。
 */
package com.cartisan.data.jpa.annotation;
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :cartisan-data-jpa:compileJava
```

Expected: SUCCESS

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/annotation/
git commit -m "feat(data-jpa): add @EnumConvert annotation

Mark fields that need enum ↔ int conversion with JPA.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: 创建 UniversalEnumConverter

**Files:**
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/UniversalEnumConverter.java`
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/package-info.java`
- Test: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/UniversalEnumConverterTest.java`

- [ ] **Step 1: 创建 Converter**

```java
package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 通用枚举转换器。
 * <p>
 * 将实现 {@link BaseEnum} 的枚举类型与数据库 Integer 值相互转换。
 *
 * @param <E> 枚举类型，必须实现 BaseEnum
 */
public class UniversalEnumConverter<E extends Enum<E> & BaseEnum<E>>
        implements AttributeConverter<E, Integer> {

    private final Class<E> enumType;

    /**
     * 创建转换器实例。
     *
     * @param enumType 枚举类型
     */
    @SuppressWarnings("unchecked")
    public UniversalEnumConverter(Class<?> enumType) {
        this.enumType = (Class<E>) enumType;
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
     */
    public Class<E> getEnumType() {
        return enumType;
    }
}
```

```java
/**
 * JPA 转换器。
 */
package com.cartisan.data.jpa.converter;
```

- [ ] **Step 2: 编写测试**

```java
package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

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
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}

class UniversalEnumConverterTest {

    @Test
    void shouldConvertEnumToInteger() {
        var converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThat(converter.convertToDatabaseColumn(TestOrderStatus.PENDING))
            .isEqualTo(1);
    }

    @Test
    void shouldReturnNull_whenConvertNullEnumToDatabase() {
        var converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThat(converter.convertToDatabaseColumn(null))
            .isNull();
    }

    @Test
    void shouldConvertIntegerToEnum() {
        var converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThat(converter.convertToEntityAttribute(2))
            .isEqualTo(TestOrderStatus.COMPLETED);
    }

    @Test
    void shouldReturnNull_whenConvertNullIntegerToEntity() {
        var converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThat(converter.convertToEntityAttribute(null))
            .isNull();
    }

    @Test
    void shouldThrowException_whenConvertInvalidInteger() {
        var converter = new UniversalEnumConverter<>(TestOrderStatus.class);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(999))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown code: 999");
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
./gradlew :cartisan-data-jpa:test --tests UniversalEnumConverterTest
```

Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/ \
        cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/
git commit -m "feat(data-jpa): add UniversalEnumConverter

Generic JPA AttributeConverter for BaseEnum types.
Converts enum ↔ Integer for database storage.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: 创建 Converter 注册逻辑

**Files:**
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/EnumConverterRegistrar.java`
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java`
- Test: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/EnumConverterRegistrarTest.java`

- [ ] **Step 1: 创建注册器**

```java
package com.cartisan.data.jpa.converter;

import com.cartisan.data.jpa.annotation.EnumConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 枚举转换器注册器。
 * <p>
 * 扫描 JPA 管理的实体类，收集所有带 {@link EnumConvert} 注解的字段，
 * 为每个枚举类型创建 {@link UniversalEnumConverter} 实例。
 */
public class EnumConverterRegistrar {

    private static final Logger log = LoggerFactory.getLogger(EnumConverterRegistrar.class);

    /**
     * 扫描实体类，收集需要转换的枚举类型。
     *
     * @param entityManagerFactory JPA EntityManagerFactory
     * @return 枚举类型去重后的集合
     */
    public Set<Class<?>> scanEnumTypes(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        Set<Class<?>> enumTypes = new HashSet<>();

        var managedTypes = entityManagerFactory.getManagedClasses();
        if (managedTypes == null) {
            return enumTypes;
        }

        for (String className : managedTypes) {
            try {
                Class<?> clazz = Class.forName(className);
                scanFields(clazz, enumTypes);
            } catch (ClassNotFoundException e) {
                log.warn("Failed to load class: {}", className, e);
            }
        }

        return enumTypes;
    }

    /**
     * 扫描类的字段，收集带 @EnumConvert 注解的枚举类型。
     */
    private void scanFields(Class<?> clazz, Set<Class<?>> enumTypes) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(EnumConvert.class)) {
                EnumConvert annotation = field.getAnnotation(EnumConvert.class);
                enumTypes.add(annotation.value());
                log.debug("Found @EnumConvert field: {} in {}",
                    field.getName(), clazz.getSimpleName());
            }
        }
    }

    /**
     * 为枚举类型创建 Converter 实例。
     */
    public UniversalEnumConverter<?> createConverter(Class<?> enumType) {
        return new UniversalEnumConverter<>(enumType);
    }
}
```

- [ ] **Step 2: 修改自动配置类**

在 `CartisanDataJpaAutoConfiguration` 类中添加：

```java
import com.cartisan.data.jpa.converter.EnumConverterRegistrar;
import com.cartisan.data.jpa.converter.UniversalEnumConverter;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

// 在类中添加新方法：

/**
 * 注册枚举转换器。
 * <p>
 * 扫描所有带 {@link com.cartisan.data.jpa.annotation.EnumConvert} 的字段，
 * 为每个枚举类型注册对应的 {@link UniversalEnumConverter}。
 *
 * @return BeanFactoryPostProcessor
 */
@Bean
public static BeanFactoryPostProcessor enumConverterRegistrar(
        LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    return factory -> {
        EnumConverterRegistrar registrar = new EnumConverterRegistrar();
        Set<Class<?>> enumTypes = registrar.scanEnumTypes(entityManagerFactory);

        for (Class<?> enumType : enumTypes) {
            UniversalEnumConverter<?> converter = registrar.createConverter(enumType);
            String beanName = enumType.getSimpleName() + "Converter";
            factory.registerSingleton(beanName, converter);
            log.info("Registered enum converter: {} for type: {}", beanName, enumType.getSimpleName());
        }
    };
}
```

- [ ] **Step 3: 编写集成测试**

```java
package com.cartisan.data.jpa.converter;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.data.jpa.annotation.EnumConvert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import java.util.Set;

enum TestPaymentStatus implements BaseEnum<TestPaymentStatus> {
    PAID(1, "已支付"),
    UNPAID(0, "未支付");

    private final Integer code;
    private final String name;

    TestPaymentStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}

@Entity
class TestPaymentOrder {
    @Id
    private Long id;

    @EnumConvert(TestPaymentStatus.class)
    @Column(name = "status")
    private TestPaymentStatus status;
}

class EnumConverterRegistrarTest {

    @Test
    void shouldScanEnumTypes_whenEntityHasEnumConvertField() {
        var registrar = new EnumConverterRegistrar();
        var emf = new LocalContainerEntityManagerFactoryBean();

        // 手动设置 managed classes 用于测试
        emf.setManagedClasses(Set.of(TestPaymentOrder.class.getName()).toArray(new String[0]));

        Set<Class<?>> enumTypes = registrar.scanEnumTypes(emf);

        assertThat(enumTypes).containsExactly(TestPaymentStatus.class);
    }

    @Test
    void shouldCreateConverter_forEnumType() {
        var registrar = new EnumConverterRegistrar();

        UniversalEnumConverter<?> converter = registrar.createConverter(TestPaymentStatus.class);

        assertThat(converter.getEnumType()).isEqualTo(TestPaymentStatus.class);
        assertThat(converter.convertToDatabaseColumn(TestPaymentStatus.PAID)).isEqualTo(1);
        assertThat(converter.convertToEntityAttribute(1)).isEqualTo(TestPaymentStatus.PAID);
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
./gradlew :cartisan-data-jpa:test --tests EnumConverterRegistrarTest
```

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java \
        cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/EnumConverterRegistrar.java \
        cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/converter/EnumConverterRegistrarTest.java
git commit -m "feat(data-jpa): add enum converter auto-registration

Scan @EnumConvert fields and register UniversalEnumConverter instances.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: 创建 Jackson 序列化器

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/config/BaseEnumSerializer.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/config/BaseEnumSerializerTest.java`

- [ ] **Step 1: 创建序列化器**

```java
package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * BaseEnum Jackson 序列化器。
 * <p>
 * 将 BaseEnum 序列化为其 code 值（Integer）。
 */
public class BaseEnumSerializer extends JsonSerializer<BaseEnum<?>> {

    @Override
    public void serialize(BaseEnum<?> value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeNumber(value.getCode());
        }
    }
}
```

- [ ] **Step 2: 编写测试**

```java
package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

enum TestVisibility implements BaseEnum<TestVisibility> {
    PUBLIC(1, "公开"),
    PRIVATE(0, "私有");

    private final Integer code;
    private final String name;

    TestVisibility(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}

record TestVisibilityResponse(TestVisibility visibility) {}

class BaseEnumSerializerTest {

    @Test
    void shouldSerializeEnumToCode() throws Exception {
        var mapper = new ObjectMapper();
        var response = new TestVisibilityResponse(TestVisibility.PUBLIC);

        String json = mapper.writeValueAsString(response);

        assertThat(json).contains("\"visibility\":1");
    }

    @Test
    void shouldSerializeNullEnum() throws Exception {
        var mapper = new ObjectMapper();
        var response = new TestVisibilityResponse(null);

        String json = mapper.writeValueAsString(response);

        assertThat(json).contains("\"visibility\":null");
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
./gradlew :cartisan-web:test --tests BaseEnumSerializerTest
```

Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/BaseEnumSerializer.java \
        cartisan-web/src/test/java/com/cartisan/web/config/BaseEnumSerializerTest.java
git commit -m "feat(web): add BaseEnum Jackson serializer

Serialize BaseEnum to its Integer code value.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: 创建 Jackson 反序列化器

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/config/BaseEnumDeserializer.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/config/BaseEnumDeserializerTest.java`

- [ ] **Step 1: 创建反序列化器**

```java
package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;

/**
 * BaseEnum Jackson 反序列化器。
 * <p>
 * 将 Integer 值反序列化为对应的 BaseEnum 实例。
 * 使用 {@link ContextualDeserializer} 获取目标字段的实际枚举类型。
 */
public class BaseEnumDeserializer extends JsonDeserializer<BaseEnum<?>>
        implements ContextualDeserializer {

    private Class<? extends BaseEnum<?>> enumType;

    // 无参构造函数，用于 Jackson 创建实例
    public BaseEnumDeserializer() {
    }

    // 带参构造函数，用于 createContextual 返回具体类型的反序列化器
    @SuppressWarnings("unchecked")
    public BaseEnumDeserializer(Class<?> enumType) {
        this.enumType = (Class<? extends BaseEnum<?>>) enumType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, com.fasterxml.jackson.databind.BeanProperty property) {
        Class<?> targetType = property.getType().getRawClass();
        return new BaseEnumDeserializer(targetType);
    }

    @Override
    public BaseEnum<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Integer code = p.getValueAsInt();
        if (code == null) {
            return null;
        }
        return BaseEnum.parseByCode(enumType, code);
    }
}
```

- [ ] **Step 2: 编写测试**

```java
package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

enum TestPriority implements BaseEnum<TestPriority> {
    HIGH(2, "高"),
    MEDIUM(1, "中"),
    LOW(0, "低");

    private final Integer code;
    private final String name;

    TestPriority(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}

record TestPriorityRequest(TestPriority priority) {}

class BaseEnumDeserializerTest {

    @Test
    void shouldDeserializeCodeToEnum() throws Exception {
        var mapper = new ObjectMapper();
        String json = "{\"priority\":2}";

        TestPriorityRequest request = mapper.readValue(json, TestPriorityRequest.class);

        assertThat(request.priority()).isEqualTo(TestPriority.HIGH);
    }

    @Test
    void shouldDeserializeNullToNull() throws Exception {
        var mapper = new ObjectMapper();
        String json = "{\"priority\":null}";

        TestPriorityRequest request = mapper.readValue(json, TestPriorityRequest.class);

        assertThat(request.priority()).isNull();
    }

    @Test
    void shouldDeserializeInvalidCodeToNull() throws Exception {
        var mapper = new ObjectMapper();
        String json = "{\"priority\":999}";

        TestPriorityRequest request = mapper.readValue(json, TestPriorityRequest.class);

        // parseByCode 对无效 code 返回 null
        assertThat(request.priority()).isNull();
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
./gradlew :cartisan-web:test --tests BaseEnumDeserializerTest
```

Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/BaseEnumDeserializer.java \
        cartisan-web/src/test/java/com/cartisan/web/config/BaseEnumDeserializerTest.java
git commit -m "feat(web): add BaseEnum Jackson deserializer

Deserialize Integer code to BaseEnum using ContextualDeserializer.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: 配置 Jackson

**Files:**
- Modify: `cartisan-web/src/main/java/com/cartisan/web/config/JacksonConfiguration.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/config/JacksonConfigurationTest.java`

- [ ] **Step 1: 修改 JacksonConfiguration**

读取现有文件内容：
1. 删除以下行：
```java
// Enum → 字符串
.featuresToDisable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
```
2. 更新类 JavaDoc 中 "Enum → 字符串：可读性更好" 为 "BaseEnum → code：业务枚举序列化为整数"

在 `jackson2ObjectMapperBuilderCustomizer` 方法中添加：
```java
.serializerByType(BaseEnum.class, new BaseEnumSerializer())
.deserializerByType(BaseEnum.class, new BaseEnumDeserializer())
```

完整修改后应类似：

```java
@Bean
public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
    return builder -> builder
        // Long → 字符串（解决 JS 精度问题）
        .serializerByType(Long.class, new ToStringSerializer())
        .serializerByType(Long.TYPE, new ToStringSerializer())

        // LocalDateTime → ISO 8601
        .modules(new JavaTimeModule())

        // BigDecimal → 禁用科学计数法
        .featuresToEnable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)

        // BaseEnum → code
        .serializerByType(BaseEnum.class, new BaseEnumSerializer())
        .deserializerByType(BaseEnum.class, new BaseEnumDeserializer())

        // 忽略未知属性
        .featuresToDisable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
}
```

- [ ] **Step 2: 添加 import 语句**

```java
import com.cartisan.core.domain.BaseEnum;
```

- [ ] **Step 3: 编写集成测试**

```java
package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;

class JacksonConfigurationTest {

    @Test
    void shouldConfigureBaseEnumSerialization() {
        JacksonConfiguration config = new JacksonConfiguration();
        Jackson2ObjectMapperBuilderCustomizer customizer = config.jackson2ObjectMapperBuilderCustomizer();

        var builder = com.fasterxml.jackson.databind.json.JsonMapper.builder();
        customizer.customize(builder);

        ObjectMapper mapper = builder.build();

        // 验证 BaseEnum 序列化
        TestColor color = TestColor.RED;
        assertThat(mapper.writeValueAsString(color)).isEqualTo("1");
    }

    @Test
    void shouldConfigureBaseEnumDeserialization() throws Exception {
        JacksonConfiguration config = new JacksonConfiguration();
        Jackson2ObjectMapperBuilderCustomizer customizer = config.jackson2ObjectMapperBuilderCustomizer();

        var builder = com.fasterxml.jackson.databind.json.JsonMapper.builder();
        customizer.customize(builder);

        ObjectMapper mapper = builder.build();

        // 验证反序列化
        TestColor color = mapper.readValue("1", TestColor.class);
        assertThat(color).isEqualTo(TestColor.RED);
    }

    enum TestColor implements BaseEnum<TestColor> {
        RED(1, "红"),
        BLUE(2, "蓝");

        private final Integer code;
        private final String name;

        TestColor(Integer code, String name) {
            this.code = code;
            this.name = name;
        }

        @Override
        public Integer getCode() { return code; }

        @Override
        public String getName() { return name; }
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
./gradlew :cartisan-web:test --tests JacksonConfigurationTest
```

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/JacksonConfiguration.java \
        cartisan-web/src/test/java/com/cartisan/web/config/JacksonConfigurationTest.java
git commit -m "feat(web): configure Jackson for BaseEnum

Remove Enum→string config, add BaseEnum serialization/deserialization.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: 端到端集成测试

**Files:**
- Test: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/EnumIntegrationTest.java`

- [ ] **Step 1: 创建集成测试**

```java
package com.cartisan.data.jpa.integration;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.data.jpa.annotation.EnumConvert;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import static org.assertj.core.api.Assertions.*;

/**
 * 枚举增强集成测试。
 * <p>
 * 验证 BaseEnum ↔ JPA ↔ Database 的完整流程。
 */
@DataJpaTest
class EnumIntegrationTest {

    @Autowired
    TestEntityManager entityManager;

    @Test
    void shouldPersistEnumAsInteger() {
        // 创建测试表
        entityManager.getEntityManager()
            .createNativeQuery("CREATE TABLE test_users (id BIGINT PRIMARY KEY, status INTEGER)")
            .executeUpdate();

        TestUser user = new TestUser();
        user.id = 1L;
        user.status = TestUserStatus.ACTIVE;

        entityManager.persistAndFlush(user);
        entityManager.clear();

        // 验证数据库中存储的是 int
        Integer dbValue = (Integer) entityManager.getEntityManager()
            .createNativeQuery("SELECT status FROM test_users WHERE id = 1")
            .getSingleResult();
        assertThat(dbValue).isEqualTo(1);
    }

    @Test
    void shouldLoadIntegerAsEnum() {
        // 插入原始数据
        entityManager.getEntityManager()
            .createNativeQuery("INSERT INTO test_users (id, status) VALUES (2, 0)")
            .executeUpdate();

        TestUser user = entityManager.find(TestUser.class, 2L);

        assertThat(user.status).isEqualTo(TestUserStatus.INACTIVE);
    }

    @Test
    void shouldHandleNullEnumValue() {
        TestUser user = new TestUser();
        user.id = 3L;
        user.status = null;

        entityManager.persistAndFlush(user);
        entityManager.clear();

        TestUser loaded = entityManager.find(TestUser.class, 3L);
        assertThat(loaded.status).isNull();
    }
}

// 测试实体
@Entity
@Table(name = "test_users")
class TestUser {
    @Id
    Long id;

    @EnumConvert(TestUserStatus.class)
    @Column(name = "status")
    TestUserStatus status;
}

// 测试枚举
enum TestUserStatus implements BaseEnum<TestUserStatus> {
    ACTIVE(1, "激活"),
    INACTIVE(0, "未激活");

    private final Integer code;
    private final String name;

    TestUserStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}
```

- [ ] **Step 2: 运行集成测试**

```bash
./gradlew :cartisan-data-jpa:test --tests EnumIntegrationTest
```

Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/
git commit -m "test(data-jpa): add enum integration test

Verify BaseEnum ↔ JPA ↔ Database end-to-end flow.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: 验证清单

- [ ] **Step 1: 运行所有测试**

```bash
./gradlew test
```

Expected: ALL PASS

- [ ] **Step 2: 检查代码覆盖率**

```bash
./gradlew test jacocoTestReport
```

Expected: 覆盖率符合项目标准

- [ ] **Step 3: 构建验证**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 最终提交**

```bash
git add docs/superpowers/plans/2026-03-25-enum-enhancement.md
git commit -m "docs: add enum enhancement implementation plan

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## 完成标准

- [ ] 所有单元测试通过
- [ ] 集成测试通过
- [ ] 代码已提交
- [ ] 实现计划已归档
