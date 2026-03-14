# Feature: F02-09 AutoConfiguration — 实施计划

> **Phase**: 3 — Plan
> **定位**: 原子任务清单，每步 50-150 行代码

---

## 目标复述

为 cartisan-web、cartisan-data-jpa、cartisan-event 三个模块创建 Spring Boot AutoConfiguration：
- cartisan-web：新建配置类 + 修改 RequestContextFilter + 创建 imports 文件
- cartisan-data-jpa：增强现有配置，导入 JPA Auditing
- cartisan-event：修改注解类型

实现"引入即用"：用户添加依赖后，框架组件自动注册，无需 `@ComponentScan`。

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| Create | `cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java` | Web 模块自动配置类 |
| Create | `cartisan-web/src/main/resources/META-INF/spring/...AutoConfiguration.imports` | 声明自动配置 |
| Modify | `cartisan-web/src/main/java/com/cartisan/web/context/RequestContextFilter.java` | 移除 @Component，实现 Ordered |
| Modify | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java` | 添加 @Import |
| Modify | `cartisan-event/src/main/java/com/cartisan/event/config/CartisanEventAutoConfiguration.java` | @Configuration → @AutoConfiguration |

---

## 核心流程（伪代码）

```
1. 创建 CartisanWebAutoConfiguration
   ├── @AutoConfiguration
   ├── @ConditionalOnWebApplication
   └── 注册 RequestContextFilter、GlobalExceptionHandler

2. 修改 RequestContextFilter
   ├── 移除 @Component("cartisanRequestContextFilter")
   ├── 移除 @Order(Ordered.HIGHEST_PRECEDENCE)
   └── 实现 Ordered 接口，getOrder() 返回 HIGHEST_PRECEDENCE

3. 增强 CartisanDataJpaAutoConfiguration
   └── 添加 @Import(JpaAuditingConfiguration.class)

4. 修改 CartisanEventAutoConfiguration
   └── @Configuration 改为 @AutoConfiguration

5. 验证
   ├── 编译通过
   ├── 测试通过
   └── ArchUnit 通过
```

---

## 原子任务清单

### Step 1: cartisan-web — 创建 CartisanWebAutoConfiguration

**文件**：`cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java`

**内容**：将 02_interface.md 中的伪代码转为 Java 源代码

```java
package com.cartisan.web.config;

