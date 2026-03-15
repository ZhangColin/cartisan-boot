# Feature: F04-01 模块骨架 + PageQuery — 实施计划

> **Epic**: Epic 4: Data-Query — jOOQ 读侧封装
>
> **复杂度**: S（Small，功能聚焦，无跨模块复杂集成）
>
> **预估代码量**: 50-80 行

---

## 目标复述

建立 cartisan-data-query 模块骨架，定义 PageQuery 分页查询参数类：
- 模块可被 Gradle 构建识别
- PageQuery 使用 Record 实现，参数自动校验（page ≥ 1，size ∈ [1, 100]）
- 提供 offset() 方法和 of() 静态工厂方法
- 在 cartisan-dependencies BOM 中添加 jOOQ 版本管理

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `settings.gradle.kts` | 添加 `include("cartisan-data-query")` |
| 修改 | `cartisan-dependencies/build.gradle.kts` | 添加 jOOQ BOM |
| 新建 | `cartisan-data-query/build.gradle.kts` | 模块构建配置 |
| 新建 | `cartisan-data-query/src/main/java/com/cartisan/data/query/package-info.java` | 根包说明 |
| 新建 | `cartisan-data-query/src/main/java/com/cartisan/data/query/page/package-info.java` | 分页包说明 |
| 新建 | `cartisan-data-query/src/main/java/com/cartisan/data/query/page/PageQuery.java` | 分页查询参数类 |
| 新建 | `cartisan-data-query/src/test/java/com/cartisan/data/query/page/PageQueryTest.java` | PageQuery 单元测试 |

---

## 核心流程（伪代码）

```
1. 修改 settings.gradle.kts，注册新模块
2. 修改 cartisan-dependencies/build.gradle.kts，添加 jOOQ BOM
3. 创建 cartisan-data-query/build.gradle.kts，配置依赖
4. 创建 package-info.java 文档
5. 创建 PageQueryTest.java（先写测试，红灯）
6. 创建 PageQuery.java（实现，绿灯）
7. 运行测试验证
```

---

## 原子任务清单

### Step 1: 修改 settings.gradle.kts

**文件**: `settings.gradle.kts`

**操作**: 在现有 include 列表末尾添加 cartisan-data-query

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

**验证**:
```bash
./gradlew projects
# 预期输出中包含 Root project 'cartisan-boot' + 子项目列表，包括 cartisan-data-query
```

---

### Step 2: 修改 cartisan-dependencies/build.gradle.kts

**文件**: `cartisan-dependencies/build.gradle.kts`

**操作**: 在 dependencies 块中添加 jOOQ BOM

```kotlin
dependencies {
    // Spring Boot BOM - manages all Spring Boot starter versions
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))

    // jOOQ BOM（F04-01）
    api(platform("org.jooq:jooq-bom:3.19.15"))

    // Sa-Token（F03-01）- 直接用 api() 声明带版本约束
    api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")
}
```

**验证**:
```bash
./gradlew :cartisan-dependencies:dependencies
# 预期输出中包含 jOOQ BOM 依赖
```

---

### Step 3: 创建 cartisan-data-query/build.gradle.kts

**文件**: `cartisan-data-query/build.gradle.kts`

**操作**: 创建模块构建配置文件

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

**验证**:
```bash
./gradlew :cartisan-data-query:compileJava
# 预期：BUILD SUCCESSFUL（当前可能编译失败因为还没有源代码，但构建配置正确）
```

---

### Step 4: 创建 package-info.java 文件

**文件**: `cartisan-data-query/src/main/java/com/cartisan/data/query/package-info.java`

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

**文件**: `cartisan-data-query/src/main/java/com/cartisan/data/query/page/package-info.java`

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

### Step 5: 编写 PageQueryTest.java（红灯）

**文件**: `cartisan-data-query/src/test/java/com/cartisan/data/query/page/PageQueryTest.java`

**操作**: 先写测试，此时 PageQuery 不存在，测试应编译通过但全部失败

