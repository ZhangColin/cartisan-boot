# Feature: F04-01 模块骨架 + PageQuery — 接口契约

> **注意**：本 Feature 主要是模块骨架和纯数据类（Record），不涉及 HTTP 端点或服务接口。

---

## 公共 API 定义

### PageQuery 类

**完整类名**：`com.cartisan.data.query.page.PageQuery`

**类型**：Java Record（不可变对象）

**职责**：封装分页查询参数（页码、每页大小），提供参数校验和 offset 计算

---

## 公共方法契约

### 构造函数

```
PageQuery(int page, int size)
```

| 参数 | 类型 | 输入范围 | 校验行为 |
|------|------|---------|---------|
| page | int | 任意整数 | < 1 时自动修正为 1 |
| size | int | 任意整数 | < 1 时修正为 20，> 100 时修正为 100 |

**前置条件**：无
**后置条件**：page ≥ 1，size ∈ [1, 100]

### offset() 方法

```
long offset()
```

**描述**：计算数据库查询的 OFFSET 值

**返回值**：`(page - 1) * size`，表示从第几条记录开始查询

**示例**：
| 输入 (page, size) | 输出 offset |
|-------------------|-------------|
| (1, 20) | 0 |
| (2, 20) | 20 |
| (3, 10) | 20 |

### of() 静态工厂方法

```
static PageQuery of(int page, int size)
```

**描述**：创建 PageQuery 实例的便捷方法

**等价于**：`new PageQuery(page, size)`

**用途**：代码可读性更高，`PageQuery.of(1, 20)` vs `new PageQuery(1, 20)`

---

## 数据结构描述

### PageQuery Record 字段

| 字段 | 类型 | 可变性 | 说明 |
|------|------|--------|------|
| page | int | 不可变 | 当前页码（从 1 开始） |
| size | int | 不可变 | 每页记录数 |

---

## 变更范围

### 修改的文件

| 文件 | 变更内容 |
|------|---------|
| `settings.gradle.kts` | 添加 `include("cartisan-data-query")` |
| `cartisan-dependencies/build.gradle.kts` | 添加 jOOQ BOM 依赖 |

### 新建的文件

| 文件 | 说明 |
|------|------|
| `cartisan-data-query/build.gradle.kts` | 模块构建配置 |
| `cartisan-data-query/src/main/java/com/cartisan/data/query/package-info.java` | 根包说明 |
| `cartisan-data-query/src/main/java/com/cartisan/data/query/page/package-info.java` | 分页包说明 |
| `cartisan-data-query/src/main/java/com/cartisan/data/query/page/PageQuery.java` | 分页查询参数类 |
| `cartisan-data-query/src/test/java/com/cartisan/data/query/page/PageQueryTest.java` | PageQuery 单元测试 |

---

## 目录结构

```
cartisan-data-query/
├── build.gradle.kts
└── src/
    ├── main/java/com/cartisan/data/query/
    │   ├── package-info.java
    │   └── page/
    │       ├── package-info.java
    │       └── PageQuery.java
    └── test/java/com/cartisan/data/query/
        └── page/
            └── PageQueryTest.java
```

---

## 配置文件内容（伪代码）

### cartisan-dependencies/build.gradle.kts（修改）

```kotlin
dependencies {
    // Spring Boot BOM
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))

    // jOOQ BOM（F04-01）
    api(platform("org.jooq:jooq-bom:3.19.15"))

    // Sa-Token
    api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")
}
```

**注意**：jOOQ 版本选用 3.19.15，与 Spring Boot 3.4.0 兼容。

### cartisan-data-query/build.gradle.kts（新建）

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

    // cartisan-web - 复用 PageResponse<T>（api 声明，传递给使用者）
    api(project(":cartisan-web"))

    // jOOQ Core
    implementation("org.jooq:jooq")

    // Testing
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

### settings.gradle.kts（修改）

