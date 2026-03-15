# Feature: F03-07 自动配置 — 实施计划

> **Phase**: Plan — 实施计划与任务拆解
> **依赖**: F03-01 至 F03-06（所有前置 Feature 已完成）

---

## 目标复述

创建 cartisan-security 模块的 Spring Boot AutoConfiguration，实现零配置引入：
- 配置属性类支持拦截器路径可配置（默认 `/**`，排除 `/error`、`/actuator/**`）
- 主配置类在 Web 应用 + Sa-Token 存在时自动生效
- 拦截器配置类将已有的 `SecurityInterceptor` 注册到 MVC 拦截器链
- 通过 `META-INF/spring/...AutoConfiguration.imports` 声明自动配置入口

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| **创建** | `cartisan-security/src/main/java/com/cartisan/security/config/properties/CartisanSecurityProperties.java` | 配置属性类 |
| **创建** | `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java` | 主配置类 |
| **创建** | `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptorConfig.java` | 拦截器配置类 |
| **创建** | `cartisan-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 自动配置声明 |
| **创建** | `cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityPropertiesTest.java` | 配置属性测试 |
| **创建** | `cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorConfigTest.java` | 拦截器配置测试 |
| **创建** | `cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityAutoConfigurationTest.java` | 自动配置集成测试 |

---

## 核心流程（伪代码）

```
应用启动时：
  1. Spring Boot 扫描 AutoConfiguration.imports
  2. 发现 CartisanSecurityAutoConfiguration
  3. 检查条件：
     - @ConditionalOnWebApplication → 是 Web 应用？
     - @ConditionalOnClass(StpUtil.class) → classpath 有 Sa-Token？
     - 任一不满足 → 跳过
  4. 条件满足：
     - 创建 CartisanSecurityProperties Bean
     - 加载 SecurityInterceptorConfig
  5. SecurityInterceptorConfig：
     - 检查 @ConditionalOnBean(SecurityInterceptor.class)
     - 注入 SecurityInterceptor（已有的 @Component）
     - 注入 CartisanSecurityProperties
     - 在 addInterceptors 中注册拦截器
  6. 拦截器生效，按 path-patterns 和 exclude-path-patterns 配置拦截请求
```

---

## 原子任务清单

### Step 1: 创建 CartisanSecurityProperties 配置属性类

**文件**: `cartisan-security/src/main/java/com/cartisan/security/config/properties/CartisanSecurityProperties.java`

**内容**:
```java
package com.cartisan.security.config.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * cartisan-security 拦截器配置属性。
 * <p>
 * 支持配置拦截器生效的路径模式和排除路径模式。
 * </p>
 *
 * @see com.cartisan.security.config.SecurityInterceptorConfig
 */
public class CartisanSecurityProperties {

    /**
     * 拦截器生效的路径模式（Ant 风格）。
     * <p>
     * 默认拦截所有路径 {@code /**}，由拦截器内部根据注解决定是否鉴权。
     * </p>
     */
    private List<String> pathPatterns = new ArrayList<>(List.of("/**"));

