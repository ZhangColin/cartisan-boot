# Feature: F02-09 AutoConfiguration — 接口契约

> **Phase**: 2 — Design
> **定位**: 技术方案与接口设计（伪代码 + 表格），不包含 Java 源代码

---

## 一、架构设计

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot 应用                          │
│                                                              │
│  @SpringBootApplication                                     │
│  class MyApp { }                                            │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  META-INF/spring/...AutoConfiguration.imports        │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │ com.cartisan.web.config.                      │  │  │
│  │  │   CartisanWebAutoConfiguration                │  │  │
│  │  │ com.cartisan.data.jpa.config.                 │  │  │
│  │  │   CartisanDataJpaAutoConfiguration            │  │  │
│  │  │ com.cartisan.event.config.                    │  │  │
│  │  │   CartisanEventAutoConfiguration              │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  自动注册 Bean                                        │  │
│  │  • RequestContextFilter                             │  │
│  │  • GlobalExceptionHandler                           │  │
│  │  • Runnable (configureDomainEventPublisherHolder)   │  │
│  │  • DomainEventPublisher                             │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**关键设计决策**：
- 使用 Spring Boot 3.4+ 的 `@AutoConfiguration` 注解（替代 `@Configuration` + `@AutoConfigureBefore/After`）
- 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 声明自动配置
- Bean 注册完全通过 `@Bean` 方法，不依赖 `@Component` 扫描

---

## 二、模块设计

### 2.1 cartisan-web 模块

#### 2.1.1 新增：CartisanWebAutoConfiguration

**职责**：注册 Web 层核心组件（Filter、ExceptionHandler）

**伪代码**：

```java
// 包：com.cartisan.web.config
@AutoConfiguration
@ConditionalOnWebApplication  // 仅在 Web 应用中生效
public class CartisanWebAutoConfiguration {

    /**
     * 注册请求上下文 Filter。
     *
     * 前置条件：Web 应用环境
     * 后置条件：Filter 在过滤器链中最早执行
     * 异常：无
     */
    @Bean("cartisanRequestContextFilter")
    public RequestContextFilter requestContextFilter() {
        return new RequestContextFilter();
    }

    /**
     * 注册全局异常处理器。
     *
     * 前置条件：Web 应用环境
     * 后置条件：@ControllerAdvice 生效，异常被统一处理
     * 异常：无
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
```

**条件装配**：
| 注解 | 作用 | 理由 |
|-----|------|------|
| `@ConditionalOnWebApplication` | 仅在 Web 应用中生效 | 非Web应用（如批处理）不需要这些组件 |

**不使用 `@ConditionalOnMissingBean`**：核心组件强制注册，用户需通过排除 AutoConfiguration 或自定义 Bean 覆盖

---

#### 2.1.2 修改：RequestContextFilter

**职责**：请求上下文初始化，提取 requestId 和 clientIp

**变更说明**：

| 变更项 | 原状态 | 新状态 | 理由 |
|-------|--------|--------|------|
| 类注解 | `@Component("cartisanRequestContextFilter")` | 无 | 不再通过扫描注册 |
| Order 方式 | `@Order(Ordered.HIGHEST_PRECEDENCE)` | 实现 `Ordered` 接口 | `@Order` 在 `@Bean` 上对 Filter 顺序无效 |

**伪代码**：

```java
// 变更前
@Component("cartisanRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter { ... }

// 变更后
public class RequestContextFilter extends OncePerRequestFilter implements Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // doFilterInternal() 等方法保持不变
}
```

---

#### 2.1.3 GlobalExceptionHandler

**职责**：全局异常处理，统一响应格式

**变更说明**：

| 变更项 | 原状态 | 新状态 | 理由 |
|-------|--------|--------|------|
| 类注解 | `@ControllerAdvice` | **保留** `@ControllerAdvice` | 行为注解，必须保留 |
| 扫描注册 | 可能被扫描发现 | 通过 `@Bean` 显式注册 | 不依赖包扫描 |

**无需修改代码**，保持 `@ControllerAdvice` 注解即可

---

#### 2.1.4 新增：AutoConfiguration.imports

**路径**：`cartisan-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**内容**：
```
com.cartisan.web.config.CartisanWebAutoConfiguration
```

---

### 2.2 cartisan-data-jpa 模块

#### 2.2.1 修改：CartisanDataJpaAutoConfiguration

**职责**：配置 JPA 相关基础设施

**变更说明**：

| 变更项 | 原状态 | 新状态 |
|-------|--------|--------|
| 导入 Auditing 配置 | 无 | `@Import(JpaAuditingConfiguration.class)` |

**伪代码**：

```java
// 变更前
@AutoConfiguration
public class CartisanDataJpaAutoConfiguration {
    @Bean
    public Runnable configureDomainEventPublisherHolder(DomainEventPublisher publisher) {
        return () -> DomainEventPublisherHolder.setPublisher(publisher);
    }
}

// 变更后
@AutoConfiguration
@Import(JpaAuditingConfiguration.class)  // 新增
public class CartisanDataJpaAutoConfiguration {