```kotlin
include("cartisan-dependencies")
include("cartisan-core")
include("cartisan-test")
include("cartisan-web")
include("cartisan-data-jpa")
include("cartisan-event")
include("cartisan-security")
include("cartisan-data-query")  // 新增
```

---

## package-info.java 内容

### 根包

```java
/**
 * cartisan-data-query 模块根包。
 *
 * <p>提供 jOOQ 读侧封装基础设施，包括分页工具、多租户查询支持、自动配置等。</p>
 *
 * <p>与 cartisan-data-jpa（写侧）配合使用，实现 CQRS 架构。</p>
 */
package com.cartisan.data.query;
```

### page 包

```java
/**
 * 分页查询工具。
 *
 * <p>包含 {@code PageQuery} 分页参数类，与 cartisan-web 的 {@code PageResponse}
 * 配对使用，为读写两侧提供一致的分页 API。</p>
 */
package com.cartisan.data.query.page;
```

---

## PageQuery 类契约（伪代码）

```
public record PageQuery(int page, int size) {

    // Compact Constructor - 参数校验
    public PageQuery {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        else if (size > 100) size = 100;
    }

    // 计算 OFFSET 值
    public long offset() {
        return (long) (page - 1) * size;
    }

    // 静态工厂方法
    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size);
    }
}
```

---

## 测试策略

### 测试命名规范

遵循 `given_{条件}_when_{操作}_then_{预期结果}` 格式：

| 测试方法 | 验证场景 |
|---------|---------|
| `given_pageLessThanOne_when_construct_then_pageNormalizedToOne` | page < 1 时修正为 1 |
| `given_sizeLessThanOne_when_construct_then_sizeNormalizedToTwenty` | size < 1 时修正为 20 |
| `given_sizeGreaterThanMax_when_construct_then_sizeNormalizedToHundred` | size > 100 时修正为 100 |
| `given_validPageAndSize_when_offset_then_returnCorrectValue` | offset() 计算正确 |
| `given_edgeCases_when_offset_then_returnCorrectValue` | 边界值计算正确 |

---

## 技术决策

### 决策 1：PageQuery 使用 Record 而非 Class

**选择**：Java Record

**理由**：
- PageQuery 是纯数据类，无需可变性
- Record 自动生成 `equals()`、`hashCode()`、`toString()`
- 与 cartisan-web 的 PageResponse 保持一致

### 决策 2：参数校验放在 Compact Constructor

**选择**：使用 `public PageQuery { ... }` compact constructor

**理由**：
- Record 规范的校验方式
- 构造时立即修正，避免后续代码处理边界情况
- 调用方无需手动校验，使用更简洁

### 决策 3：jOOQ 版本选择 3.19.15

**选择**：3.19.15

**理由**：
- 与 Spring Boot 3.4.0 兼容
- jOOQ 3.19.x 是当前稳定系列
- 可通过 BOM 统一升级，无需修改各模块

---

## 验收方式

### 构建验证命令

| 命令 | 预期结果 |
|------|---------|
| `./gradlew :cartisan-data-query:compileJava` | BUILD SUCCESSFUL |
| `./gradlew :cartisan-data-query:test` | BUILD SUCCESSFUL，测试通过 |
| `./gradlew :cartisan-data-query:build` | BUILD SUCCESSFUL |

### 手动检查清单

| # | 检查项 | 预期结果 |
|---|--------|---------|
| 1 | settings.gradle.kts | 包含 `include("cartisan-data-query")` |
| 2 | cartisan-dependencies/build.gradle.kts | 包含 jOOQ BOM |
| 3 | cartisan-data-query/build.gradle.kts | 依赖配置正确 |
| 4 | package-info.java（2个） | 全部存在，内容正确 |
| 5 | PageQuery.java | 存在，Record 类型，包含 compact constructor |
| 6 | PageQueryTest.java | 存在，测试覆盖所有 AC |
