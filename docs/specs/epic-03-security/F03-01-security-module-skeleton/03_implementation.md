# F03-01 cartisan-security 模块骨架 — 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 建立 cartisan-security 模块骨架，配置依赖，创建包结构，验证模块可构建和测试。

**Architecture:** 这是一个无代码 Feature（纯配置 + 骨架）。修改 cartisan-dependencies BOM 添加 Sa-Token 版本约束，新建 cartisan-security 子模块，建立四个核心子包的目录结构，创建占位测试类验证构建环境。

**Tech Stack:** Gradle Kotlin DSL、Java 21、JUnit 5、AssertJ、Sa-Token 1.45.0

---

## Task 1: 修改 cartisan-dependencies BOM

**Files:**
- Modify: `cartisan-dependencies/build.gradle.kts`

**Step 1: 添加 Sa-Token 版本约束**

在 `dependencies` 块中添加 Sa-Token 声明：

```kotlin
plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    // Spring Boot BOM - manages all Spring Boot starter versions
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))

    // 现有约束（如有）...

    // Sa-Token（F03-01）- 直接用 api() 声明带版本约束
    api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")
}
```

**Step 2: 验证 BOM 可编译**

Run: `./gradlew :cartisan-dependencies:build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add cartisan-dependencies/build.gradle.kts
git commit -m "feat(dependencies): add Sa-Token 1.45.0 to BOM (F03-01)"
```

---

## Task 2: 修改 settings.gradle.kts

**Files:**
- Modify: `settings.gradle.kts`

**Step 1: 添加 cartisan-security 子模块**

在文件中添加 `include("cartisan-security")`：

```kotlin
rootProject.name = "cartisan-boot"

include("cartisan-dependencies")
include("cartisan-core")
include("cartisan-test")
include("cartisan-web")
include("cartisan-data-jpa")
include("cartisan-event")
include("cartisan-security")  // F03-01 新增
```

**Step 2: 验证 Gradle 可识别新模块**

Run: `./gradlew projects`
Expected: 输出中包含 `Root project 'cartisan-boot'` 和子模块列表，包括 `cartisan-security`

**Step 3: Commit**

```bash
git add settings.gradle.kts
git commit -m "feat: include cartisan-security module (F03-01)"
```

---

## Task 3: 创建 cartisan-security 模块目录结构

**Files:**
- Create: `cartisan-security/build.gradle.kts`
- Create: `cartisan-security/src/main/java/com/cartisan/security/`
- Create: `cartisan-security/src/test/java/com/cartisan/security/`

**Step 1: 创建目录**

Run:
```bash
mkdir -p cartisan-security/src/main/java/com/cartisan/security
mkdir -p cartisan-security/src/main/java/com/cartisan/security/annotation
mkdir -p cartisan-security/src/main/java/com/cartisan/security/context
mkdir -p cartisan-security/src/main/java/com/cartisan/security/authentication
mkdir -p cartisan-security/src/main/java/com/cartisan/security/config
mkdir -p cartisan-security/src/test/java/com/cartisan/security
```

**Step 2: 验证目录创建**

Run: `find cartisan-security -type d`
Expected: 显示所有新创建的目录

**Step 3: Commit**

```bash
git add cartisan-security/
git commit -m "feat: create cartisan-security module directory structure (F03-01)"
```

---

## Task 4: 创建 cartisan-security/build.gradle.kts

**Files:**
- Create: `cartisan-security/build.gradle.kts`

**Step 1: 创建构建配置文件**

创建 `cartisan-security/build.gradle.kts`：

```kotlin
plugins {
    java
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    // Platform - versions managed by cartisan-dependencies
    api(platform(project(":cartisan-dependencies")))

    // 依赖 cartisan-core（基础类型）
    api(project(":cartisan-core"))

    // 依赖 cartisan-web（ApiResponse、GlobalExceptionHandler、RequestContext）
    api(project(":cartisan-web"))

    // Sa-Token（版本由 BOM 管理，不写版本号）
    implementation("cn.dev33:sa-token-spring-boot3-starter")

    // 测试依赖（spring-boot-starter-test 已包含 JUnit 5 + AssertJ）
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
```

**Step 2: 验证模块可编译**

Run: `./gradlew :cartisan-security:compileJava`
Expected: BUILD SUCCESSFUL（可能没有源文件，但编译配置正确）

**Step 3: Commit**

```bash
git add cartisan-security/build.gradle.kts
git commit -m "feat: add cartisan-security build configuration (F03-01)"
```

---

## Task 5: 创建根包 package-info.java

**Files:**
- Create: `cartisan-security/src/main/java/com/cartisan/security/package-info.java`

**Step 1: 创建文件**

创建 `cartisan-security/src/main/java/com/cartisan/security/package-info.java`：

```java
/**
 * cartisan-security 模块根包。
 *
 * <p>提供认证授权薄抽象层 + 多租户上下文基础设施，底层实现可替换（Sa-Token）。</p>
 */
package com.cartisan.security;
```

**Step 2: 验证编译**

Run: `./gradlew :cartisan-security:compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/package-info.java
git commit -m "feat: add security root package-info (F03-01)"
```

---

## Task 6: 创建 annotation/package-info.java

**Files:**
- Create: `cartisan-security/src/main/java/com/cartisan/security/annotation/package-info.java`

**Step 1: 创建文件**

创建 `cartisan-security/src/main/java/com/cartisan/security/annotation/package-info.java`：

