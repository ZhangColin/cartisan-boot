# Feature: F05-01 cartisan-ai 模块骨架

## 背景

cartisan-ai 是 cartisan-boot 的大模型集成模块，提供统一的多 Provider 对话调用抽象（同步 + 流式 SSE）。本 Feature 建立模块骨架，为后续 F05-02 至 F05-09 提供编译单元与包结构基础。

## 目标

- 在 Gradle 多模块项目中注册 `cartisan-ai` 子模块
- 建立标准的构建配置（依赖声明、版本管理）
- 建立包结构，明确各子包职责边界

## 范围

### 包含（In Scope）

- `settings.gradle.kts` 追加 `include("cartisan-ai")`
- `cartisan-ai/build.gradle.kts` 依赖声明
- 5 个 `package-info.java`（根包 + `model`/`provider`/`sse`/`config` 四个子包）

### 不包含（Out of Scope）

- 任何业务类、接口、枚举（均属后续 Feature）
- 测试代码（本 Feature 无业务逻辑，无需单元测试）
- AutoConfiguration 注册文件（属 F05-09）

## 验收标准

- AC1: `./gradlew :cartisan-ai:compileJava` 编译通过，无报错
- AC2: `cartisan-ai` 出现在 `./gradlew projects` 输出中
- AC3: 根包及四个子包共 5 个 `package-info.java` 文件均存在

## 约束

- 无代码 Feature，无需单元测试
- 构建配置须与其他模块风格一致（参照 `cartisan-event/build.gradle.kts`）
