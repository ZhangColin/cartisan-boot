# 枚举选项功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 为 cartisan-boot 框架添加枚举选项支持，允许前端轻松获取枚举选项列表用于下拉框等 UI 组件

**架构：** 基于 BaseEnum 接口，通过自动扫描注册枚举类，提供工具类转换、默认 Controller 端点和批量接口

**技术栈：** Spring Boot 3.4.x, Java 21, JUnit 5, AssertJ

---

## 文件结构

### 新建文件
- `cartisan-web/src/main/java/com/cartisan/web/response/EnumOption.java` - 枚举选项 DTO
- `cartisan-web/src/main/java/com/cartisan/web/support/EnumOptionUtils.java` - 枚举转换工具类
- `cartisan-web/src/main/java/com/cartisan/web/enums/EnumRegistry.java` - 枚举注册表
- `cartisan-web/src/main/java/com/cartisan/web/enums/EnumScanner.java` - 枚举扫描器
- `cartisan-web/src/main/java/com/cartisan/web/controller/EnumControllerBase.java` - Controller 基类
- `cartisan-web/src/main/java/com/cartisan/web/controller/EnumController.java` - 默认 Controller 实现
- `cartisan-web/src/main/java/com/cartisan/web/controller/EnumBatchRequest.java` - 批量请求 DTO
- `cartisan-web/src/main/java/com/cartisan/web/config/EnumControllerProperties.java` - 配置属性类

### 修改文件
- `cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java` - 添加枚举相关 Bean 注册

### 测试文件
- `cartisan-web/src/test/java/com/cartisan/web/response/EnumOptionTest.java` - DTO 测试
- `cartisan-web/src/test/java/com/cartisan/web/support/EnumOptionUtilsTest.java` - 工具类测试
- `cartisan-web/src/test/java/com/cartisan/web/enums/EnumRegistryTest.java` - 注册表测试
- `cartisan-web/src/test/java/com/cartisan/web/enums/EnumScannerTest.java` - 扫描器测试
- `cartisan-web/src/test/java/com/cartisan/web/controller/EnumControllerTest.java` - Controller 集成测试
- `cartisan-web/src/test/java/com/cartisan/web/config/EnumControllerPropertiesTest.java` - 配置属性测试

### 文档更新
- `docs/guide/cartisan-boot-使用手册.md` - 添加枚举选项功能说明

---