    /**
     * 排除的路径模式（Ant 风格）。
     * <p>
     * 默认排除错误页和 Actuator 端点，避免对系统路径做无意义拦截。
     * </p>
     */
    private List<String> excludePathPatterns = new ArrayList<>(List.of(
        "/error",
        "/actuator/**"
    ));

    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    public void setPathPatterns(List<String> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    public List<String> getExcludePathPatterns() {
        return excludePathPatterns;
    }

    public void setExcludePathPatterns(List<String> excludePathPatterns) {
        this.excludePathPatterns = excludePathPatterns;
    }
}
```

**验证**: 编译通过 `./gradlew :cartisan-security:compileJava`

---

### Step 2: 编写 CartisanSecurityProperties 单元测试（红灯）

**文件**: `cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityPropertiesTest.java`

**内容**:
```java
package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartisanSecurityPropertiesTest {

    @Test
    void given_newInstance_when_getPathPatterns_then_returnDefault() {
        CartisanSecurityProperties properties = new CartisanSecurityProperties();

        assertThat(properties.getPathPatterns()).containsExactly("/**");
    }

    @Test
    void given_newInstance_when_getExcludePathPatterns_then_returnDefault() {
        CartisanSecurityProperties properties = new CartisanSecurityProperties();

        assertThat(properties.getExcludePathPatterns()).containsExactly("/error", "/actuator/**");
    }

    @Test
    void given_setPathPatterns_when_getPathPatterns_then_returnCustom() {
        CartisanSecurityProperties properties = new CartisanSecurityProperties();
        properties.setPathPatterns(List.of("/api/**", "/admin/**"));

        assertThat(properties.getPathPatterns()).containsExactly("/api/**", "/admin/**");
    }

    @Test
    void given_setExcludePathPatterns_when_getExcludePathPatterns_then_returnCustom() {
        CartisanSecurityProperties properties = new CartisanSecurityProperties();
        properties.setExcludePathPatterns(List.of("/api/public/**"));

        assertThat(properties.getExcludePathPatterns()).containsExactly("/api/public/**");
    }
}
```

**验证**:
```bash
./gradlew :cartisan-security:test --tests CartisanSecurityPropertiesTest
# 预期：测试全绿（Step 1 已实现，可直接验证）
```

---

### Step 3: 创建 SecurityInterceptorConfig 拦截器配置类

**文件**: `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptorConfig.java`

**内容**:
```java
package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC 拦截器配置，将 {@link SecurityInterceptor} 注册到拦截器链。
 * <p>
 * 仅在 {@link SecurityInterceptor} Bean 存在时生效，由自动配置类导入。
 * </p>
 */
@ConditionalOnBean(SecurityInterceptor.class)
public class SecurityInterceptorConfig implements WebMvcConfigurer {

    private final SecurityInterceptor securityInterceptor;
    private final CartisanSecurityProperties properties;