import com.cartisan.web.context.RequestContextFilter;
import com.cartisan.web.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * cartisan-web 模块的 Spring Boot 自动配置。
 *
 * <p>注册 Web 层核心组件：
 * <ul>
 *   <li>{@link RequestContextFilter} — 请求上下文初始化</li>
 *   <li>{@link GlobalExceptionHandler} — 全局异常处理</li>
 * </ul>
 *
 * <p><strong>引入即用</strong>：添加 cartisan-web 依赖后，无需 {@code @ComponentScan}，
 * 这些组件会自动注册。</p>
 *
 * <h3>条件装配</h3>
 * <p>仅在 Web 应用环境中生效（非 Web 应用如批处理不需要这些组件）。</p>
 *
 * <h3>用户覆盖</h3>
 * <p>核心组件强制注册，不使用 {@code @ConditionalOnMissingBean}。
 * 用户需要替换时，通过排除 AutoConfiguration 或显式注册自定义 Bean 处理。</p>
 *
 * @since 0.2.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class CartisanWebAutoConfiguration {

    /**
     * 注册请求上下文 Filter。
     *
     * <p>Bean 名称使用 {@code cartisanRequestContextFilter}，与之前 {@code @Component} 注解时的名称一致，
     * 保持向后兼容。</p>
     *
     * @return RequestContextFilter 实例
     */
    @Bean("cartisanRequestContextFilter")
    public RequestContextFilter requestContextFilter() {
        return new RequestContextFilter();
    }

    /**
     * 注册全局异常处理器。
     *
     * <p>{@link GlobalExceptionHandler} 类本身保留 {@code @ControllerAdvice} 注解，
     * 这是 Spring MVC 识别异常处理器的必要注解。</p>
     *
     * @return GlobalExceptionHandler 实例
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
```

**验证**：编译通过

```bash
./gradlew :cartisan-web:compileJava
```

---

### Step 2: cartisan-web — 创建 AutoConfiguration.imports

**文件**：`cartisan-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**内容**：
```
com.cartisan.web.config.CartisanWebAutoConfiguration
```

**验证**：文件存在且内容正确

---

### Step 3: cartisan-web — 修改 RequestContextFilter

**文件**：`cartisan-web/src/main/java/com/cartisan/web/context/RequestContextFilter.java`

**变更**：
1. 移除 `@Component("cartisanRequestContextFilter")` 注解
2. 移除 `@Order(Ordered.HIGHEST_PRECEDENCE)` 注解
3. 类声明添加 `implements Ordered`
4. 添加 `getOrder()` 方法

**修改前**：
```java
@Component("cartisanRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {
    // ...
}
```

**修改后**：
```java
public class RequestContextFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // ... 其余代码保持不变
}
```

**验证**：编译通过

```bash
./gradlew :cartisan-web:compileJava
```

---

### Step 4: cartisan-data-jpa — 增强 CartisanDataJpaAutoConfiguration

**文件**：`cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java`

**变更**：添加 `@Import(JpaAuditingConfiguration.class)`

**修改前**：
```java
@AutoConfiguration
public class CartisanDataJpaAutoConfiguration {
    // ...
}
```

**修改后**：
```java
@AutoConfiguration
@Import(JpaAuditingConfiguration.class)
public class CartisanDataJpaAutoConfiguration {
    // ...
}
```

同时确保有 import：
```java
import org.springframework.context.annotation.Import;
```

**验证**：编译通过

```bash
./gradlew :cartisan-data-jpa:compileJava
```

---

### Step 5: cartisan-event — 修改 CartisanEventAutoConfiguration 注解

**文件**：`cartisan-event/src/main/java/com/cartisan/event/config/CartisanEventAutoConfiguration.java`

**变更**：`@Configuration` → `@AutoConfiguration`

**修改前**：
```java
@Configuration
@ConditionalOnMissingBean(DomainEventPublisher.class)
public class CartisanEventAutoConfiguration {
    // ...
}
```

**修改后**：
```java
@AutoConfiguration
@ConditionalOnMissingBean(DomainEventPublisher.class)
public class CartisanEventAutoConfiguration {
    // ...
}
```

同时更新 import：
```java
// 删除（如果有）
import org.springframework.context.annotation.Configuration;

// 确保有
import org.springframework.boot.autoconfigure.AutoConfiguration;
```

**验证**：编译通过

```bash
./gradlew :cartisan-event:compileJava
```

---

### Step 6: 全量编译验证

**验证**：所有模块编译通过

```bash
./gradlew compileJava
```

**Expected**: BUILD SUCCESSFUL

---

### Step 7: 全量测试验证

**验证**：所有测试通过

```bash
./gradlew test
```

**Expected**: BUILD SUCCESSFUL

---

### Step 8: ArchUnit 验证

**验证**：架构规则通过

```bash
./gradlew check
```

**Expected**: BUILD SUCCESSFUL

---

## 验收检查清单

完成所有 Step 后，逐项检查：

- [ ] `CartisanWebAutoConfiguration.java` 已创建
- [ ] cartisan-web 的 `AutoConfiguration.imports` 已创建
- [ ] `RequestContextFilter` 移除 `@Component`，实现 `Ordered`
- [ ] `CartisanDataJpaAutoConfiguration` 添加 `@Import(JpaAuditingConfiguration.class)`
- [ ] `CartisanEventAutoConfiguration` 使用 `@AutoConfiguration`
- [ ] `./gradlew compileJava` 成功
- [ ] `./gradlew test` 成功
- [ ] `./gradlew check` 成功