    // 现有：配置领域事件发布器持有者
    @Bean
    public Runnable configureDomainEventPublisherHolder(DomainEventPublisher publisher) {
        return () -> DomainEventPublisherHolder.setPublisher(publisher);
    }
}
```

**设计说明**：
- `JpaAuditingConfiguration` 保持独立，其 `@ConditionalOnBean(AuditorAware.class)` 继续生效
- 只有用户提供 `AuditorAware` Bean 时，JPA Auditing 才会启用

---

#### 2.2.2 imports 文件

**已存在**，无需修改：
```
com.cartisan.data.jpa.config.CartisanDataJpaAutoConfiguration
```

---

### 2.3 cartisan-event 模块

#### 2.3.1 修改：CartisanEventAutoConfiguration

**职责**：配置领域事件发布器

**变更说明**：

| 变更项 | 原状态 | 新状态 |
|-------|--------|--------|
| 配置注解 | `@Configuration` | `@AutoConfiguration` |

**伪代码**：

```java
// 变更前
@Configuration
@ConditionalOnMissingBean(DomainEventPublisher.class)
public class CartisanEventAutoConfiguration {
    @Bean
    public DomainEventPublisher domainEventPublisher(ApplicationEventPublisher aep) {
        return new SpringDomainEventPublisher(aep);
    }
}

// 变更后
@AutoConfiguration  // 改为 @AutoConfiguration
@ConditionalOnMissingBean(DomainEventPublisher.class)
public class CartisanEventAutoConfiguration {
    // 内容不变
}
```

**设计说明**：
- 保持 `@ConditionalOnMissingBean`，允许用户替换 `DomainEventPublisher` 实现
- 例如：替换为 Kafka 事件发布器、事件存储发布器等

---

#### 2.3.2 imports 文件

**已存在**，无需修改：
```
com.cartisan.event.config.CartisanEventAutoConfiguration
```

---

## 三、依赖关系

### 3.1 AutoConfiguration 之间的依赖

```
CartisanEventAutoConfiguration
        ↓
CartisanDataJpaAutoConfiguration
        ↓
CartisanWebAutoConfiguration
```

**说明**：
- `CartisanDataJpaAutoConfiguration` 依赖 `CartisanEventAutoConfiguration`（需要 `DomainEventPublisher`）
- `CartisanWebAutoConfiguration` 无依赖，可独立加载
- 当前不需要显式声明 `@AutoConfigureBefore/After`，Spring Boot 根据 Bean 依赖关系自动处理

### 3.2 外部依赖

| AutoConfiguration | 外部依赖 | 来源 |
|------------------|---------|------|
| CartisanDataJpaAutoConfiguration | `DomainEventPublisher` | cartisan-event |
| CartisanDataJpaAutoConfiguration | `AuditorAware`（可选） | 用户或 cartisan-security |

---

## 四、错误处理

### 4.1 缺失必需依赖

| 场景 | 行为 | 用户解决方案 |
|-----|------|-------------|
| 缺少 `DomainEventPublisher` | 启动失败，提示缺少 Bean | 引入 cartisan-event 依赖 |
| 缺少 `AuditorAware` | JPA Auditing 不启用，不报错 | 提供 `AuditorAware` Bean（可选） |

### 4.2 条件装配失效

| 场景 | 行为 |
|-----|------|
| 非 Web 应用引入 cartisan-web | `CartisanWebAutoConfiguration` 不生效，不注册任何 Bean |
| 用户自定义 `DomainEventPublisher` | 默认的 `SpringDomainEventPublisher` 不注册 |

---

## 五、数据结构

### 5.1 无新增数据结构

本 Feature 仅做配置整合，不新增：
- HTTP 接口
- Command/Query Record
- Response Record
- 错误码枚举

---

## 六、核心流程

### 6.1 应用启动流程

```
1. Spring Boot 启动
   ↓
2. 读取 META-INF/spring/...AutoConfiguration.imports
   ↓
3. 加载 CartisanWebAutoConfiguration
   ├── @ConditionalOnWebApplication 检查
   └── 注册 RequestContextFilter、GlobalExceptionHandler
   ↓
4. 加载 CartisanEventAutoConfiguration
   ├── @ConditionalOnMissingBean(DomainEventPublisher.class) 检查
   └── 注册 SpringDomainEventPublisher
   ↓
5. 加载 CartisanDataJpaAutoConfiguration
   ├── 注入 DomainEventPublisher（来自步骤4）
   ├── 注册 Runnable configureDomainEventPublisherHolder
   └── @Import JpaAuditingConfiguration
       └── @ConditionalOnBean(AuditorAware.class) 检查
           └── 启用/跳过 JPA Auditing
   ↓
6. 应用就绪
```

---

## 七、变更汇总

| 模块 | 文件 | 操作 | 说明 |
|-----|------|------|------|
| cartisan-web | `CartisanWebAutoConfiguration.java` | 新增 | Web 模块自动配置 |
| cartisan-web | `RequestContextFilter.java` | 修改 | 移除 @Component，实现 Ordered |
| cartisan-web | `GlobalExceptionHandler.java` | 确认 | 保留 @ControllerAdvice |
| cartisan-web | `...AutoConfiguration.imports` | 新增 | 声明自动配置 |
| cartisan-data-jpa | `CartisanDataJpaAutoConfiguration.java` | 修改 | 添加 @Import |
| cartisan-event | `CartisanEventAutoConfiguration.java` | 修改 | @Configuration → @AutoConfiguration |
