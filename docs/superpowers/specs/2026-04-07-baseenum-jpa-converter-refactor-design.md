# BaseEnum JPA Converter 重构设计

**日期**: 2026-04-07
**状态**: 已批准
**作者**: Claude

## 问题陈述

当前 `@EnumConvert` 注解和 `UniversalEnumConverter` 的实现存在问题：

1. **功能失效**：`@EnumConvert` 标记的字段仍然使用 Hibernate 默认的 `EnumType.ORDINAL` 映射
2. **注册缺失**：`EnumConverterRegistrar` 只扫描并记录日志，没有将 Converter 注册到 Hibernate
3. **测试覆盖不足**：`EnumIntegrationTest` 是伪集成测试，没有验证与 JPA 的实际集成
4. **设计过度复杂**：引入了不必要的注解和运行时扫描逻辑

### 问题表现

```java
// 数据库：status = 1 (integer)
// AdminUserStatus 枚举：
//   ACTIVE(1) → ordinal = 0（第一个声明）
//   DISABLED(0) → ordinal = 1（第二个声明）
// Hibernate 把数据库的 1 当作 ordinal，返回了 DISABLED ❌
```

## 设计方案

### 核心思路

采用**枚举内部 Converter 类**模式：

1. 将 `UniversalEnumConverter` 重构为抽象基类 `BaseEnumConverter<T>`
2. 每个枚举内部声明静态内部类 `Converter extends BaseEnumConverter<XXX>`
3. 使用 `@Converter(autoApply = true)` 让 Hibernate 自动应用

### 架构对比

```
旧架构:
@EnumConvert(AdminUserStatus.class)
→ EnumConverterRegistrar 扫描
→ UniversalEnumConverter 转换
❌ 注册失败，使用 ORDINAL

新架构:
AdminUserStatus {
    @Converter(autoApply = true)
    static class Converter extends BaseEnumConverter<AdminUserStatus> {...}
}
→ Hibernate 自动扫描
→ BaseEnumConverter 转换
✅ 正常工作
```

### 核心组件

#### 1. BaseEnumConverter<T> 抽象基类

```java
public abstract class BaseEnumConverter<T extends Enum<T> & BaseEnum<T>>
        implements AttributeConverter<T, Integer> {

    private final Class<T> enumClass;

    protected BaseEnumConverter(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public Integer convertToDatabaseColumn(T attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public T convertToEntityAttribute(Integer dbData) {
        return BaseEnum.parseByCode(enumClass, dbData);
    }
}
```

**职责**：
- 封装 BaseEnum 与 Integer 的转换逻辑
- 提供类型安全的泛型支持
- 作为所有枚举 Converter 的基类

#### 2. 枚举内部 Converter 声明

```java
public enum AdminUserStatus implements BaseEnum<AdminUserStatus> {
    ACTIVE(1, "激活"),
    DISABLED(0, "禁用");

    private final Integer code;
    private final String name;

    AdminUserStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }

    /**
     * JPA 枚举转换器。
     * <p>
     * 通过 @Converter(autoApply = true) 让 Hibernate 自动应用到所有
     * AdminUserStatus 类型的字段，无需在实体类中显式标注。
     */
    @Converter(autoApply = true)
    public static class Converter extends BaseEnumConverter<AdminUserStatus> {
        public Converter() {
            super(AdminUserStatus.class);
        }
    }
}
```

**职责**：
- 为具体枚举类型提供 Converter 实现
- 通过 `@Converter(autoApply = true)` 实现自动应用
- 与枚举定义在同一文件，保持内聚性

#### 3. 实体类使用

```java
@Entity
public class AdminUser {
    @Id
    private Long id;

    // 零注解！Hibernate 自动应用 AdminUserStatus.Converter
    private AdminUserStatus status;
}
```

### 删除的组件

以下组件将被删除，因为它们不再需要：

1. **`@EnumConvert` 注解**
   - 文件：`cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/annotation/EnumConvert.java`
   - 原因：`@Converter(autoApply = true)` 已足够

2. **`EnumConverterRegistrar`**
   - 文件：`cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/EnumConverterRegistrar.java`
   - 原因：Hibernate 自动扫描 Converter，无需手动注册

3. **`enumConverterScanner` Bean**
   - 文件：`cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java` (第 99-112 行)
   - 原因：扫描逻辑已无必要

4. **伪集成测试**
   - 文件：`cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/integration/EnumIntegrationTest.java`
   - 原因：不测试真正的 JPA 集成，应该删除并重写

5. **过时的单元测试**
   - `EnumConverterRegistrarTest.java` - 测试已删除的类
   - `UniversalEnumConverterTest.java` - 需要重写为测试新的抽象基类

## 使用示例

### 定义枚举

