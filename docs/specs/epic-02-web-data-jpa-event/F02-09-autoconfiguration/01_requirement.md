# Feature: F02-09 AutoConfiguration

> **Epic**: Epic 2: Web + Data-JPA + Event
> **复杂度**: M
> **依赖**: F02-01 ~ F02-08

---

## 背景

cartisan-boot 的 cartisan-web、cartisan-data-jpa、cartisan-event 三个模块已完成核心功能开发（F02-01 ~ F02-08），但尚未实现 Spring Boot AutoConfiguration 机制。

当前问题：
- 框架类位于 `com.cartisan.*` 包，不会被 `@SpringBootApplication` 的默认组件扫描覆盖
- 用户引入依赖后，需要手动添加 `@ComponentScan("com.cartisan.*")` 才能生效，违反"引入即用"原则

## 目标

为 cartisan-web、cartisan-data-jpa、cartisan-event 三个模块创建完整的 Spring Boot AutoConfiguration，实现：
1. **引入即用**：添加依赖后，框架组件自动注册
2. **条件装配**：根据环境或用户配置决定是否启用某些功能
3. **可扩展性**：核心扩展点（如 `DomainEventPublisher`）允许用户替换

## 范围

### 包含（In Scope）

#### cartisan-web
- 创建 `CartisanWebAutoConfiguration`
- 显式注册 `RequestContextFilter`、`GlobalExceptionHandler` Bean
- `RequestContextFilter` 实现 `Ordered` 接口确保最高优先级
- 创建 AutoConfiguration imports 文件

#### cartisan-data-jpa
- 增强 `CartisanDataJpaAutoConfiguration`，用 `@Import` 导入 `JpaAuditingConfiguration`
- 确保 JPA Auditing 在存在 `AuditorAware` Bean 时自动启用

#### cartisan-event
- `CartisanEventAutoConfiguration` 改用 `@AutoConfiguration` 注解
- 保持 `@ConditionalOnMissingBean` 允许用户替换 `DomainEventPublisher`

### 不包含（Out of Scope）

- 配置属性类（`@ConfigurationProperties`）—— 延后到有实际需求时实现
- 新增功能 —— 仅做配置整合，不新增业务功能

## 验收标准（Acceptance Criteria）

### AC1: cartisan-web 模块"引入即用"
- 用户添加 `implementation("com.cartisan:cartisan-web")` 依赖后
- 无需 `@ComponentScan`，`RequestContextFilter` 和 `GlobalExceptionHandler` 自动注册
- `RequestContextFilter` 在过滤器链中最早执行

### AC2: cartisan-data-jpa 模块 JPA Auditing 自动启用
- 当容器中存在 `AuditorAware` Bean 时，`@CreatedDate`/`@LastModifiedDate` 自动填充
- 当不存在 `AuditorAware` Bean 时，不启用 Auditing，不报错

### AC3: cartisan-event 模块可扩展
- 默认注册 `SpringDomainEventPublisher` Bean
- 用户可定义自己的 `DomainEventPublisher` Bean 覆盖默认实现

### AC4: 集成测试验证
- 创建测试应用：仅引入 cartisan 依赖，无 `@ComponentScan`
- 验证 `RequestContextFilter` 生效（requestId 有值）
- 验证 `GlobalExceptionHandler` 处理异常
- 验证 JPA Auditing 在有/无 `AuditorAware` 时的行为

### AC5: 非侵入性
- 组件通过 AutoConfiguration 显式注册，不依赖对 `com.cartisan.*` 的组件扫描
- `RequestContextFilter`：移除 `@Component` 与 `@Order`，实现 `Ordered` 接口，由配置类 `@Bean` 注册
- `GlobalExceptionHandler`：保留 `@ControllerAdvice`（行为所需），仅通过配置类 `@Bean` 注册，不依赖包扫描发现

## 约束

- 必须使用 Spring Boot 3.4+ 的 `@AutoConfiguration` 注解
- imports 文件路径：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 不引入配置属性，所有行为使用代码定义的默认值

## 变更范围

| 模块 | 新增文件 | 修改文件 | 删除文件 |
|-----|---------|---------|---------|
| cartisan-web | 2 (AutoConfiguration 类 + imports 资源) | 1 (RequestContextFilter) | 0 |
| cartisan-data-jpa | 0 | 1 (CartisanDataJpaAutoConfiguration) | 0 |
| cartisan-event | 0 | 1 (CartisanEventAutoConfiguration) | 0 |
| 合计 | 2 | 3 | 0 |
