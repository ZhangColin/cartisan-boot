# BaseEnum Spring MVC 参数绑定支持设计文档

**日期**: 2026-04-01
**状态**: 设计中
**模块**: cartisan-web

## 1. 背景

### 1.1 现状

cartisan-boot 框架的 `BaseEnum` 接口目前已支持：
- ✅ **JPA 层**：通过 `@EnumConvert` 注解 + `UniversalEnumConverter` 实现 int ↔ enum 转换
- ✅ **Jackson 层**：通过 `BaseEnumSerializer` + `BaseEnumDeserializer` 实现 JSON 序列化
- ❌ **Spring MVC 层**：**缺失**，无法在 `@RequestParam`、`@PathVariable` 中直接使用枚举类型

### 1.2 问题

业务代码中需要手动转换 String 参数为枚举：

```java
// 现在需要手动转换
@GetMapping("/users")
public List<User> getUsers(@RequestParam Integer status) {
    UserStatus userStatus = UserStatus.parseByCode(UserStatus.class, status);
    // ...
}
```

期望的用法：

```java
// 直接使用枚举类型
@GetMapping("/users")
public List<User> getUsers(@RequestParam UserStatus status) {
    // status 已自动转换为 UserStatus 枚举
    // ...
}
```

### 1.3 目标

在 cartisan-boot 框架中添加 Spring MVC 参数绑定支持，实现零配置自动转换。

## 2. 设计方案

### 2.1 架构

在 `cartisan-web` 模块中新增组件：

```
cartisan-web/
├── config/
│   ├── CartisanWebAutoConfiguration.java  (修改 - 实现 WebMvcConfigurer)
│   ├── JacksonConfiguration.java          (不变)
│   └── BaseEnumConverterFactory.java     (新增 - Converter 工厂)
└── exception/
    └── GlobalExceptionHandler.java        (修改 - 优化 MethodArgumentTypeMismatchException 处理)
```

### 2.2 组件设计

#### 2.2.1 BaseEnumConverter

**职责**：将 String 参数（Integer code）转换为 BaseEnum 枚举类型。

**特性**：
- 实现 `ConverterFactory<String, BaseEnum<?>>` 接口
- 只接受 Integer code 格式（如 `"1"`），不支持 name（如 `"ACTIVE"`）
- null/空字符串返回 null（由业务层的 `@NotNull` 校验处理）
- 无效 code 抛出 `IllegalArgumentException`

**核心逻辑**：
```java
public class BaseEnumConverter implements ConverterFactory<String, BaseEnum<?>> {

    @Override
    public <T extends BaseEnum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToBaseEnumConverter<>(targetType);
    }

    private static class StringToBaseEnumConverter<T extends BaseEnum<?>>
            implements Converter<String, T> {

        private final Class<T> enumType;

        @Override
        public T convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }

            try {
                Integer code = Integer.valueOf(source);
                T result = BaseEnum.parseByCode(enumType, code);
                if (result == null) {
                    throw new IllegalArgumentException(
                        "Invalid enum code: " + code + " for " + enumType.getSimpleName()
                    );
                }
                return result;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Enum value must be Integer code, not string: " + source
                );
            }
        }
    }
}
```

#### 2.2.2 CartisanWebAutoConfiguration 修改

**修改内容**：
1. 类声明添加 `implements WebMvcConfigurer`
2. 添加 `addFormatters()` 方法注册 Converter

**修改后代码**：
```java
@AutoConfiguration
@ConditionalOnWebApplication
@Import(AutoResponseConfiguration.class)
public class CartisanWebAutoConfiguration implements WebMvcConfigurer {

    // ... 现有代码 ...

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new BaseEnumConverter());
    }
}
```

#### 2.2.3 GlobalExceptionHandler 修改

**问题**：当前 `MethodArgumentTypeMismatchException` 返回 404 NOT_FOUND，但参数值无效应该是 400 BAD_REQUEST。

**修改内容**：细化 `MethodArgumentTypeMismatchException` 处理逻辑。

**修改后代码**：
```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String paramName = ex.getName();
    String paramValue = ex.getValue() != null ? ex.getValue().toString() : "null";

    // 如果是 BaseEnum 转换失败，给出更友好的提示
    Throwable cause = ex.getCause();
    if (cause instanceof IllegalArgumentException illegalArgEx) {
        log.warn("Invalid parameter value: {} = {}", paramName, paramValue);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, illegalArgEx.getMessage()));
    }

    // 其他类型不匹配（如路径变量类型错误），仍返回 404
    log.warn("Type mismatch: {}", paramName);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(BaseCodeMessage.NOT_FOUND));
}
```

