# Feature: F04-01 模块骨架 + PageQuery

> **Epic**: Epic 4: Data-Query — jOOQ 读侧封装
>
> **复杂度**: S（Small，功能聚焦，无跨模块复杂集成）
>
> **预估代码量**: 50-80 行

---

## 背景

cartisan-data-query 是 cartisan-boot 框架的 jOOQ 读侧封装模块，为 CQRS 架构提供类型安全的数据库查询能力。

作为 Epic 4 的第一个 Feature，需要先建立模块骨架，并定义分页查询参数类 PageQuery。PageQuery 与 cartisan-web 中的 PageResponse 配对使用，为读写两侧提供一致的分页 API。

---

## 目标

- 建立 cartisan-data-query 模块的 Gradle 构建配置
- 定义 PageQuery Record，提供分页参数校验和 offset 计算
- 在 cartisan-dependencies BOM 中添加 jOOQ 版本管理，为后续 Feature 做好准备
- 模块可被 Gradle 构建识别，为后续 F04-02/F04-03 提供基础

---

## 范围

### 包含（In Scope）

| 项 | 说明 |
|---|------|
| 模块骨架 | `cartisan-data-query/build.gradle.kts`，配置模块依赖 |
| jOOQ BOM | 在 `cartisan-dependencies/build.gradle.kts` 中添加 jOOQ BOM 依赖 |
| PageQuery 类 | `com.cartisan.data.query.page.PageQuery`，Record 类型 |
| 参数校验 | page 最小值为 1，size 范围 1-100 |
| offset 计算 | 提供 offset() 方法计算数据库查询偏移量 |
| 工厂方法 | 提供 `of(int page, int size)` 静态方法 |
| 模块注册 | 在 `settings.gradle.kts` 中 include 新模块 |

### 不包含（Out of Scope）

| 项 | 原因 |
|---|------|
| jOOQ DSLContext 配置 | 由 F04-02 负责 |
| 多租户查询工具 | 由 F04-03 负责 |
| 代码生成配置指南 | 由 F04-04 负责 |
| 集成测试 | 由 F04-05 负责 |
| PageQuery.ofFirstPage() 等便捷方法 | 非必需，调用方可直接使用 `of(1, 20)` |

---

## 验收标准（Acceptance Criteria）

### AC1: 模块可被 Gradle 识别
```bash
./gradlew :cartisan-data-query:compileJava
```
执行成功，无编译错误。

### AC2: PageQuery 参数校验正确
- `page < 1` 时自动修正为 1
- `size < 1` 时自动修正为 20
- `size > 100` 时自动修正为 100
- 构造后 `page` 和 `size` 值在合法范围内

### AC3: offset() 计算正确
- `PageQuery.of(1, 20).offset()` 返回 `0`
- `PageQuery.of(2, 20).offset()` 返回 `20`
- `PageQuery.of(3, 10).offset()` 返回 `20`
- 计算公式：`(page - 1) * size`

### AC4: jOOQ 依赖版本已管理
- cartisan-dependencies 中添加了 jOOQ BOM
- cartisan-data-query 声明 jOOQ 依赖时不指定版本号

### AC5: 模块已注册到 settings.gradle.kts
- `include("cartisan-data-query")` 已添加

---

## 设计决策

### 决策 1：jOOQ 版本管理方式

**方案选择**: 在 cartisan-dependencies BOM 中添加 jOOQ BOM

**理由**:
- 与现有「cartisan-dependencies 做 BOM、继承 Spring Boot BOM」的架构一致
- 版本集中管理，业务项目通过 BOM 获取统一版本
- 避免不同模块声明不同版本的 jOOQ

**实现方式**:
```kotlin
// cartisan-dependencies/build.gradle.kts
dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    api(platform("org.jooq:jooq-bom:3.19.15"))  // 新增
    // ...
}
```

### 决策 2：PageQuery 包路径

**方案选择**: `com.cartisan.data.query.page.PageQuery`

**理由**:
- 符合设计文档中的包结构约定（`page/` 用于分页工具）
- 与未来可能添加的其他分页相关类保持一致

---

## 约束

### 技术约束
- Java 21 Record 语法
- 无外部框架依赖（纯 Java 类）
- 不依赖 cartisan-core（保持模块独立性）

### 依赖约束
- api 依赖 cartisan-web（复用 PageResponse<T>，虽然 PageQuery 本身不直接使用）
- implementation 依赖 jooq-core（为后续 Feature 准备）

### 包结构约束
```
com.cartisan.data.query/
└── page/
    └── PageQuery.java
```

---

## 参考文档

- [Epic 4 Backlog](../00_epic_backlog.md)
- [cartisan-boot 设计文档 - 4.6 cartisan-data-query](../../cartisan-boot-设计文档.md#46-cartisan-data-queryjooq-读侧封装)
- [cartisan-web PageResponse](../../cartisan-web/src/main/java/com/cartisan/web/response/PageResponse.java)