    /**
     * 构造器注入依赖。
     *
     * @param securityInterceptor 已有的 SecurityInterceptor Bean（由 @Component 扫描创建）
     * @param properties 配置属性
     */
    public SecurityInterceptorConfig(SecurityInterceptor securityInterceptor,
                                      CartisanSecurityProperties properties) {
        this.securityInterceptor = securityInterceptor;
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> pathPatterns = properties.getPathPatterns();
        List<String> excludePathPatterns = properties.getExcludePathPatterns();

        registry.addInterceptor(securityInterceptor)
            .addPathPatterns(pathPatterns.toArray(new String[0]))
            .excludePathPatterns(excludePathPatterns.toArray(new String[0]));
    }
}
```

**验证**: 编译通过

---

### Step 4: 编写 SecurityInterceptorConfig 单元测试（红灯）

**文件**: `cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorConfigTest.java`

**内容**:
```java
package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityInterceptorConfigTest {

    private SecurityInterceptor mockInterceptor;
    private CartisanSecurityProperties properties;
    private SecurityInterceptorConfig config;
    private InterceptorRegistry mockRegistry;

    @BeforeEach
    void setUp() {
        mockInterceptor = mock(SecurityInterceptor.class);
        properties = new CartisanSecurityProperties();
        config = new SecurityInterceptorConfig(mockInterceptor, properties);
        mockRegistry = mock(InterceptorRegistry.class);
    }

    @Test
    void given_defaultProperties_when_addInterceptors_then_registerWithDefaults() {
        // 当
        config.addInterceptors(mockRegistry);

        // 那么
        verify(mockRegistry).addInterceptor(mockInterceptor);
        verify(mockRegistry).addInterceptor(eq(mockInterceptor), any())
            .addPathPatterns("/**")
            .excludePathPatterns("/error", "/actuator/**");
    }

    @Test
    void given_customPathPatterns_when_addInterceptors_then_registerWithCustomPaths() {
        // 给定
        properties.setPathPatterns(List.of("/api/**", "/admin/**"));
        properties.setExcludePathPatterns(List.of("/api/public/**"));

        // 当
        config.addInterceptors(mockRegistry);

        // 那么
        verify(mockRegistry).addInterceptor(mockInterceptor);
        verify(mockRegistry).addInterceptor(eq(mockInterceptor), any())
            .addPathPatterns("/api/**", "/admin/**")
            .excludePathPatterns("/api/public/**");
    }
}
```

**验证**:
```bash
./gradlew :cartisan-security:test --tests SecurityInterceptorConfigTest
# 预期：测试全绿（Step 3 已实现）
```

---

### Step 5: 创建 CartisanSecurityAutoConfiguration 主配置类

**文件**: `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`

**内容**:
```java
package com.cartisan.security.config;

import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.springframework.boot.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * cartisan-security 自动配置主类。
 * <p>
 * 零配置引入 cartisan-security 模块的入口类。
 * </p>
 *
 * <h3>条件装配</h3>
 * <ul>
 *   <li>Web 应用（@ConditionalOnWebApplication）</li>
 *   <li>classpath 存在 Sa-Token（@ConditionalOnClass(StpUtil.class)）</li>
 * </ul>
 *
 * <h3>配置项</h3>
 * <pre>
 * cartisan:
 *   security:
 *     interceptor:
 *       path-patterns: ["/**"]
 *       exclude-path-patterns: ["/error", "/actuator/**"]
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(StpUtil.class)
@EnableConfigurationProperties(CartisanSecurityProperties.class)
@Import(SecurityInterceptorConfig.class)
public class CartisanSecurityAutoConfiguration {
    // 主类只负责模块级条件和导入，具体配置由 SecurityInterceptorConfig 处理
}
```

**验证**: 编译通过

---

### Step 6: 创建 AutoConfiguration.imports 文件

**文件**: `cartisan-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**内容**:
```
com.cartisan.security.config.CartisanSecurityAutoConfiguration
```

**验证**: 文件存在于正确路径

```bash
ls -la cartisan-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

### Step 7: 编写自动配置集成测试

**文件**: `cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityAutoConfigurationTest.java`

**内容**:
```java
package com.cartisan.security.config;

import com.cartisan.security.config.properties.CartisanSecurityProperties;
import com.cartisan.security.context.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
    SecurityInterceptor.class,
    SecurityInterceptorConfig.class,
    CartisanSecurityAutoConfiguration.class,
    SecurityContext.class
})
@TestPropertySource(properties = {
    "cartisan.security.interceptor.path-patterns=/api/**",
    "cartisan.security.interceptor.exclude-path-patterns=/api/public/**"
})
class CartisanSecurityAutoConfigurationTest {

    @Autowired(required = false)
    private CartisanSecurityProperties properties;

    @Autowired(required = false)
    private SecurityInterceptor securityInterceptor;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void given_context_when_getProperties_then_loadedWithCustomConfig() {
        assertThat(properties).isNotNull();
        assertThat(properties.getPathPatterns()).containsExactly("/api/**");
        assertThat(properties.getExcludePathPatterns()).containsExactly("/api/public/**");
    }

    @Test
    void given_context_when_getSecurityInterceptor_then_exists() {
        assertThat(securityInterceptor).isNotNull();
    }

    @Test
    void given_context_when_getSecurityInterceptorConfig_then_exists() {
        SecurityInterceptorConfig config = applicationContext.getBean(SecurityInterceptorConfig.class);
        assertThat(config).isNotNull();
    }

    @Test
    void given_config_when_addInterceptors_then_registryConfigured() {
        SecurityInterceptorConfig config = applicationContext.getBean(SecurityInterceptorConfig.class);
        InterceptorRegistry registry = new InterceptorRegistry();

        config.addInterceptors(registry);

        assertThat(registry.getInterceptors()).hasSize(1);
    }
}
```

**验证**:
```bash
./gradlew :cartisan-security:test --tests CartisanSecurityAutoConfigurationTest
```

---

### Step 8: 全量验证

```bash
# 编译
./gradlew :cartisan-security:compileJava

