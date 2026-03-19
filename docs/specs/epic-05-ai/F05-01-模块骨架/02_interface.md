# Feature: F05-01 cartisan-ai 模块骨架 — 接口契约

> 无代码 Feature：本文档描述变更范围与验收方式，无 Java 源码接口。

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `settings.gradle.kts` | 追加 `include("cartisan-ai")` |
| 新建 | `cartisan-ai/build.gradle.kts` | 模块构建配置 |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/package-info.java` | 根包：cartisan-ai 模块说明 |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/model/package-info.java` | 统一对话模型（ChatMessage、ChatRequest 等） |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/provider/package-info.java` | Provider SPI 及各实现 |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/sse/package-info.java` | SSE 流式工具 |
| 新建 | `cartisan-ai/src/main/java/com/cartisan/ai/config/package-info.java` | Spring Boot 自动配置 |

## build.gradle.kts 内容规格

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

依赖选型说明：
- `spring-webflux` 用 `api` 而非 `implementation`：`ModelProvider.chatStream()` 返回 `Flux<ChatStreamEvent>`，该类型出现在公开 SPI 签名中，下游需在编译期可见
- `spring-webflux` 而非 `spring-boot-starter-webflux`：后者包含 Netty 嵌入式服务器，库模块不需要
- `tasks.withType<JavaCompile>` 加 `-parameters`：Spring 运行时通过参数名解析注入，与其他模块保持一致

## 验收方式

```bash
# 编译通过
./gradlew :cartisan-ai:compileJava

# 子模块注册成功
./gradlew projects | grep cartisan-ai
```