## Task 1: 创建 EnumOption DTO

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/response/EnumOption.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/response/EnumOptionTest.java`

- [ ] **Step 1: 编写 EnumOptionTest 测试**

```java
package com.cartisan.web.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumOptionTest {

    @Test
    void shouldCreateEnumOption() {
        EnumOption option = new EnumOption(1, "激活");

        assertThat(option.code()).isEqualTo(1);
        assertThat(option.name()).isEqualTo("激活");
    }

    @Test
    void shouldImplementSerializable() {
        EnumOption option = new EnumOption(1, "激活");

        assertThat(option).isInstanceOf(java.io.Serializable.class);
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :cartisan-web:test --tests EnumOptionTest`
Expected: FAIL with "class EnumOption not found"

- [ ] **Step 3: 实现 EnumOption**

```java
package com.cartisan.web.response;

import java.io.Serializable;

/**
 * 枚举选项 DTO，供前端下拉框等组件使用。
 *
 * @param code 枚举 code 值（提交给后端）
 * @param name 枚举显示名称（前端展示）
 * @since 0.9.0
 */
public record EnumOption(
    Integer code,
    String name
) implements Serializable {
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :cartisan-web:test --tests EnumOptionTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/response/EnumOption.java \
        cartisan-web/src/test/java/com/cartisan/web/response/EnumOptionTest.java
git commit -m "feat: add EnumOption DTO for enum options"
```

---

## Task 2: 创建 EnumOptionUtils 工具类

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/support/EnumOptionUtils.java`
- Create: `cartisan-web/src/test/java/com/cartisan/web/enums/TestUserStatus.java` (测试用枚举)
- Test: `cartisan-web/src/test/java/com/cartisan/web/support/EnumOptionUtilsTest.java`

- [ ] **Step 1: 创建测试用枚举 TestUserStatus**

```java
package com.cartisan.web.enums;

import com.cartisan.core.domain.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TestUserStatus implements BaseEnum<TestUserStatus> {
    ACTIVE(1, "激活"),
    INACTIVE(0, "未激活");

    private final Integer code;
    private final String name;
}
```

- [ ] **Step 2: 编写 EnumOptionUtilsTest 测试**

```java
package com.cartisan.web.support;

import com.cartisan.web.enums.TestUserStatus;
import com.cartisan.web.response.EnumOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnumOptionUtilsTest {

    @Test
    void shouldConvertEnumToOptions() {
        List<EnumOption> options = EnumOptionUtils.fromEnum(TestUserStatus.class);

        assertThat(options).hasSize(2);
        assertThat(options.get(0).code()).isEqualTo(1);
        assertThat(options.get(0).name()).isEqualTo("激活");
        assertThat(options.get(1).code()).isEqualTo(0);
        assertThat(options.get(1).name()).isEqualTo("未激活");
    }

    @Test
    void shouldConvertEnumArrayToOptions() {
        List<EnumOption> options = EnumOptionUtils.fromEnums(
            TestUserStatus.ACTIVE,
            TestUserStatus.INACTIVE
        );

        assertThat(options).hasSize(2);
        assertThat(options.get(0).code()).isEqualTo(1);
    }
}
```

- [ ] **Step 3: 运行测试验证失败**

Run: `./gradlew :cartisan-web:test --tests EnumOptionUtilsTest`
Expected: FAIL with "class EnumOptionUtils not found"

- [ ] **Step 4: 实现 EnumOptionUtils**

```java
package com.cartisan.web.support;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.web.response.EnumOption;

import java.util.Arrays;
import java.util.List;

/**
 * 枚举选项工具类。
 *
 * @since 0.9.0
 */
public class EnumOptionUtils {

    /**
     * 将枚举类转换为选项列表。
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 选项列表
     */
    public static <E extends Enum<E> & BaseEnum<E>> List<EnumOption> fromEnum(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
            .map(e -> new EnumOption(e.getCode(), e.getName()))
            .toList();
    }

    /**
     * 将枚举类转换为选项列表（通配符版本，供 EnumRegistry 使用）。
     *
     * @param enumClass 枚举类
     * @return 选项列表
     */
    @SuppressWarnings("rawtypes")
    public static List<EnumOption> fromEnum(Class<? extends BaseEnum> enumClass) {
        Object[] enumConstants = enumClass.getEnumConstants();
        return Arrays.stream(enumConstants)
            .map(e -> (BaseEnum) e)
            .map(e -> new EnumOption(e.getCode(), e.getName()))
            .toList();
    }

    /**
     * 将枚举数组转换为选项列表。
     *
     * @param enums 枚举数组
     * @param <E>   枚举类型
     * @return 选项列表
     */
    @SafeVarargs
    public static <E extends Enum<E> & BaseEnum<E>> List<EnumOption> fromEnums(E... enums) {
        return Arrays.stream(enums)
            .map(e -> new EnumOption(e.getCode(), e.getName()))
            .toList();
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `./gradlew :cartisan-web:test --tests EnumOptionUtilsTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/support/EnumOptionUtils.java \
        cartisan-web/src/test/java/com/cartisan/web/support/EnumOptionUtilsTest.java \
        cartisan-web/src/test/java/com/cartisan/web/enums/TestUserStatus.java
git commit -m "feat: add EnumOptionUtils for enum conversion"
```

---

## Task 3: 创建 EnumRegistry 注册表

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/enums/EnumRegistry.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/enums/EnumRegistryTest.java`

- [ ] **Step 1: 编写 EnumRegistryTest 测试**

```java
package com.cartisan.web.enums;

import com.cartisan.web.response.EnumOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumRegistryTest {

    private EnumRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EnumRegistry();
    }

    @Test
    void shouldRegisterAndGetEnum() {
        registry.register("TestUserStatus", TestUserStatus.class);

        Class<? extends Enum<?>> enumClass = registry.getEnumClass("TestUserStatus");
        assertThat(enumClass).isEqualTo(TestUserStatus.class);
    }

    @Test
    void shouldGetEnumOptions() {
        registry.register("TestUserStatus", TestUserStatus.class);

        List<EnumOption> options = registry.getEnumOptions("TestUserStatus");

        assertThat(options).hasSize(2);
        assertThat(options.get(0).code()).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionWhenEnumNotFound() {
        assertThatThrownBy(() -> registry.getEnumClass("NotExist"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Enum not found: NotExist");
    }

    @Test
    void shouldThrowExceptionWhenRegisteringNonEnum() {
        class NotAnEnum implements com.cartisan.core.domain.BaseEnum<NotAnEnum> {
            @Override public Integer getCode() { return 1; }
            @Override public String getName() { return "test"; }
        }

        assertThatThrownBy(() -> registry.register("NotAnEnum", NotAnEnum.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Class must be an enum");
    }

    @Test
    void shouldListRegisteredEnums() {
        registry.register("TestUserStatus", TestUserStatus.class);
        registry.register("AnotherEnum", TestUserStatus.class);

        List<String> enums = registry.listRegisteredEnums();

        assertThat(enums).hasSize(2);
        assertThat(enums).containsExactlyInAnyOrder("AnotherEnum", "TestUserStatus");
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :cartisan-web:test --tests EnumRegistryTest`
Expected: FAIL with "class EnumRegistry not found"

- [ ] **Step 3: 实现 EnumRegistry**

```java
package com.cartisan.web.enums;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.web.support.EnumOptionUtils;
import com.cartisan.web.response.EnumOption;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举注册表，维护枚举类名到 Class 的映射。
 *
 * @since 0.9.0
 */
@Component
public class EnumRegistry {

    private final Map<String, Class<? extends BaseEnum<?>>>> enumClassMap = new ConcurrentHashMap<>();

    /**
     * 注册枚举类。
     *
     * @param name      枚举类名（简单类名）
     * @param enumClass 枚举类
     * @throws IllegalArgumentException 如果类不是枚举
     */
    public void register(String name, Class<? extends BaseEnum<?>> enumClass) {
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("Class must be an enum: " + enumClass.getName());
        }
        enumClassMap.put(name, enumClass);
    }

    /**
     * 获取枚举 Class。
     *
     * @param name 枚举类名
     * @return 枚举 Class
     * @throws IllegalArgumentException 枚举不存在
     */
    public Class<? extends BaseEnum<?>> getEnumClass(String name) {
        Class<? extends BaseEnum<?>> enumClass = enumClassMap.get(name);
        if (enumClass == null) {
            throw new IllegalArgumentException("Enum not found: " + name);
        }
        return enumClass;
    }

    /**
     * 获取枚举选项列表。
     *
     * @param name 枚举类名
     * @return 选项列表
     */
    public List<EnumOption> getEnumOptions(String name) {
        Class<? extends BaseEnum<?>> enumClass = getEnumClass(name);
        return EnumOptionUtils.fromEnum(enumClass);
    }

    /**
     * 列出所有已注册的枚举。
     *
     * @return 枚举类名集合
     */
    public List<String> listRegisteredEnums() {
        return enumClassMap.keySet().stream().sorted().toList();
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :cartisan-web:test --tests EnumRegistryTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/enums/EnumRegistry.java \
        cartisan-web/src/test/java/com/cartisan/web/enums/EnumRegistryTest.java
git commit -m "feat: add EnumRegistry for enum management"
```

---

## Task 4: 创建 EnumScanner 扫描器

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/enums/EnumScanner.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/enums/EnumScannerTest.java`

- [ ] **Step 1: 编写 EnumScannerTest 测试**

```java
package com.cartisan.web.enums;

import com.cartisan.web.TestUserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumScannerTest {

    private EnumRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EnumRegistry();
    }

    @Test
    void shouldScanAndRegisterEnums() {
        EnumScanner scanner = new EnumScanner(registry);
        // 扫描 com.cartisan.web 包，其中包含 TestUserStatus 枚举
        scanner.scanBaseEnums("com.cartisan.web");

        List<String> enums = registry.listRegisteredEnums();
        assertThat(enums).contains("TestUserStatus");
    }

    @Test
    void shouldNotRegisterNonBaseEnumClasses() {
        EnumScanner scanner = new EnumScanner(registry);
        scanner.scanBaseEnums("java.lang");

        // String 不是 BaseEnum，不应该被注册
        assertThat(registry.listRegisteredEnums()).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :cartisan-web:test --tests EnumScannerTest`
Expected: FAIL with "class EnumScanner not found"

- [ ] **Step 3: 实现 EnumScanner**

```java
package com.cartisan.web.enums;

import com.cartisan.core.domain.BaseEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import jakarta.annotation.PostConstruct;
import java.util.Set;

/**
 * 枚举扫描器，启动时扫描所有实现 BaseEnum 的枚举。
 *
 * @since 0.9.0
 */
@Component
public class EnumScanner {

    private final EnumRegistry registry;

    @Value("${cartisan.web.enum-controller.scan-packages:}")
    private String[] scanPackages;

    public EnumScanner(EnumRegistry registry) {
        this.registry = registry;
    }

    /**
     * 扫描指定包下的所有 BaseEnum 枚举并注册。
     *
     * @param basePackage 基础包名
     */
    public void scanBaseEnums(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseEnum.class));

        Set<org.springframework.beans.factory.config.BeanDefinition> candidates =
            scanner.findCandidateComponents(basePackage);

        for (org.springframework.beans.factory.config.BeanDefinition candidate : candidates) {
            try {
                String className = candidate.getBeanClassName();
                Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());

                if (clazz.isEnum() && BaseEnum.class.isAssignableFrom(clazz)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends BaseEnum<?>> enumClass = (Class<? extends BaseEnum<?>>) clazz;
                    registry.register(clazz.getSimpleName(), enumClass);
                }
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
    }

    /**
     * 启动时扫描，默认扫描 com.cartisan 和 com.example 包。
     */
    @PostConstruct
    public void autoScan() {
        String[] packagesToScan = scanPackages.length > 0 ? scanPackages : new String[]{"com.cartisan", "com.example"};
        for (String pkg : packagesToScan) {
            scanBaseEnums(pkg);
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :cartisan-web:test --tests EnumScannerTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/enums/EnumScanner.java \
        cartisan-web/src/test/java/com/cartisan/web/enums/EnumScannerTest.java
git commit -m "feat: add EnumScanner for auto-registering enums"
```

---

## Task 5: 创建 Controller 层相关类

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/controller/EnumControllerBase.java`
- Create: `cartisan-web/src/main/java/com/cartisan/web/controller/EnumBatchRequest.java`
- Create: `cartisan-web/src/main/java/com/cartisan/web/controller/EnumController.java`
- Create: `cartisan-web/src/test/java/com/cartisan/web/controller/EnumControllerTest.java`
- Create: `cartisan-web/src/test/java/com/cartisan/web/controller/TestEnumController.java` (测试用 Controller)

- [ ] **Step 1: 编写 EnumBatchRequest DTO**

```java
package com.cartisan.web.controller;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量获取枚举请求 DTO。
 *
 * @param enums 枚举类名列表
 * @since 0.9.0
 */
public record EnumBatchRequest(
    @NotEmpty List<String> enums
) {}
```

- [ ] **Step 2: 编写 EnumControllerBase 基类**

```java
package com.cartisan.web.controller;

import com.cartisan.web.enums.EnumRegistry;
import com.cartisan.web.response.EnumOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举选项 Controller 基类。
 *
 * <p>业务项目可继承此类并自定义 URL 路径和业务逻辑。
 *
 * @since 0.9.0
 */
public abstract class EnumControllerBase {

    protected final EnumRegistry enumRegistry;

    protected EnumControllerBase(EnumRegistry enumRegistry) {
        this.enumRegistry = enumRegistry;
    }

    /**
     * 批量获取枚举选项。
     *
     * @param enumNames 枚举类名列表
     * @return key=类名, value=选项列表
     */
    protected Map<String, List<EnumOption>> batchEnums(List<String> enumNames) {
        Map<String, List<EnumOption>> result = new HashMap<>();
        for (String enumName : enumNames) {
            result.put(enumName, enumRegistry.getEnumOptions(enumName));
        }
        return result;
    }
}
```

- [ ] **Step 3: 编写 EnumController 默认实现**

```java
package com.cartisan.web.controller;

import com.cartisan.web.enums.EnumRegistry;
import com.cartisan.web.response.ApiResponse;
import com.cartisan.web.response.EnumOption;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 枚举选项 Controller（默认实现）。
 *
 * @since 0.9.0
 */
@RestController
@RequestMapping("${cartisan.web.enum-controller.path:/api/enums}")
public class EnumController extends EnumControllerBase {

    private final EnumRegistry enumRegistry;

    public EnumController(EnumRegistry enumRegistry) {
        super(enumRegistry);
        this.enumRegistry = enumRegistry;
    }

    @GetMapping("/{enumName}")
    public ApiResponse<List<EnumOption>> getEnum(@PathVariable String enumName) {
        return ApiResponse.ok(enumRegistry.getEnumOptions(enumName));
    }

    @PostMapping("/batch")
    public ApiResponse<Map<String, List<EnumOption>>> batchEnums(
            @RequestBody @Valid EnumBatchRequest request) {
        Map<String, List<EnumOption>> result = super.batchEnums(request.enums());
        return ApiResponse.ok(Map.of("enums", result));
    }
}
```

- [ ] **Step 4: 编写 EnumControllerTest 集成测试**

```java
package com.cartisan.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EnumControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldGetSingleEnum() throws Exception {
        mvc.perform(get("/api/enums/TestUserStatus"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].code").value(1))
            .andExpect(jsonPath("$.data[0].name").value("激活"));
    }

    @Test
    void shouldBatchGetEnums() throws Exception {
        String json = "{\"enums\":[\"TestUserStatus\"]}";

        mvc.perform(post("/api/enums/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enums.TestUserStatus").isArray())
            .andExpect(jsonPath("$.data.enums.TestUserStatus[0].code").value(1));
    }

    @Test
    void shouldReturn404WhenEnumNotFound() throws Exception {
        mvc.perform(get("/api/enums/NotExist"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldReturn400WhenBatchRequestIsEmpty() throws Exception {
        String json = "{\"enums\":[]}";

        mvc.perform(post("/api/enums/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 5: 运行测试验证失败**

Run: `./gradlew :cartisan-web:test --tests EnumControllerTest`
Expected: FAIL - 这是预期的失败，因为：
1. EnumController 需要 EnumRegistry Bean（由 EnumScanner 的 @Component 提供）
2. Spring Boot Test 会启动应用上下文
3. EnumScanner 的 @PostConstruct 会自动扫描并注册枚举
4. 但需要等待 Task 7 完成自动配置后才能正常工作

这个测试会在 Task 7 完成后通过。

- [ ] **Step 6: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/controller/ \
        cartisan-web/src/test/java/com/cartisan/web/controller/
git commit -m "feat: add EnumController and base class"
```

---

## Task 6: 创建配置属性类

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/config/EnumControllerProperties.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/config/EnumControllerPropertiesTest.java`

- [ ] **Step 1: 编写 EnumControllerPropertiesTest 测试**

```java
package com.cartisan.web.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumControllerPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        EnumControllerProperties properties = new EnumControllerProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getPath()).isEqualTo("/api/enums");
    }

    @Test
    void shouldSetProperties() {
        EnumControllerProperties properties = new EnumControllerProperties();
        properties.setEnabled(false);
        properties.setPath("/api/v2/enums");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getPath()).isEqualTo("/api/v2/enums");
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :cartisan-web:test --tests EnumControllerPropertiesTest`
Expected: FAIL with "class EnumControllerProperties not found"

- [ ] **Step 3: 实现 EnumControllerProperties**

```java
package com.cartisan.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 枚举 Controller 配置属性。
 *
 * @since 0.9.0
 */
@ConfigurationProperties(prefix = "cartisan.web.enum-controller")
public class EnumControllerProperties {
    private boolean enabled = true;
    private String path = "/api/enums";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :cartisan-web:test --tests EnumControllerPropertiesTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/EnumControllerProperties.java \
        cartisan-web/src/test/java/com/cartisan/web/config/EnumControllerPropertiesTest.java
git commit -m "feat: add EnumControllerProperties"
```

---

## Task 7: 修改 CartisanWebAutoConfiguration 添加自动配置

**Files:**
- Modify: `cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java`

- [ ] **Step 1: 阅读现有 CartisanWebAutoConfiguration**

Read: `cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java`
确认现有导入和类结构

- [ ] **Step 2: 添加必要的导入和类级别注解**

在文件顶部的 import 区域添加：

```java
import com.cartisan.web.controller.EnumController;
import com.cartisan.web.enums.EnumRegistry;
import com.cartisan.web.enums.EnumScanner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
```

在类声明上添加 `@EnableConfigurationProperties`：

```java
@AutoConfiguration
@ConditionalOnWebApplication
@Import(AutoResponseConfiguration.class)
@EnableConfigurationProperties(EnumControllerProperties.class)  // 添加这一行
public class CartisanWebAutoConfiguration implements WebMvcConfigurer {
```

- [ ] **Step 3: 添加 EnumScanner Bean**

```java
/**
 * 注册枚举扫描器。
 *
 * @param enumRegistry 枚举注册表
 * @return EnumScanner 实例
 */
@Bean
public EnumScanner enumScanner(EnumRegistry enumRegistry) {
    return new EnumScanner(enumRegistry);
}
```

- [ ] **Step 4: 添加 EnumScanner Bean**

```java
/**
 * 注册枚举扫描器。
 *
 * @param enumRegistry 枚举注册表
 * @return EnumScanner 实例
 */
@Bean
public EnumScanner enumScanner(EnumRegistry enumRegistry) {
    return new EnumScanner(enumRegistry);
}
```

- [ ] **Step 5: 添加 EnumController Bean**

```java
/**
 * 注册枚举 Controller（默认实现）。
 *
 * <p>可通过配置项 {@code cartisan.web.enum-controller.enabled} 禁用。
 *
 * @param enumRegistry 枚举注册表
 * @return EnumController 实例
 */
@Bean
@ConditionalOnProperty(
    prefix = "cartisan.web.enum-controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public EnumController enumController(EnumRegistry enumRegistry) {
    return new EnumController(enumRegistry);
}
```

- [ ] **Step 6: 运行全部测试验证集成**

Run: `./gradlew :cartisan-web:test`
Expected: PASS (包括 EnumControllerTest)

- [ ] **Step 7: 提交**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java
git commit -m "feat: register enum controller beans in auto-configuration"
```

---

## Task 8: 更新使用手册文档

**Files:**
- Modify: `docs/guide/cartisan-boot-使用手册.md`

- [ ] **Step 1: 在"一、模块能力清单"的 1.3 cartisan-web 模块中添加**

在表格中添加新行：

```markdown
| **枚举选项** | `EnumOption`、`EnumOptionUtils`、`EnumController` 支持前端获取枚举选项列表 |
```

- [ ] **Step 2: 在"二、核心概念和 API"中添加新章节**

在合适位置（如 2.35 之后）添加：

```markdown
### 2.36 枚举选项支持（com.cartisan.web.response）

| 类/方法 | 说明 |
|--------|------|
| `EnumOption` | 枚举选项 DTO，包含 code 和 name 字段 |
| `EnumOptionUtils.fromEnum(Class)` | 将枚举类转换为选项列表 |
| `EnumRegistry` | 枚举注册表，维护枚举类名到 Class 的映射 |
| `EnumController` | 默认 Controller，提供 `/api/enums/{enumName}` 和 `/api/enums/batch` 端点 |
| `EnumControllerBase` | Controller 基类，可继承自定义 |

**配置属性**：

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `cartisan.web.enum-controller.enabled` | `boolean` | `true` | 是否启用默认 Controller |
| `cartisan.web.enum-controller.path` | `String` | `/api/enums` | Controller 路径 |
```

- [ ] **Step 3: 在"三、使用示例"中添加使用示例**

在合适位置添加：

```markdown
### 3.34 使用枚举选项工具

```java
// 转换单个枚举
List<EnumOption> options = EnumOptionUtils.fromEnum(UserStatus.class);

// 转换枚举数组
List<EnumOption> options = EnumOptionUtils.fromEnums(
    UserStatus.ACTIVE,
    UserStatus.INACTIVE
);

// 在 Response 中包含选项
public record UserResponse(
    Long id,
    UserStatus status,
    List<EnumOption> statusOptions
) {}

@Mapper(componentModel = "spring")
public interface UserMapper extends DomainMapper<User, UserResponse> {
    @Mapping(target = "statusOptions",
             expression = "java(EnumOptionUtils.fromEnum(UserStatus.class))")
    UserResponse toResponse(User user);
}
```

### 3.35 使用默认枚举 Controller

```yaml
# application.yml（默认配置）
cartisan:
  web:
    enum-controller:
      enabled: true
      path: /api/enums
```

```javascript
// 前端调用示例
const fetchEnums = async () => {
  // 单个枚举
  const response1 = await fetch('/api/enums/UserStatus');
  const data1 = await response1.json();
  // data1.data = [{code: 1, name: "激活"}, {code: 0, name: "禁用"}]

  // 批量获取
  const response2 = await fetch('/api/enums/batch', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({enums: ['UserStatus', 'OrderStatus']})
  });
  const data2 = await response2.json();
  // data2.data.enums = {UserStatus: [...], OrderStatus: [...]}
};
```

### 3.36 自定义枚举 Controller

```yaml
# 禁用默认实现
cartisan:
  web:
    enum-controller:
      enabled: false
```

```java
@RestController
@RequestMapping("/api/v2/dict")
public class DictController extends EnumControllerBase {

    public DictController(EnumRegistry enumRegistry) {
        super(enumRegistry);
    }

    @GetMapping("/{enumName}")
    public ApiResponse<List<EnumOption>> getEnum(@PathVariable String enumName) {
        return ApiResponse.ok(enumRegistry.getEnumOptions(enumName));
    }

    @PostMapping("/batch")
    public ApiResponse<Map<String, List<EnumOption>>> batch(
            @RequestBody EnumBatchRequest request) {
        return ApiResponse.ok(batchEnums(request.enums()));
    }
}
```
```

- [ ] **Step 4: 提交**

```bash
git add docs/guide/cartisan-boot-使用手册.md
git commit -m "docs: add enum option feature to user guide"
```

---

## Task 9: 全量测试和验证

**Files:**
- Run: All tests

- [ ] **Step 1: 运行 cartisan-web 模块全部测试**

Run: `./gradlew :cartisan-web:test`
Expected: ALL PASS

- [ ] **Step 2: 检查测试覆盖率**

Run: `./gradlew :cartisan-web:test jacocoTestReport`
Expected: 覆盖率 ≥ 80%

- [ ] **Step 3: 手动验证（可选）**

创建一个简单的 Spring Boot 应用验证功能：
1. 启动应用
2. 访问 `http://localhost:8080/api/enums/TestUserStatus`
3. 验证返回正确的 JSON

- [ ] **Step 4: 提交**

如果测试全部通过，标记版本：

```bash
git tag -a v0.9.0 -m "Release v0.9.0: Add enum option support"
git push origin v0.9.0
```

---

## 完成检查清单

- [ ] 所有测试通过
- [ ] 代码符合项目规范（ArchUnit 规则通过）
- [ ] 文档已更新
- [ ] Git 提交信息清晰
- [ ] 无 TODO 或 FIXME 注释遗留
- [ ] 代码已通过 Code Review（如适用）

---

**实现计划完成！** 🎉
