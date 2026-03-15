# Feature: F03-07 自动配置 — 需求规格

> **Phase**: Research → Plan → Execute → Review → **Done (2026-03-15)**
> **依赖**: F03-01 至 F03-06（所有前置 Feature）
> **复杂度**: M

---

## 状态摘要

| 阶段 | 状态 | 完成日期 |
|------|------|----------|
| Phase 0: Epic 分解 | ✅ | - |
| Phase 1: 需求澄清 | ✅ | - |
| Phase 2: 接口设计 | ✅ | - |
| Phase 3: 实施方案 | ✅ | - |
| Phase 4: Execute (TDD) | ✅ | 2026-03-15 |
| Phase 5: Review & Archive | ✅ | 2026-03-15 |

---

## 背景

cartisan-security 模块已完成核心组件实现（权限注解、拦截器、Filter、认证服务），但这些组件目前需要在业务项目中手动配置才能生效。

为实现「零配置引入」的目标，需要提供 Spring Boot AutoConfiguration 支持，使得业务项目只需添加 `cartisan-security` 依赖，所有组件自动生效。

---

## 目标

1. 创建 `CartisanSecurityAutoConfiguration` 自动配置类
2. 提供 `CartisanSecurityProperties` 配置属性类，支持拦截器路径配置
3. 注册 `SecurityInterceptor` 到 Spring MVC 拦截器链
4. 确保条件装配：仅在 Web 应用 + Sa-Token 存在时生效

---

## 范围

### 包含（In Scope）

- `CartisanSecurityAutoConfiguration` 主配置类
- `CartisanSecurityProperties` 配置属性类
- `SecurityInterceptorConfig` 内部配置类（WebMvcConfigurer）
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 单元测试 + 集成测试

### 不包含（Out of Scope）

- **Filter 自动注册**：`TenantContextFilter` 已有 `@Component`，会被自动扫描
- **AuthenticationService 注册**：`SaTokenAuthenticationService` 已有 `@Component`
- **SecurityExceptionHandler 注册**：已有 `@ControllerAdvice`
- **CORS 配置**：不属于本次 Feature
- **多认证实现切换**：当前仅支持 Sa-Token

---

## 功能需求

### FR1: 自动配置主类

提供 `CartisanSecurityAutoConfiguration` 类：

- 使用 `@AutoConfiguration` 注解
- 条件：`@ConditionalOnWebApplication` + `@ConditionalOnClass(StpUtil.class)`
- 通过 `@Import` 导入 `SecurityInterceptorConfig`
- 启用配置属性：`@EnableConfigurationProperties(CartisanSecurityProperties.class)`

### FR2: 配置属性类

提供 `CartisanSecurityProperties` 类：

- 前缀：`cartisan.security.interceptor`
- 属性：
  - `path-patterns`：拦截器生效的路径模式（Ant 风格）
  - `exclude-path-patterns`：排除的路径模式
- 默认值：
  - `path-patterns`: `["/**"]`
  - `exclude-path-patterns`: `["/error", "/actuator/**"]`

### FR3: 拦截器注册配置

提供 `SecurityInterceptorConfig` 类：

- 实现 `WebMvcConfigurer`
- **注入**已有的 `SecurityInterceptor` Bean（不声明新的 @Bean）
- 从 `CartisanSecurityProperties` 读取路径配置
- 在 `addInterceptors` 中注册拦截器

### FR4: AutoConfiguration.imports

在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中声明：

```
com.cartisan.security.config.CartisanSecurityAutoConfiguration
```

---

## 验收标准（Acceptance Criteria）

### AC1: 引入依赖后自动生效
- **Given** 一个 Spring Boot Web 应用，引入 `cartisan-security` 和 `sa-token-spring-boot3-starter` 依赖
- **When** 应用启动
- **Then** `CartisanSecurityAutoConfiguration` 被加载
- **And** `SecurityInterceptorConfig` 被加载
- **And** `SecurityInterceptor` 被注册到 MVC 拦截器链

### AC2: 条件装配 - 非 Web 应用
- **Given** 一个非 Web 应用（如批处理应用）
- **When** 引入 `cartisan-security` 依赖
- **Then** `CartisanSecurityAutoConfiguration` **不**被加载

### AC3: 条件装配 - 无 Sa-Token
- **Given** 一个 Web 应用，未引入 Sa-Token 依赖
- **When** 引入 `cartisan-security` 依赖
- **Then** `CartisanSecurityAutoConfiguration` **不**被加载

### AC4: 默认路径配置
- **Given** 应用未配置 `cartisan.security.interceptor` 属性
- **When** 应用启动
- **Then** 拦截器应用于 `/**` 路径
- **And** `/error` 和 `/actuator/**` 被排除

### AC5: 自定义路径配置
- **Given** 应用配置：
  ```yaml
  cartisan:
    security:
      interceptor:
        path-patterns: ["/api/**", "/admin/**"]
        exclude-path-patterns: ["/api/public/**"]
  ```
- **When** 应用启动
- **Then** 拦截器仅应用于 `/api/**` 和 `/admin/**`
- **And** `/api/public/**` 被排除

### AC6: 拦截器行为验证
- **Given** 应用已启动，自动配置生效
- **When** 请求带有 `@RequireRole("admin")` 的 Controller 方法
- **Then** `SecurityInterceptor.preHandle()` 被调用
- **And** 鉴权逻辑正常执行

### AC7: 单元测试覆盖率
- 所有配置类均有单元测试覆盖
- 覆盖率 ≥ 80%

---

## 约束

### 技术约束

| 约束项 | 要求 |
|--------|------|
| Java 版本 | Java 21+ |
| Spring Boot | 3.4.x |
| 测试框架 | JUnit 5 + AssertJ |
| 配置绑定 | `@ConfigurationProperties` + 可变 List |

### 架构约束

- `SecurityInterceptorConfig` **只注入**已有的 `SecurityInterceptor` Bean，不声明新 Bean
- 配置属性使用可变 List（`ArrayList`）确保与 Spring Boot 配置绑定兼容
- 主配置类作为唯一入口，AutoConfiguration.imports 只声明一个类

---

## 配置示例

### 默认行为（无需配置）

```yaml
# 无需配置，自动生效
# 默认：拦截 /**，排除 /error、/actuator/**
```

### 自定义路径

```yaml
cartisan:
  security:
    interceptor:
      path-patterns:
        - "/api/**"
        - "/admin/**"
      exclude-path-patterns:
        - "/api/public/**"
        - "/error"
```

### 仅保护 API 路径

```yaml
cartisan:
  security:
    interceptor:
      path-patterns:
        - "/api/**"
```

---

## 参考文档

- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- AI 协作 SOP: [AI协作开发SOP.md](../../../sop/AI协作开发SOP.md)
- Spring Boot AutoConfiguration: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration
