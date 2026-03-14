# Feature: F03-01 cartisan-security 模块骨架

> **Epic**: Epic 03 - Security
> **复杂度**: S（Small）
> **依赖**: 无
> **预估工时**: 0.5d

---

## 背景

cartisan-security 是 cartisan-boot 框架的安全模块，提供认证授权薄抽象层和多租户上下文基础设施。底层使用 Sa-Token 实现，但通过抽象层实现可替换性。

作为 Epic 03 的第一个 Feature，需要先建立模块骨架，配置依赖，建立包结构，为后续 Feature（F03-02 ~ F03-08）奠定基础。

---

## 目标

1. 建立 cartisan-security 模块，可正常构建和测试
2. 配置依赖：cartisan-core、cartisan-web、Sa-Token 1.45.0
3. 建立四个核心子包的目录结构
4. 为后续 Feature 预留测试基础设施

---

## 范围

### 包含（In Scope）

| 项 | 说明 |
|----|------|
| 模块创建 | cartisan-security 子模块，加入 settings.gradle.kts |
| BOM 配置 | 在 cartisan-dependencies 中声明 Sa-Token 1.45.0 |
| 依赖配置 | cartisan-core、cartisan-web、sa-token-spring-boot3-starter |
| 包结构 | annotation、context、authentication、config 四个子包 |
| 测试基础设施 | 测试目录 + 占位测试类 |
| 构建验证 | 模块可 build、可 test |

### 不包含（Out of Scope）

| 项 | 原因 |
|----|------|
| 注解实现 | F03-02 负责 |
| SecurityContext 实现 | F03-03 负责 |
| TenantContext 实现 | F03-04 负责 |
| Filter 实现 | F03-05 负责 |
| AuthenticationService 实现 | F03-06 负责 |
| 自动配置 | F03-07 负责 |
| 集成测试 | F03-08 负责 |

---

## 验收标准（Acceptance Criteria）

| # | 验收项 | 验证方式 |
|---|--------|---------|
| AC1 | settings.gradle.kts 包含 `include("cartisan-security")` | 检查文件内容 |
| AC2 | cartisan-dependencies BOM 声明 `api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")` | 检查文件内容 |
| AC3 | cartisan-security/build.gradle.kts 配置正确（依赖、Java 21 toolchain、测试配置） | 检查文件内容 |
| AC4 | 四个子包 + 根包有 package-info.java（annotation、context、authentication、config + 根包） | 检查目录结构 |
| AC5 | src/test/java 目录 + CartisanSecurityModuleTest 占位测试类存在 | 检查目录结构 |
| AC6 | `./gradlew :cartisan-security:build` 成功 | 执行命令 |
| AC7 | `./gradlew :cartisan-security:test` 成功 | 执行命令 |

---

## 约束

### 架构约束

| 约束项 | 说明 |
|--------|------|
| Java 版本 | Java 21（通过 toolchain 配置） |
| 依赖版本 | Sa-Token 1.45.0，由 BOM 统一管理 |
| 包命名 | com.cartisan.security.{子包} |
| 测试框架 | JUnit 5 + AssertJ |

### 技术约定

| 约束项 | 说明 |
|--------|------|
| BOM 使用 | cartisan-dependencies 使用 `java-platform` 插件，用 `api()` 声明约束 |
| 依赖传递 | cartisan-core、cartisan-web 使用 `api(project(...))` 暴露给使用者 |
| Sa-Token | 使用 `implementation()`，版本由 BOM 管理 |
| 编译参数 | 添加 `-parameters` 保留参数名（Spring 需要） |

---

## 交付物

| 文件 | 路径 | 说明 |
|------|------|------|
| build.gradle.kts | cartisan-security/build.gradle.kts | 模块构建配置 |
| package-info.java (x5) | cartisan-security/src/main/java/com/cartisan/security/{子包}/ | 包职责说明 |
| CartisanSecurityModuleTest.java | cartisan-security/src/test/java/com/cartisan/security/ | 占位测试类 |
| build.gradle.kts (修改) | cartisan-dependencies/build.gradle.kts | 添加 Sa-Token 约束 |
| settings.gradle.kts (修改) | settings.gradle.kts | 添加 include("cartisan-security") |

---

## 后续依赖

本 Feature 完成后，以下 Feature 可以开始：
- F03-02: 权限注解 + MVC 拦截器
- F03-03: SecurityContext
- F03-04: TenantContext
- F03-06: AuthenticationService

（F03-05 依赖 F03-04，F03-07 依赖全部前置，F03-08 最后）