```java
package com.cartisan.data.query.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

@DisplayName("PageQuery 分页查询参数测试")
class PageQueryTest {

    @DisplayName("给定 page < 1，构造时应修正为 1")
    @Test
    void given_pageLessThanOne_when_construct_then_pageNormalizedToOne() {
        PageQuery query = new PageQuery(0, 20);
        assertThat(query.page()).isEqualTo(1);
    }

    @DisplayName("给定 page 为负数，构造时应修正为 1")
    @Test
    void given_negativePage_when_construct_then_pageNormalizedToOne() {
        PageQuery query = new PageQuery(-5, 20);
        assertThat(query.page()).isEqualTo(1);
    }

    @DisplayName("给定 size < 1，构造时应修正为默认值 20")
    @Test
    void given_sizeLessThanOne_when_construct_then_sizeNormalizedToTwenty() {
        PageQuery query = new PageQuery(1, 0);
        assertThat(query.size()).isEqualTo(20);
    }

    @DisplayName("给定 size > 100，构造时应修正为最大值 100")
    @Test
    void given_sizeGreaterThanMax_when_construct_then_sizeNormalizedToHundred() {
        PageQuery query = new PageQuery(1, 150);
        assertThat(query.size()).isEqualTo(100);
    }

    @DisplayName("给定有效 page 和 size，构造时应保持原值")
    @Test
    void given_validPageAndSize_when_construct_then_valuesUnchanged() {
        PageQuery query = new PageQuery(2, 50);
        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(50);
    }

    @DisplayName("给定边界值 page=1 size=1，offset 应返回 0")
    @Test
    void given_boundaryValues_when_offset_then_returnZero() {
        PageQuery query = new PageQuery(1, 1);
        assertThat(query.offset()).isEqualTo(0);
    }

    @DisplayName("给定 page=2 size=20，offset 应返回 20")
    @Test
    void given_pageTwoSizeTwenty_when_offset_then_returnCorrectValue() {
        PageQuery query = new PageQuery(2, 20);
        assertThat(query.offset()).isEqualTo(20);
    }

    @DisplayName("给定 page=3 size=10，offset 应返回 20")
    @Test
    void given_pageThreeSizeTen_when_offset_then_returnCorrectValue() {
        PageQuery query = new PageQuery(3, 10);
        assertThat(query.offset()).isEqualTo(20);
    }

    @DisplayName("给定 page 和 size，of() 静态方法应创建正确实例")
    @Test
    void given_pageAndSize_when_of_then_returnPageQuery() {
        PageQuery query = PageQuery.of(5, 30);
        assertThat(query.page()).isEqualTo(5);
        assertThat(query.size()).isEqualTo(30);
    }

    @DisplayName("给定非法参数，of() 静态方法也应触发校验修正")
    @Test
    void given_invalidParams_when_of_then_applyNormalization() {
        PageQuery query = PageQuery.of(-1, 200);
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(100);
    }
}
```

**验证**:
```bash
./gradlew :cartisan-data-query:compileTestJava
# 预期：BUILD SUCCESSFUL

./gradlew :cartisan-data-query:test
# 预期：BUILD FAILED，测试失败因为 PageQuery 类不存在
```

---

### Step 6: 编写 PageQuery.java（绿灯）

**文件**: `cartisan-data-query/src/main/java/com/cartisan/data/query/page/PageQuery.java`

**操作**: 实现 PageQuery 使所有测试通过

```java
package com.cartisan.data.query.page;

/**
 * 分页查询参数。
 *
 * <p>使用 Record 实现，不可变对象。参数自动校验和修正：
 * <ul>
 *   <li>page < 1 时修正为 1</li>
 *   <li>size < 1 时修正为 20</li>
 *   <li>size > 100 时修正为 100</li>
 * </ul>
 *
 * @param page 当前页码（从 1 开始，构造时自动校验）
 * @param size 每页大小（1-100，构造时自动校验）
 */
public record PageQuery(int page, int size) {

    /**
     * Compact Constructor - 参数校验和修正
     */
    public PageQuery {
        // page 校验：小于 1 时修正为 1
        if (page < 1) {
            page = 1;
        }

        // size 校验：小于 1 时修正为 20，大于 100 时修正为 100
        if (size < 1) {
            size = 20;
        } else if (size > 100) {
            size = 100;
        }
    }

    /**
     * 计算数据库查询的 OFFSET 值。
     *
     * @return OFFSET 值，公式：(page - 1) * size
     */
    public long offset() {
        return (long) (page - 1) * size;
    }

    /**
     * 创建 PageQuery 实例的静态工厂方法。
     *
     * <p>等价于 {@code new PageQuery(page, size)}，提供更好的可读性。
     *
     * @param page 页码
     * @param size 每页大小
     * @return PageQuery 实例
     */
    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size);
    }
}
```

**验证**:
```bash
./gradlew :cartisan-data-query:compileJava
# 预期：BUILD SUCCESSFUL

./gradlew :cartisan-data-query:test
# 预期：BUILD SUCCESSFUL，所有测试通过

./gradlew :cartisan-data-query:build
# 预期：BUILD SUCCESSFUL
```

---

## 验收检查清单

| # | 检查项 | 验证方式 | 预期结果 |
|---|--------|---------|---------|
| 1 | 模块已注册 | `./gradlew projects` | 包含 cartisan-data-query |
| 2 | jOOQ BOM 已添加 | 查看 cartisan-dependencies/build.gradle.kts | 包含 jOOQ BOM |
| 3 | 模块可编译 | `./gradlew :cartisan-data-query:compileJava` | BUILD SUCCESSFUL |
| 4 | 测试通过 | `./gradlew :cartisan-data-query:test` | 全部 PASS |
| 5 | AC1：模块可被识别 | `./gradlew :cartisan-data-query:build` | BUILD SUCCESSFUL |
| 6 | AC2：参数校验正确 | 查看测试报告 | PageQueryTest 中 4 个校验测试通过 |
| 7 | AC3：offset() 计算正确 | 查看测试报告 | PageQueryTest 中 3 个 offset 测试通过 |
| 8 | package-info.java 存在 | 文件检查 | 2 个文件存在且内容正确 |

---

## 参考文档

- [01_requirement.md](./01_requirement.md) — 需求规格
- [02_interface.md](./02_interface.md) — 接口契约
- [Epic 4 Backlog](../00_epic_backlog.md) — Epic 总览
- [cartisan-web PageResponse](../../cartisan-web/src/main/java/com/cartisan/web/response/PageResponse.java) — 配对的响应类
