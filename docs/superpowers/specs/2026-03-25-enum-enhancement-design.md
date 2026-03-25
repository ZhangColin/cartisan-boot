# 枚举增强设计文档

**日期**: 2026-03-25
**状态**: 设计中

## 1. 需求概述

业务枚举只需定义 `code`/`name`，框架自动完成：
- 数据库存储：`int` 值
- 前端交互：JSON 传递 `int` 值
- 枚举转换：`int` ↔ `Enum` 自动转换

## 2. 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                        cartisan-core                         │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  BaseEnum 接口                                          │  │
│  │  - Integer getCode()                                    │  │
│  │  - String getName()                                     │  │
│  │  - static T parseByCode(Class<T>, Integer)              │  │
│  │  - static T requireByCode(Class<T>, Integer)            │  │
│  └────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┴─────────────┐
                ▼                           ▼
┌─────────────────────────┐   ┌───────────────────────────────┐
│     cartisan-web        │   │    cartisan-data-jpa          │
│ ┌─────────────────────┐ │   │ ┌───────────────────────────┐ │
│ │ JacksonConfiguration│ │   │ │ @EnumConvert 注解          │ │
│ │ - BaseEnum → int    │ │   │ │ - 标识需要转换的枚举字段    │ │
│ │ - int → BaseEnum    │ │   │ └───────────────────────────┘ │
│ └─────────────────────┘ │   │ ┌───────────────────────────┐ │
└─────────────────────────┘   │ │ UniversalEnumConverter     │ │
                              │ │ - Entity ↔ int 转换        │ │
                              │ └───────────────────────────┘ │
                              └───────────────────────────────┘
```

## 3. 模块划分

### 3.1 cartisan-core

**新增包**: `com.cartisan.core.model`

**新增类**: `BaseEnum<T>`

```java
public interface BaseEnum<T extends Enum<T> & BaseEnum<T>> {
    Integer getCode();
    String getName();

    static <T extends Enum<T> & BaseEnum<T>> T parseByCode(Class<T> cls, Integer code) {
        if (code == null) return null;
        for (T t : cls.getEnumConstants()) {
            if (t.getCode().equals(code)) {
                return t;
            }
        }
        return null;
    }

    static <T extends Enum<T> & BaseEnum<T>> T requireByCode(Class<T> cls, Integer code) {
        T result = parseByCode(cls, code);
        if (result == null) {
            throw new IllegalArgumentException("Unknown code: " + code + " for " + cls.getSimpleName());
        }
        return result;
    }
}
```

### 3.2 cartisan-data-jpa

**新增包**: `com.cartisan.data.jpa.annotation`

**新增注解**: `@EnumConvert`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumConvert {
    Class<? extends Enum<?>> value();
}
```

**新增包**: `com.cartisan.data.jpa.converter`

**新增类**: `UniversalEnumConverter<E>`

```java
public class UniversalEnumConverter<E extends Enum<E> & BaseEnum<E>>
        implements AttributeConverter<E, Integer> {

    private final Class<E> enumType;

    public UniversalEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public Integer convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public E convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return null;
        return BaseEnum.requireByCode(enumType, dbData);
    }
}
```

**修改类**: `CartisanDataJpaAutoConfiguration`

新增 `BeanFactoryPostProcessor`，扫描 `@EnumConvert` 字段并注册 Converter：

```java
@Bean
public static BeanFactoryPostProcessor enumConverterRegistrar() {
    return factory -> {
        Set<Class<?>> enumTypes = scanEnumConvertFields(factory);
        for (Class<?> enumType : enumTypes) {
            registerConverter(factory, enumType);
        }
    };
}
```

### 3.3 cartisan-web

**修改类**: `JacksonConfiguration`

1. 删除 `Enum → 字符串` 配置
2. 新增 `BaseEnum` 序列化器
3. 新增 `BaseEnum` 反序列化器（实现 `ContextualDeserializer`）