```java
/**
 * 权限与多租户相关注解。
 *
 * <p>包含 {@code @RequireAuth}、{@code @RequireRole}、{@code @RequirePermission} 等注解，
 * 用于声明式的访问控制。</p>
 */
package com.cartisan.security.annotation;
```

**Step 2: 验证编译**

Run: `./gradlew :cartisan-security:compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/annotation/package-info.java
git commit -m "feat: add annotation package-info (F03-01)"
```

---

## Task 7: 创建 context/package-info.java

**Files:**
- Create: `cartisan-security/src/main/java/com/cartisan/security/context/package-info.java`

**Step 1: 创建文件**

创建 `cartisan-security/src/main/java/com/cartisan/security/context/package-info.java`：

```java
/**
 * 安全与多租户上下文。
 *
 * <p>包含 {@code SecurityContext}（当前用户上下文）和
 * {@code TenantContext}（多租户上下文）。</p>
 */
package com.cartisan.security.context;
```

**Step 2: 验证编译**

Run: `./gradlew :cartisan-security:compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/context/package-info.java
git commit -m "feat: add context package-info (F03-01)"
```

---

## Task 8: 创建 authentication/package-info.java

**Files:**
- Create: `cartisan-security/src/main/java/com/cartisan/security/authentication/package-info.java`

**Step 1: 创建文件**

创建 `cartisan-security/src/main/java/com/cartisan/security/authentication/package-info.java`：

```java
/**
 * 认证服务抽象。
 *
 * <p>包含 {@code AuthenticationService} 接口及其 Sa-Token 实现，
 * 提供登录、登出、Token 信息查询等功能。</p>
 */
package com.cartisan.security.authentication;
```

**Step 2: 验证编译**

Run: `./gradlew :cartisan-security:compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/authentication/package-info.java
git commit -m "feat: add authentication package-info (F03-01)"
```

---

## Task 9: 创建 config/package-info.java

**Files:**
- Create: `cartisan-security/src/main/java/com/cartisan/security/config/package-info.java`

**Step 1: 创建文件**

创建 `cartisan-security/src/main/java/com/cartisan/security/config/package-info.java`：

```java
/**
 * 自动配置与拦截器配置。
 *
 * <p>包含 Spring Boot AutoConfiguration、MVC 拦截器、Filter 等配置组件。</p>
 */
package com.cartisan.security.config;
```

**Step 2: 验证编译**

Run: `./gradlew :cartisan-security:compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/config/package-info.java
git commit -m "feat: add config package-info (F03-01)"
```

---

## Task 10: 创建占位测试类

**Files:**
- Create: `cartisan-security/src/test/java/com/cartisan/security/CartisanSecurityModuleTest.java`

**Step 1: 创建测试类**

创建 `cartisan-security/src/test/java/com/cartisan/security/CartisanSecurityModuleTest.java`：

```java
package com.cartisan.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * cartisan-security 模块占位测试类。
 *
 * <p>用于验证模块可正确构建且测试环境可用。</p>
 * <p>后续 Feature 将在此包下添加真实测试用例。</p>
 */
class CartisanSecurityModuleTest {

    @Test
    void moduleLoads() {
        // 占位测试：验证模块能构建且测试可执行
        assertThat(1).isEqualTo(1);
    }
}
```

**Step 2: 验证测试通过**

Run: `./gradlew :cartisan-security:test`
Expected: BUILD SUCCESSFUL，测试通过

**Step 3: Commit**

```bash
git add cartisan-security/src/test/java/com/cartisan/security/CartisanSecurityModuleTest.java
git commit -m "feat: add placeholder test class (F03-01)"
```

---

## Task 11: 最终验证

**Step 1: 验证模块可构建**

Run: `./gradlew :cartisan-security:build`
Expected: BUILD SUCCESSFUL

**Step 2: 验证测试通过**

Run: `./gradlew :cartisan-security:test`
Expected: BUILD SUCCESSFUL，测试通过

**Step 3: 验证目录结构**

Run: `find cartisan-security/src -name "*.java"`
Expected: 输出包含 5 个 package-info.java 和 1 个测试类

**Step 4: 验证 settings.gradle.kts**

Run: `grep "cartisan-security" settings.gradle.kts`
Expected: 输出 `include("cartisan-security")`

**Step 5: 验证 BOM 配置**

Run: `grep "sa-token" cartisan-dependencies/build.gradle.kts`
Expected: 输出包含 `api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")`

---

## 验收检查清单

| # | 验收项 | 状态 |
|---|--------|------|
| AC1 | settings.gradle.kts 包含 `include("cartisan-security")` | ⬜ |
| AC2 | cartisan-dependencies BOM 声明 Sa-Token 1.45.0 | ⬜ |
| AC3 | cartisan-security/build.gradle.kts 配置正确 | ⬜ |
| AC4 | 四个子包 + 根包有 package-info.java | ⬜ |
| AC5 | src/test/java 目录 + CartisanSecurityModuleTest 存在 | ⬜ |
| AC6 | `./gradlew :cartisan-security:build` 成功 | ⬜ |
| AC7 | `./gradlew :cartisan-security:test` 成功 | ⬜ |

---

## 完成后的后续步骤

1. 所有 AC 验证通过后，在 `docs/specs/epic-03-security/F03-01-security-module-skeleton/03_implementation.md` 中标记每个 Task 为 ✅
2. 进入 Phase 5: Review，交叉审查代码变更
3. 归档 04_test_spec.md
