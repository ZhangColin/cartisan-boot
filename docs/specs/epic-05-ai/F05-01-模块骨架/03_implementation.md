# F05-01 cartisan-ai 模块骨架 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 cartisan-boot Gradle 多模块项目中注册 `cartisan-ai` 子模块，建立构建配置和包结构骨架。

**Architecture:** 纯配置 Feature，无业务代码。修改 `settings.gradle.kts` 注册模块，新建 `build.gradle.kts` 声明依赖，新建 5 个 `package-info.java` 建立包结构。

**Tech Stack:** Gradle Kotlin DSL、Java 21、Spring Boot 3.4.x

---

## 目标复述

- 注册 `cartisan-ai` 为 Gradle 子模块
- 声明依赖：`cartisan-core`（api）、`spring-webflux`（api）、`spring-boot-starter`（implementation）
- 建立 4 个子包：`model`、`provider`、`sse`、`config`，每个包含 `package-info.java`

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `settings.gradle.kts` | 追加 `include("cartisan-ai")` |
| 新建 | `cartisan-ai/build.gradle.kts` | 模块构建配置 |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/package-info.java` | 根包 |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/model/package-info.java` | 统一对话模型包 |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/provider/package-info.java` | Provider SPI 包 |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/sse/package-info.java` | SSE 流式工具包 |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/config/package-info.java` | 自动配置包 |

## 原子任务清单

### Step 1: 注册子模块

**Files:**
- Modify: `settings.gradle.kts`

- [ ] **1.1 追加 include**

在 `settings.gradle.kts` 末尾追加：

```kotlin
include("cartisan-ai")
```

- [ ] **1.2 验证模块注册**

```bash
./gradlew projects | grep cartisan-ai
```

期望输出包含：`--- Project ':cartisan-ai'`

---

### Step 2: 创建 build.gradle.kts ✅

**Files:**
- Create: `cartisan-ai/build.gradle.kts`

- [ ] **2.1 新建构建配置文件**

```kotlin
plugins { java }

dependencies {
    // 版本 BOM：cartisan-dependencies 内嵌 spring-boot-dependencies，
    // spring-webflux 等 Spring 组件版本均由此管理，无需手写版本号
    api(platform(project(":cartisan-dependencies")))

    // CartisanException 等基础类型出现在公开 API，下游可见
    api(project(":cartisan-core"))

    // Flux<ChatStreamEvent> 是公开 SPI 返回类型，下游可见；
    // 选 spring-webflux 而非 spring-boot-starter-webflux，避免引入 Netty 嵌入式服务器
    api("org.springframework:spring-webflux")

    // AutoConfiguration 基础设施，内部使用
    implementation("org.springframework.boot:spring-boot-starter")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
```

- [ ] **2.2 编译验证**

```bash
./gradlew :cartisan-ai:compileJava
```

期望：`BUILD SUCCESSFUL`

---

### Step 3: 创建包结构（5 个 package-info.java）

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/package-info.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/model/package-info.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/provider/package-info.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/sse/package-info.java`
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/config/package-info.java`

- [ ] **3.1 根包**

```java
/**
 * Cartisan AI — 大模型调用统一抽象模块
 *
 * <p>本模块提供多 Provider 的统一对话接口（同步 + 流式 SSE），包括：</p>
 * <ul>
 *   <li>{@code model} — 统一对话模型（ChatMessage、ChatRequest、ChatResponse 等）</li>
 *   <li>{@code provider} — Provider SPI 及 OpenAI/Anthropic/DeepSeek 实现</li>
 *   <li>{@code sse} — SSE 流式工具（Flux → SseEmitter）</li>
 *   <li>{@code config} — Spring Boot 自动配置</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.ai;
```

- [ ] **3.2 model 包**

```java
/**
 * 统一对话模型
 *
 * <p>与具体 Provider 无关的数据类型，包括：</p>
 * <ul>
 *   <li>{@code ChatMessage} — 单条消息（role + content）</li>
 *   <li>{@code ChatRequest} — 对话请求</li>
 *   <li>{@code ChatResponse} — 同步响应</li>
 *   <li>{@code ChatStreamEvent} — 流式事件（delta + finished + usage）</li>
 *   <li>{@code TokenUsage} — Token 用量统计</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.ai.model;
```

- [ ] **3.3 provider 包**

```java
/**
 * Provider SPI 及各实现
 *
 * <p>包括：</p>
 * <ul>
 *   <li>{@code ModelProvider} — 统一调用接口（chat / chatStream）</li>
 *   <li>{@code ModelProviderRegistry} — 按 providerId / modelName 查找 Provider</li>
 *   <li>{@code OpenAiProvider}、{@code DeepSeekProvider}、{@code AnthropicProvider} — 各 Provider 实现</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.ai.provider;
```

- [ ] **3.4 sse 包**

```java
/**
 * SSE 流式工具
 *
 * <p>将 {@code Flux<ChatStreamEvent>} 转换为 Spring MVC {@code SseEmitter}，
 * 封装超时、异常和客户端断开处理。</p>
 *
 * @since 0.1.0
 */
package com.cartisan.ai.sse;
```

- [ ] **3.5 config 包**

```java
/**
 * Spring Boot 自动配置
 *
 * <p>通过 {@code @ConditionalOnProperty} 按配置文件中存在的 api-key 条件装配
 * 对应的 Provider Bean 及 {@code ModelProviderRegistry}。</p>
 *
 * @since 0.1.0
 */
package com.cartisan.ai.config;
```

- [ ] **3.6 最终编译验证**

```bash
./gradlew :cartisan-ai:compileJava
```

期望：`BUILD SUCCESSFUL`，无任何 warning 或 error

---

### Step 4: 提交

- [ ] **4.1 暂存并提交**

```bash
git add settings.gradle.kts cartisan-ai/
git commit -m "feat(cartisan-ai): F05-01 模块骨架 — 注册子模块、构建配置、包结构"
```
