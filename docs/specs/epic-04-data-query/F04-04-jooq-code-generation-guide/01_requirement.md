# Feature: jOOQ 代码生成配置指南

## 背景

业务项目使用 cartisan-data-query 时需要配置 jOOQ 代码生成。当前缺乏官方配置指南，开发者需要：
- 自己搜索 jOOQ + Gradle 配置方式
- 踩坑：Flyway 顺序、IDE 识别、版本管理
- 不了解 cartisan-boot 的约定（BOM、目录结构）

一份官方指南可以降低上手成本，同时记录框架设计决策，方便维护。

## 目标

- 提供复制即用的 Gradle 配置示例
- 说明各配置项的作用和设计考虑
- 记录常见问题和解决方案
- 支持 cartisan-boot 的 AI 协作开发流程

## 范围

### 包含（In Scope）

- **配置指南**：`build.gradle.kts` 完整示例
- **配置说明**：jOOQ 插件、数据库连接、生成策略、输出配置、任务依赖
- **版本管理**：BOM 管理说明、版本覆盖方式
- **IDE 集成**：IDEA 识别生成目录的配置
- **常见问题**：现象-原因-处理表格
- **AI 协作**：简短说明 + 链接到 SOP

### 不包含（Out of Scope）

- jOOQ 查询语法教程（官方文档已覆盖）
- jOOQ 代码生成器所有选项的完整说明（见官方文档）
- 非 PostgreSQL 数据库的配置（当前仅支持 PostgreSQL）
- 业务项目的具体 schema 设计

## 验收标准（Acceptance Criteria）

- **AC1**：读者复制配置片段后，`generateJooq` 任务可成功运行
- **AC2**：文档说明为何 `generateJooq` 依赖 `flywayMigrate`
- **AC3**：文档说明 jOOQ 版本由 cartisan-dependencies BOM 管理
- **AC4**：常见问题表格覆盖至少 5 个典型问题
- **AC5**：AI 协作部分链接到 SOP，不重复展开
- **AC6**：文档存放在 `docs/guides/jooq-code-generation.md`

## 约束

- **格式**：Markdown
- **目录**：首次创建 `docs/guides/` 目录
- **语言**：中文
- **维护**：框架变更时同步更新（如 jOOQ 版本升级、插件变更）

## 交付物

- `docs/guides/jooq-code-generation.md`（使用指南）
- F04-04 Spec 文档（01 本文件）