# 运行所有测试
./gradlew :cartisan-security:test

# 构建模块
./gradlew :cartisan-security:build
```

**预期**:
- 编译通过
- 所有测试绿灯
- 构建成功

---

### Step 9: 更新进度

在 `03_implementation.md` 中标记完成状态：

```markdown
### Step 1: 创建 CartisanSecurityProperties 配置属性类 ✅
### Step 2: 编写 CartisanSecurityProperties 单元测试 ✅
### Step 3: 创建 SecurityInterceptorConfig 拦截器配置类 ✅
### Step 4: 编写 SecurityInterceptorConfig 单元测试 ✅
### Step 5: 创建 CartisanSecurityAutoConfiguration 主配置类 ✅
### Step 6: 创建 AutoConfiguration.imports 文件 ✅
### Step 7: 编写自动配置集成测试 ✅
### Step 8: 全量验证 ✅
```

---

## 执行状态

**Phase 4 (Execute) 完成**: 2026-03-15

- ✅ Step 1: 创建 CartisanSecurityProperties 配置属性类
- ✅ Step 2: 编写 CartisanSecurityProperties 单元测试
- ✅ Step 3: 创建 SecurityInterceptorConfig 拦截器配置类
- ✅ Step 4: 编写 SecurityInterceptorConfig 单元测试
- ✅ Step 5: 创建 CartisanSecurityAutoConfiguration 主配置类
- ✅ Step 6: 创建 AutoConfiguration.imports 文件
- ✅ Step 7: 编写自动配置集成测试
- ✅ Step 8: 全量验证

**Phase 5 (Review & Archive) 完成**: 2026-03-15

- ✅ 代码审查 - 使用 superpowers:code-reviewer
- ✅ 验证测试 - 编译/测试/构建全部通过
- ✅ 缺陷修复 - 添加 @Configuration 注解到 SecurityInterceptorConfig
- ✅ 验收标准检查 - 7/7 通过

**修复记录**:
- Phase 4: 添加 `@ConfigurationProperties` 注解到 CartisanSecurityProperties
- Phase 4: 测试中移除 SecurityContext.class（工具类不应作为 Bean）
- Phase 5: 添加 `@Configuration` 注解到 SecurityInterceptorConfig（代码审查发现）

---

## 复杂度评估

| Step | 预估行数 | 复杂度 |
|------|---------|--------|
| Step 1 | ~50 行 | S |
| Step 2 | ~40 行 | S |
| Step 3 | ~50 行 | S |
| Step 4 | ~60 行 | S |
| Step 5 | ~50 行 | S |
| Step 6 | 1 行 | S |
| Step 7 | ~70 行 | M |
| Step 8 | - | - |

**总计**: 约 320 行，符合复杂度 M（80-200 行范围，多文件可适当放宽）

---

## 依赖关系

```
Step 1 (Properties 类)
    ↓
Step 2 (Properties 测试)
    ↓
Step 3 (Config 类，依赖 Properties)
    ↓
Step 4 (Config 测试)
    ↓
Step 5 (主配置类，依赖 Config)
    ↓
Step 6 (imports 文件)
    ↓
Step 7 (集成测试)
    ↓
Step 8 (全量验证)
```

---

## 技术约定

- 使用 Java 21 特性（如 `List.of()`）
- 配置属性使用可变 `ArrayList` 确保绑定兼容
- 构造器注入依赖，不使用 `@Autowired` 字段注入
- `@ConditionalOnBean` 确保只在 Bean 存在时加载配置

---

## 参考文档

- 需求规格: [01_requirement.md](01_requirement.md)
- 接口契约: [02_interface.md](02_interface.md)
- Spring Boot AutoConfiguration: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration
