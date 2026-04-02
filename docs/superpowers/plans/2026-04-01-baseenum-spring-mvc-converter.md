# BaseEnum Spring MVC 参数绑定支持实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 在 cartisan-web 模块中添加 Spring MVC 参数绑定支持，使 `@RequestParam`、`@PathVariable` 能直接接收 BaseEnum 类型参数

**架构:** 通过 Spring MVC 的 ConverterFactory 机制，将 String 参数（Integer code）自动转换为 BaseEnum 枚举类型，零配置自动生效

**技术栈:** Spring Boot 3.4.x, Spring MVC, JUnit 5, AssertJ, MockMvc

---

## 前置检查

**验证当前状态：**

运行以下命令确保环境干净：
```bash
cd /Users/zhangcolin/workspace/cartisan-boot
./gradlew :cartisan-web:test --tests "*CartisanWeb*" --quiet
```

预期：测试通过（如果有现有测试）

---

## Task 1: 创建测试用枚举

**文件:**
- Create: `cartisan-web/src/test/java/com/cartisan/web/config/TestUserStatus.java`

- [ ] **Step 1: 创建测试用枚举 TestUserStatus**

创建测试文件：
```java
package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;

/**
 * BaseEnum 测试用枚举。
 */
public enum TestUserStatus implements BaseEnum<TestUserStatus> {
    ACTIVE(1, "启用"),
    DISABLED(0, "禁用"),
    PENDING(2, "待审核");

    private final Integer code;
    private final String name;

    TestUserStatus(Integer code, String name) {
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
```

- [ ] **Step 2: 验证编译通过**

```bash
./gradlew :cartisan-web:compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-web/src/test/java/com/cartisan/web/config/TestUserStatus.java
git commit -m "test: add TestUserStatus enum for BaseEnum converter testing"
```

---

## Task 2: 创建 BaseEnumConverter (单元测试先行)

**文件:**
- Create: `cartisan-web/src/test/java/com/cartisan/web/config/BaseEnumConverterTest.java`
- Create: `cartisan-web/src/main/java/com/cartisan/web/config/BaseEnumConverter.java`

- [ ] **Step 1: 编写失败的单元测试**

创建测试文件：
```java
package com.cartisan.web.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BaseEnumConverter 单元测试")
class BaseEnumConverterTest {

    private final BaseEnumConverter converter = new BaseEnumConverter();

    @Test
    @DisplayName("应该将有效的 Integer code 转换为对应枚举")
    void shouldConvertValidCodeToEnum() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        TestUserStatus result = enumConverter.convert("1");

        assertThat(result).isEqualTo(TestUserStatus.ACTIVE);
    }

    @Test
    @DisplayName("应该将 code 0 转换为对应枚举")
    void shouldConvertZeroCodeToEnum() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        TestUserStatus result = enumConverter.convert("0");

        assertThat(result).isEqualTo(TestUserStatus.DISABLED);
    }

    @Test
    @DisplayName("当传入 null 时应该返回 null")
    void shouldReturnNull_whenInputIsNull() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        TestUserStatus result = enumConverter.convert(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("当传入空字符串时应该返回 null")
    void shouldReturnNull_whenInputIsEmpty() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        TestUserStatus result = enumConverter.convert("");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("当传入无效 code 时应该抛出异常")
    void shouldThrowException_whenCodeIsInvalid() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        assertThatThrownBy(() -> enumConverter.convert("999"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid enum code: 999")
            .hasMessageContaining("TestUserStatus");
    }

    @Test
    @DisplayName("当传入非数字字符串时应该抛出异常")
    void shouldThrowException_whenInputIsNotNumber() {
        var enumConverter = converter.getConverter(TestUserStatus.class);

        assertThatThrownBy(() -> enumConverter.convert("ACTIVE"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Enum value must be Integer code")
            .hasMessageContaining("ACTIVE");
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests "BaseEnumConverterTest" --quiet
```

预期：测试失败，找不到 BaseEnumConverter 类

- [ ] **Step 3: 提交测试**

```bash
git add cartisan-web/src/test/java/com/cartisan/web/config/BaseEnumConverterTest.java
git commit -m "test: add failing BaseEnumConverter unit tests"
```

---

## Task 3: 实现 BaseEnumConverter

**文件:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/config/BaseEnumConverter.java`

- [ ] **Step 1: 实现 BaseEnumConverter**

创建实现文件：
```java
package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * BaseEnum Converter Factory。
 *
 * 将 String 参数（Integer code）转换为 BaseEnum 枚举类型。
 *
 * <p>支持 @RequestParam、@PathVariable 等场景直接使用枚举类型参数：</p>
 * <pre>
 * {@code
 * public void updateStatus(
 *         @PathVariable Long id,
 *         @RequestParam AdminStatus status) {  // "1" → AdminStatus.ACTIVE
 *     // ...
 * }
 * }
 * </pre>
 *
 * @since 0.3.0
 */
