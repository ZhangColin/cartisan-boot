# Feature: F03-01 cartisan-security 模块骨架 — 接口契约

> **注意**：本 Feature 为无代码 Feature（纯配置 + 骨架），不涉及业务接口定义。
> 真正的业务接口（注解、Context、Service）在后续 Feature 中定义。

---

## 接口定义

### N/A - 本 Feature 不涉及业务接口

本 Feature 的职责是建立模块骨架和配置依赖，不定义任何业务接口。

以下接口由后续 Feature 定义：
| Feature | 接口/类 |
|---------|---------|
| F03-02 | `@RequireAuth`、`@RequireRole`、`@RequirePermission`、`SecurityInterceptor` |
| F03-03 | `SecurityContext` |
| F03-04 | `TenantContext` |
| F03-05 | `TenantContextFilter` |
| F03-06 | `AuthenticationService`、`SaTokenAuthenticationService`、`TokenInfo` |
| F03-07 | `CartisanSecurityAutoConfiguration` |

---

## 变更范围

### 修改的文件

| 文件 | 变更内容 |
|------|---------|
| `cartisan-dependencies/build.gradle.kts` | 在 `dependencies` 块中添加 `api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")` |
| `settings.gradle.kts` | 添加 `include("cartisan-security")` |

### 新建的文件

| 文件 | 说明 |
|------|------|
| `cartisan-security/build.gradle.kts` | 模块构建配置 |
| `cartisan-security/src/main/java/com/cartisan/security/package-info.java` | 根包说明 |
| `cartisan-security/src/main/java/com/cartisan/security/annotation/package-info.java` | 注解包说明 |
| `cartisan-security/src/main/java/com/cartisan/security/context/package-info.java` | 上下文包说明 |
| `cartisan-security/src/main/java/com/cartisan/security/authentication/package-info.java` | 认证包说明 |
| `cartisan-security/src/main/java/com/cartisan/security/config/package-info.java` | 配置包说明 |
| `cartisan-security/src/test/java/com/cartisan/security/CartisanSecurityModuleTest.java` | 占位测试类 |

---

## 目录结构

```
cartisan-security/
├── build.gradle.kts
└── src/
    ├── main/java/com/cartisan/security/
    │   ├── package-info.java
    │   ├── annotation/
    │   │   └── package-info.java
    │   ├── context/
    │   │   └── package-info.java
    │   ├── authentication/
    │   │   └── package-info.java
    │   └── config/
    │       └── package-info.java
    └── test/java/com/cartisan/security/
        └── CartisanSecurityModuleTest.java
```

---

## 配置文件内容（伪代码）

### cartisan-dependencies/build.gradle.kts（修改）

```kotlin
dependencies {
    // ... 现有内容 ...

    // Sa-Token（F03-01）
    api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")
}
```

### cartisan-security/build.gradle.kts（新建）

```kotlin
plugins {
    java
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    api(platform(project(":cartisan-dependencies")))
    api(project(":cartisan-core"))
    api(project(":cartisan-web"))
    implementation("cn.dev33:sa-token-spring-boot3-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
```

### settings.gradle.kts（修改）

```kotlin
include("cartisan-security")  // 新增
```

---

## package-info.java 内容

### 根包

```java
/**
 * cartisan-security 模块根包。
 *
 * <p>提供认证授权薄抽象层 + 多租户上下文基础设施，底层实现可替换（Sa-Token）。</p>
 */
package com.cartisan.security;
```

### annotation 包

```java
/**
 * 权限与多租户相关注解。
 *
 * <p>包含 {@code @RequireAuth}、{@code @RequireRole}、{@code @RequirePermission} 等注解，
 * 用于声明式的访问控制。</p>
 */
package com.cartisan.security.annotation;
```

### context 包

```java
/**
 * 安全与多租户上下文。
 *
 * <p>包含 {@code SecurityContext}（当前用户上下文）和
 * {@code TenantContext}（多租户上下文）。</p>
 */
package com.cartisan.security.context;
```

### authentication 包

```java
/**
 * 认证服务抽象。
 *
 * <p>包含 {@code AuthenticationService} 接口及其 Sa-Token 实现，
 * 提供登录、登出、Token 信息查询等功能。</p>
 */
package com.cartisan.security.authentication;
```

### config 包

```java
/**
 * 自动配置与拦截器配置。
 *
 * <p>包含 Spring Boot AutoConfiguration、MVC 拦截器、Filter 等配置组件。</p>
 */
package com.cartisan.security.config;
```

---

## 验收方式

### 手动检查清单

| # | 检查项 | 预期结果 |
|---|--------|---------|
| 1 | settings.gradle.kts | 包含 `include("cartisan-security")` |
| 2 | cartisan-dependencies/build.gradle.kts | 包含 Sa-Token 1.45.0 约束 |
| 3 | cartisan-security/build.gradle.kts | 依赖配置正确 |
| 4 | package-info.java（5个） | 全部存在，内容正确 |
| 5 | CartisanSecurityModuleTest.java | 存在，包含占位测试方法 |

### 构建验证命令

| 命令 | 预期结果 |
|------|---------|
| `./gradlew :cartisan-security:build` | BUILD SUCCESSFUL |
| `./gradlew :cartisan-security:test` | BUILD SUCCESSFUL，测试通过 |

---

## 技术决策

本 Feature 无需技术决策，所有配置已在 Epic Backlog 和 01_requirement.md 中明确。