```java
public enum OrderStatus implements BaseEnum<OrderStatus> {
    PENDING(1, "待支付"),
    PAID(2, "已支付"),
    CANCELLED(3, "已取消");

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

### 实体类使用

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private Long id;

    // 无需任何注解，自动应用 OrderStatus.Converter
    private OrderStatus status;

    private OrderType type;

    // Getters and Setters
}
```

### 持久化验证

```java
@DataJpaTest
class OrderRepositoryTest {
    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldSaveAndLoadEnumWithCorrectCode() {
        // Given
        Order order = new Order();
        order.setStatus(OrderStatus.PAID); // code = 2

        // When
        orderRepository.save(order);
        Order loaded = orderRepository.findById(order.getId()).orElseThrow();

        // Then
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.PAID);
        // 验证数据库存储的是 code 值 2，不是 ordinal 1
    }
}
```

## 优势分析

### 1. 符合 JPA 标准
- 使用标准的 `@Converter` 注解
- 不依赖 Hibernate 私有 API（如 `DynamicParameterizedType`）
- 理论上可切换到其他 JPA 实现（EclipseLink、DataNucleus）

### 2. 零样板代码
- 实体字段无需任何注解
- 每个枚举只需 3 行内部类代码
- 比 `@Convert(converter = XXX.class)` 更简洁

### 3. 内聚性更好
- 枚举和它的持久化规则在同一文件
- 删除枚举时，Converter 不会被遗漏
- 代码组织更清晰，类似 MapStruct 的 Mapper 模式

### 4. 类型安全
- 泛型 `BaseEnumConverter<T>` 提供编译时类型检查
- 无需运行时反射获取类型
- IDE 自动补全友好

### 5. 可维护性
- 每个枚举独立可控
- 修改一个枚举的转换逻辑不影响其他枚举
- 调试友好，静态类型清晰

## 迁移指南

### 现有代码迁移

**旧代码**（如果有）：
```java
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

**新代码**：
```java
@Entity
public class Order {
    // 删除 @EnumConvert 注解
    private OrderStatus status;
}

public enum OrderStatus implements BaseEnum<OrderStatus> {
    PENDING(1, "待支付"),
    PAID(2, "已支付");

    // 添加内部 Converter 类
    @Converter(autoApply = true)
    public static class Converter extends BaseEnumConverter<OrderStatus> {
        public Converter() {
            super(OrderStatus.class);
        }
    }
    // ...
}
```

### 文档更新

需要更新以下文档：

1. **`docs/PITFALLS.md` DATA-007**
   ```markdown
   ### 规则 DATA-007：枚举持久化使用内部 Converter

   **正确做法**：
   ```java
   public enum UserStatus implements BaseEnum<UserStatus> {
       ACTIVE(1, "激活"),
       INACTIVE(0, "未激活");

       // ...

       @Converter(autoApply = true)
       public static class Converter extends BaseEnumConverter<UserStatus> {
           public Converter() {
               super(UserStatus.class);
           }
       }
   }
   ```

   **记忆口诀**：枚举持久化用内部 Converter，实体字段零注解。
   ```

2. **`cartisan-core/domain/BaseEnum.java` JavaDoc**
   - 更新使用说明
   - 添加内部 Converter 示例

## 风险评估

### 低风险
- ✅ 没有发现实际业务代码使用 `@EnumConvert`
- ✅ 改造局限于 `cartisan-data-jpa` 和 `cartisan-core` 模块
- ✅ 不影响其他模块

### 需要注意
- ⚠️ 确保所有枚举都添加了内部 Converter 类
- ⚠️ 更新项目文档和编码规范
- ⚠️ 提供清晰的迁移指南

## 测试策略

### 单元测试
- 测试 `BaseEnumConverter` 转换逻辑
- 测试 null 值处理
- 测试无效 code 抛异常

### 集成测试
- 使用 `@DataJpaTest` 验证 JPA 持久化
- 验证数据库存储的是 code 值
- 验证读取时正确反序列化

### ArchUnit 测试
- 确保所有 `BaseEnum` 实现都有内部 Converter 类
- 确保实体类不再使用 `@EnumConvert`

## 实施计划

详细实施计划将在 `writing-plans` 阶段输出，包括：

1. 重构 `UniversalEnumConverter` 为 `BaseEnumConverter`
2. 删除 `@EnumConvert` 注解和相关类
3. 更新配置类
4. 删除伪集成测试
5. 编写新的集成测试
6. 更新文档
7. 更新 ArchUnit 规则

## 参考

- [Baeldung - Custom Types in Hibernate](https://www.baeldung.com/hibernate-custom-types)
- [Hibernate 6 AttributeConverter Documentation](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#basic-datatype)
- [Stack Overflow - How do I handle custom types in Hibernate 6?](https://stackoverflow.com/questions/76418374/how-do-i-handle-custom-types-in-hibernate-6)