public class BaseEnumConverter implements ConverterFactory<String, BaseEnum<?>> {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BaseEnum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToBaseEnumConverter<>(targetType);
    }

    /**
     * String → BaseEnum Converter。
     * 只接受 Integer code 格式（如 "1"），不支持 name（如 "ACTIVE"）。
     */
    private static class StringToBaseEnumConverter<T extends BaseEnum<?>>
            implements Converter<String, T> {

        private final Class<T> enumType;

        StringToBaseEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

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

- [ ] **Step 2: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests "BaseEnumConverterTest" --quiet
```

预期：测试全部通过

- [ ] **Step 3: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/BaseEnumConverter.java
git commit -m "feat: implement BaseEnumConverter for Spring MVC parameter binding"
```

---

## Task 4: 修改 CartisanWebAutoConfiguration

**文件:**
- Modify: `cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java`

- [ ] **Step 1: 修改 CartisanWebAutoConfiguration 实现 WebMvcConfigurer**

在文件开头添加 import：
```java
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
```

修改类声明，添加接口：
```java
@AutoConfiguration
@ConditionalOnWebApplication
@Import(AutoResponseConfiguration.class)
public class CartisanWebAutoConfiguration implements WebMvcConfigurer {
```

在类的末尾（最后一个方法之后，类结束 `}` 之前）添加：
```java
    /**
     * 注册 BaseEnum Converter Factory。
     *
     * 支持 @RequestParam、@PathVariable 直接使用 BaseEnum 类型参数。
     *
     * @see com.cartisan.web.config.BaseEnumConverter
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new BaseEnumConverter());
    }
```

- [ ] **Step 2: 验证编译通过**

```bash
./gradlew :cartisan-web:compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java
git commit -m "feat: register BaseEnumConverter in CartisanWebAutoConfiguration"
```

---

## Task 5: 创建集成测试（测试 Controller）

**文件:**
- Create: `cartisan-web/src/test/java/com/cartisan/web/config/TestEnumController.java`
- Create: `cartisan-web/src/test/java/com/cartisan/web/config/BaseEnumConverterIntegrationTest.java`

- [ ] **Step 1: 创建测试 Controller**

创建测试 Controller：
```java
package com.cartisan.web.config;

import org.springframework.web.bind.annotation.*;

/**
 * BaseEnum 集成测试用 Controller。
 */
@RestController
@RequestMapping("/test/enum")
public class TestEnumController {

    @GetMapping("/request-param")
    public String testRequestParam(@RequestParam TestUserStatus status) {
        return "Status: " + status.name() + " (code=" + status.getCode() + ")";
    }

    @GetMapping("/path-variable/{status}")
    public String testPathVariable(@PathVariable TestUserStatus status) {
        return "Status: " + status.name() + " (code=" + status.getCode() + ")";
    }

    @GetMapping("/optional")
    public String testOptional(@RequestParam(required = false) TestUserStatus status) {
        if (status == null) {
            return "Status is null";
        }
        return "Status: " + status.name() + " (code=" + status.getCode() + ")";
    }
}
```

- [ ] **Step 2: 编写集成测试**

创建集成测试文件：
```java
package com.cartisan.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BaseEnumConverter 集成测试")
class BaseEnumConverterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("应该将 @RequestParam Integer code 转换为枚举")
    void shouldConvertRequestParam() throws Exception {
        mockMvc.perform(get("/test/enum/request-param")
                .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Status: ACTIVE (code=1)"));
    }

    @Test
    @DisplayName("应该将 @PathVariable Integer code 转换为枚举")
    void shouldConvertPathVariable() throws Exception {
        mockMvc.perform(get("/test/enum/path-variable/0"))
                .andExpect(status().isOk())
                .andExpect(content().string("Status: DISABLED (code=0)"));
    }

    @Test
    @DisplayName("应该处理可选参数为 null 的情况")
    void shouldHandleNullOptionalParam() throws Exception {
        mockMvc.perform(get("/test/enum/optional"))
                .andExpect(status().isOk())
                .andExpect(content().string("Status is null"));
    }

    @Test
    @DisplayName("当传入无效 code 时应该返回 400")
    void shouldReturn400_whenCodeIsInvalid() throws Exception {
        mockMvc.perform(get("/test/enum/request-param")
                .param("status", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                    "{\"success\":false,\"code\":400,\"message\":\"Invalid enum code: 999 for TestUserStatus\"}"
                ));
    }

    @Test
    @DisplayName("当传入非数字字符串时应该返回 400")
    void shouldReturn400_whenInputIsNotNumber() throws Exception {
        mockMvc.perform(get("/test/enum/request-param")
                .param("status", "ACTIVE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                    "{\"success\":false,\"code\":400,\"message\":\"Enum value must be Integer code, not string: ACTIVE\"}"
                ));
    }
}
```

- [ ] **Step 3: 运行集成测试验证失败（异常处理尚未修改）**

```bash
./gradlew :cartisan-web:test --tests "BaseEnumConverterIntegrationTest" --quiet
```

预期：部分测试失败（异常处理返回 404 而不是 400）

- [ ] **Step 4: 提交**

```bash
git add cartisan-web/src/test/java/com/cartisan/web/config/TestEnumController.java
git add cartisan-web/src/test/java/com/cartisan/web/config/BaseEnumConverterIntegrationTest.java
git commit -m "test: add integration tests for BaseEnumConverter"
```

---

## Task 6: 修改 GlobalExceptionHandler 优化异常处理

**文件:**
- Modify: `cartisan-web/src/main/java/com/cartisan/web/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 修改 MethodArgumentTypeMismatchException 处理**

找到第 128 行的 `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` 方法，替换为：

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

- [ ] **Step 2: 验证编译通过**

```bash
./gradlew :cartisan-web:compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 运行集成测试验证通过**

```bash
./gradlew :cartisan-web:test --tests "BaseEnumConverterIntegrationTest" --quiet
```

预期：所有测试通过

- [ ] **Step 4: 运行所有 web 模块测试确保无回归**

```bash
./gradlew :cartisan-web:test --quiet
```

预期：所有测试通过

- [ ] **Step 5: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/exception/GlobalExceptionHandler.java
git commit -m "fix: improve MethodArgumentTypeMismatchException handling for BaseEnum"
```

---

## Task 7: 更新 BaseEnum 接口注释

**文件:**
- Modify: `cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java`

- [ ] **Step 1: 更新 BaseEnum 接口的 JavaDoc 注释**

找到第 3-10 行的注释，修改为：

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
```

- [ ] **Step 2: 验证编译通过**

```bash
./gradlew :cartisan-core:compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java
git commit -m "docs: update BaseEnum JavaDoc to include Spring MVC support"
```

---

## Task 8: 全量测试验证

- [ ] **Step 1: 运行所有模块测试**

```bash
./gradlew test --quiet
```

预期：所有测试通过

如果测试失败，检查并修复问题后重新运行。

- [ ] **Step 2: 运行 cartisan-web 模块测试（详细输出）**

```bash
./gradlew :cartisan-web:test
```

预期：所有测试通过，包括新增的 BaseEnumConverterTest 和 BaseEnumConverterIntegrationTest

- [ ] **Step 3: 检查测试覆盖率**

```bash
./gradlew :cartisan-web:test JacocoTestReport --quiet
```

预期：BaseEnumConverter 有良好覆盖率

---

## Task 9: 清理和归档

- [ ] **Step 1: 合并所有 commit 为一个功能 commit（可选）**

如果希望保持提交历史整洁，可以 squash：

```bash
# 查看最近的 commit
git log --oneline -10

# 交互式 rebase 合并 commit（保留最近 9 个 commit）
git rebase -i HEAD~9

# 在编辑器中，将除了第一个 commit 外的所有 commit 从 "pick" 改为 "squash" 或 "s"
# 保存并退出，然后编辑 commit message

# 或者创建一个汇总 commit
git reset --soft HEAD~9
git commit -m "feat: add BaseEnum Spring MVC parameter binding support

- 实现 BaseEnumConverter 将 Integer code 转换为枚举
- 在 CartisanWebAutoConfiguration 中自动注册 Converter
- 优化 GlobalExceptionHandler 对 IllegalArgumentException 的处理
- 添加完整的单元测试和集成测试
- 更新 BaseEnum 接口文档

支持 @RequestParam、@PathVariable 直接使用 BaseEnum 类型参数，
无需手动转换。"
```

- [ ] **Step 2: 清理过程文件（如果有）**

检查是否有临时文件或测试数据需要清理：
```bash
git status
```

- [ ] **Step 3: 查看最终差异**

```bash
git diff HEAD~1
```

确认所有修改都是预期的。

---

## 验收标准

完成后，应该满足以下标准：

1. **功能完整性**
   - ✅ @RequestParam 可以接收 BaseEnum 类型参数
   - ✅ @PathVariable 可以接收 BaseEnum 类型参数
   - ✅ 支持 null 值（可选参数）
   - ✅ 无效 code 返回 400 Bad Request

2. **代码质量**
   - ✅ 所有测试通过
   - ✅ 单元测试覆盖核心逻辑
   - ✅ 集成测试覆盖真实场景
   - ✅ 无测试警告

3. **文档完整性**
   - ✅ BaseEnum 接口注释已更新
   - ✅ 代码注释清晰
   - ✅ 设计文档完整

4. **架构一致性**
   - ✅ 遵循框架现有模式
   - ✅ 零配置自动生效
   - ✅ 不破坏现有功能

---

## 参考资料

- **设计文档:** [docs/superpowers/specs/2026-04-01-baseenum-spring-mvc-converter-design.md](../specs/2026-04-01-baseenum-spring-mvc-converter-design.md)
- **现有 BaseEnum 支持:** [cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java](../../cartisan-core/src/main/java/com/cartisan/core/domain/BaseEnum.java)
- **Jackson 配置:** [cartisan-web/src/main/java/com/cartisan/web/config/JacksonConfiguration.java](../../cartisan-web/src/main/java/com/cartisan/web/config/JacksonConfiguration.java)
- **JPA 转换器:** [cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/UniversalEnumConverter.java](../../cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/converter/UniversalEnumConverter.java)