### 2.3 使用示例

**Controller 示例**：
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // GET /api/users?status=1
    @GetMapping
    public List<UserDTO> getUsers(@RequestParam UserStatus status) {
        // status 已自动转换为 UserStatus.ACTIVE
        return userService.getUsersByStatus(status);
    }

    // PUT /api/users/123?status=0
    @PutMapping("/{id}")
    public void updateUserStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {
        // status 已自动转换为 UserStatus.DISABLED
        userService.updateStatus(id, status);
    }
}
```

**错误响应示例**：
```json
# GET /api/users?status=999
{
  "success": false,
  "code": 400,
  "message": "Invalid enum code: 999 for UserStatus"
}
```

## 3. 测试策略

### 3.1 单元测试

**文件**：`BaseEnumConverterTest.java`

**测试用例**：
1. 有效 code → 正确枚举
2. 无效 code → IllegalArgumentException
3. null → null
4. 空字符串 → null
5. 非数字字符串 → IllegalArgumentException

### 3.2 集成测试

**文件**：`BaseEnumConverterIntegrationTest.java`

**测试内容**：
- 创建 TestController 接收 BaseEnum 参数
- 使用 `MockMvc` 测试 `@RequestParam` 场景
- 使用 `MockMvc` 测试 `@PathVariable` 场景
- 验证异常处理返回 400 BAD_REQUEST

**测试枚举**：复用 `cartisan-core` 中的 `TestStatus` 或创建新的 `TestUserStatus`。

## 4. 文档更新

### 4.1 更新 BaseEnum.java 注释

在 `cartisan-core` 的 `BaseEnum` 接口中：

```java
/**
 * 基础枚举接口。
 * <p>
 * 业务枚举实现此接口后，框架自动完成：
 * <ul>
 *   <li>JPA：int ↔ enum 转换（需 @EnumConvert 注解）</li>
 *   <li>Jackson：enum ↔ int 序列化</li>
 *   <li>Spring MVC：String → enum 参数绑定（@RequestParam、@PathVariable）</li>
 * </ul>
 */
public interface BaseEnum<T extends Enum<T> & BaseEnum<T>> {
    // ...
}
```

### 4.2 用户文档

在 `docs/guide/` 中添加使用说明（如果需要）。

## 5. 实施清单

- [ ] 创建 `BaseEnumConverterFactory.java`
- [ ] 修改 `CartisanWebAutoConfiguration.java` 实现 `WebMvcConfigurer`
- [ ] 修改 `GlobalExceptionHandler.java` 的 `MethodArgumentTypeMismatchException` 处理
- [ ] 编写 `BaseEnumConverterTest.java` 单元测试
- [ ] 编写 `BaseEnumConverterIntegrationTest.java` 集成测试
- [ ] 更新 `BaseEnum.java` 注释
- [ ] 运行所有测试确保通过
- [ ] 提交代码

## 6. 设计决策记录

### 6.1 为什么只支持 Integer code，不支持 String name？

**决策**：只支持 Integer code 格式。

**原因**：
1. BaseEnum 的设计理念是"code 用于存储和传输，name 用于显示"
2. HTTP 参数传输应该是稳定的 code 值，而不是可能变化的 name
3. 保持 API 设计一致性（JPA、Jackson 都是 code）
4. 实现更简洁，错误信息更明确

### 6.2 为什么集成到 CartisanWebAutoConfiguration 而不是创建新的 AutoConfiguration？

**决策**：集成到现有的 `CartisanWebAutoConfiguration`。

**原因**：
1. BaseEnum 参数绑定是 Web 层基础功能
2. 避免配置类碎片化
3. 零配置，引入 cartisan-web 即生效
4. 符合框架"简洁"的设计理念

### 6.3 为什么 null/空字符串返回 null 而不是抛异常？

**决策**：返回 null。

**原因**：
1. 符合 Spring MVC 的 Converter 规范（Converter 可以返回 null）
2. 职责分离：Converter 负责类型转换，校验由 `@NotNull` 等注解处理
3. 更灵活：业务层可以决定是否允许 null
4. 与 Jackson、JPA 的处理方式一致