```java
// 序列化：BaseEnum → int
.serializerByType(BaseEnum.class, new JsonSerializer<BaseEnum>() {
    @Override
    public void serialize(BaseEnum value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeNumber(value.getCode());
    }
})

// 反序列化：int → BaseEnum（通过 ContextualDeserializer 获取目标类型）
.deserializerByType(BaseEnum.class, new BaseEnumDeserializer())
```

## 4. 数据流

### 4.1 写操作（前端 → 数据库）

```
前端 JSON {"status": 1}
    ↓ Jackson 反序列化
DTO.status = AdminStatus.ACTIVE
    ↓ MapStruct 映射
Entity.status = AdminStatus.ACTIVE
    ↓ JPA + UniversalEnumConverter
数据库 status = 1
```

### 4.2 读操作（数据库 → 前端）

```
数据库 status = 1
    ↓ JPA + UniversalEnumConverter
Entity.status = AdminStatus.ACTIVE
    ↓ MapStruct 映射
DTO.status = AdminStatus.ACTIVE
    ↓ Jackson 序列化
前端 JSON {"status": 1}
```

## 5. 业务使用示例

### 5.1 定义枚举

```java
@Getter
@AllArgsConstructor
public enum AdminStatus implements BaseEnum<AdminStatus> {
    ACTIVE(1, "启用"),
    DISABLED(0, "禁用");

    private final Integer code;
    private final String name;
}
```

### 5.2 实体使用

```java
@Entity
@Table(name = "adm_users")
public class AdminUser {

    @EnumConvert(AdminStatus.class)
    @Column(name = "status")
    private AdminStatus status;
}
```

### 5.3 DTO 定义

```java
public record AdminUserResponse(
    AdminStatus status,      // 自动序列化为 code
    Integer statusCode,      // MapStruct 自动调用 getCode()
    String statusName        // MapStruct 自动调用 getName()
) {}
```

### 5.4 Controller 使用

```java
// 方式1：自动反序列化
public void update(@RequestBody UpdateStatusRequest request) {
    // request.getStatus() 已是 AdminStatus 枚举
}

// 方式2：手动转换
public void updateStatus(@PathVariable Long id, @RequestParam Integer status) {
    AdminStatus statusEnum = BaseEnum.requireByCode(AdminStatus.class, status);
}
```

## 6. 技术决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| JPA 转换方式 | @Convert + UniversalEnumConverter | code 值稳定，不依赖枚举顺序 |
| @DbValue 注解 | 不使用 | 直接调用 BaseEnum.getCode()，更简单 |
| Jackson 反序列化 | 自动 + 手动都支持 | ContextualDeserializer 获取字段类型 |
| null 值处理 | null → null，非法 code 抛异常 | 明确语义，便于发现错误 |

## 7. 测试策略

1. **单元测试**：`BaseEnum.parseByCode()` / `requireByCode()` 各种边界情况
2. **集成测试**：JPA 读写数据库，验证 int ↔ enum 转换
3. **序列化测试**：Jackson 序列化/反序列化验证
4. **端到端测试**：Controller → Service → Repository → Database

## 8. 文件清单

### 新增文件

| 模块 | 路径 | 说明 |
|------|------|------|
| cartisan-core | `model/BaseEnum.java` | 基础枚举接口 |
| cartisan-data-jpa | `annotation/EnumConvert.java` | 枚举转换注解 |
| cartisan-data-jpa | `converter/UniversalEnumConverter.java` | 通用转换器 |
| cartisan-data-jpa | `converter/BaseEnumDeserializer.java` | Jackson 反序列化器 |
| cartisan-data-jpa | `converter/BaseEnumSerializer.java` | Jackson 序列化器 |

### 修改文件

| 模块 | 路径 | 修改内容 |
|------|------|----------|
| cartisan-data-jpa | `config/CartisanDataJpaAutoConfiguration.java` | 新增 Converter 注册逻辑 |
| cartisan-web | `config/JacksonConfiguration.java` | 新增枚举序列化/反序列化配置 |
