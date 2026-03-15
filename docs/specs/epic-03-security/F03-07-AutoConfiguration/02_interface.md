# Feature: F03-07 自动配置 — 接口契约

> **Phase**: Design — 接口设计与技术方案
> **依赖**: F03-01 至 F03-06

---

## 1. 接口定义

### 1.1 配置属性接口

**类名**: `CartisanSecurityProperties`
**包路径**: `com.cartisan.security.config.properties`
**注解**: `@ConfigurationProperties("cartisan.security.interceptor")`

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `pathPatterns` | `List<String>` | `["/**"]` | 拦截器生效的路径模式（Ant 风格） |
| `excludePathPatterns` | `List<String>` | `["/error", "/actuator/**"]` | 排除的路径模式 |

**方法签名**:
```java
List<String> getPathPatterns();
void setPathPatterns(List<String> pathPatterns);
List<String> getExcludePathPatterns();
void setExcludePathPatterns(List<String> excludePathPatterns);
```

---

### 1.2 主配置类接口

**类名**: `CartisanSecurityAutoConfiguration`
**包路径**: `com.cartisan.security.config`

**注解**:
```java
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(cn.dev33.satoken.stp.StpUtil.class)
@EnableConfigurationProperties(CartisanSecurityProperties.class)
@Import(SecurityInterceptorConfig.class)
```

**职责**:
- 作为自动配置的唯一入口
- 声明装配条件（Web 应用 + Sa-Token 存在）
- 启用配置属性绑定
- 导入拦截器配置类

---

### 1.3 拦截器配置类接口

**类名**: `SecurityInterceptorConfig`
**包路径**: `com.cartisan.security.config`

**注解**:
```java
@Configuration
@ConditionalOnBean(SecurityInterceptor.class)
```

**实现的接口**: `org.springframework.web.servlet.config.annotation.WebMvcConfigurer`

**依赖注入**:
```java
private final SecurityInterceptor securityInterceptor;
private final CartisanSecurityProperties properties;
```

**方法签名**:
```java
/**
 * 注册 SecurityInterceptor 到 Spring MVC 拦截器链。
 *
 * @param registry Spring MVC 拦截器注册表
 */
void addInterceptors(InterceptorRegistry registry);
```

**伪代码实现**:
```java
void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(securityInterceptor)
        .addPathPatterns(properties.getPathPatterns().toArray(new String[0]))
        .excludePathPatterns(properties.getExcludePathPatterns().toArray(new String[0]));
}
```

---

## 2. 文件结构

### 2.1 Java 源文件

```
com.cartisan.security.config/
├── CartisanSecurityAutoConfiguration.java      # 主配置类
├── SecurityInterceptorConfig.java              # 拦截器配置
└── properties/
    └── CartisanSecurityProperties.java         # 配置属性
```

### 2.2 AutoConfiguration.imports

**文件路径**: `cartisan-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**内容**:
```
com.cartisan.security.config.CartisanSecurityAutoConfiguration
```

---

## 3. 核心流程

### 3.1 自动配置加载流程

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Spring Boot 启动                                         │
│    扫描 META-INF/spring/...AutoConfiguration.imports        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. 发现 CartisanSecurityAutoConfiguration                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. 条件检查                                                 │
│    ├─ @ConditionalOnWebApplication    → 是否 Web 应用?      │
│    └─ @ConditionalOnClass(StpUtil)    → Sa-Token 存在?      │
│    任一不满足 → 跳过配置                                     │
└────────────────────────┬────────────────────────────────────┘
                         │ 条件满足
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. 加载 CartisanSecurityProperties                          │
│    ├─ 读取 application.yml 中的 cartisan.security.*         │
│    ├─ 应用默认值 (path-patterns=/**, exclude=/error,...)    │
│    └─ 覆盖默认值（如果配置了的话）                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. 加载 SecurityInterceptorConfig                           │
│    ├─ @ConditionalOnBean(SecurityInterceptor.class)         │
│    ├─ 注入已有的 SecurityInterceptor                        │
│    ├─ 注入 CartisanSecurityProperties                       │
│    └─ 注册拦截器到 InterceptorRegistry                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. 应用就绪                                                 │
│    SecurityInterceptor 生效，按 path-patterns 配置拦截      │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 拦截器执行流程

```
HTTP 请求到达
    │
    ▼
匹配 path-patterns?
    │ No  → 直接放行
    │ Yes
    ▼
匹配 exclude-path-patterns?
    │ Yes → 直接放行
    │ No
    ▼
SecurityInterceptor.preHandle()
    │
    ├─ 检查 @RequireAuth
    ├─ 检查 @RequireRole
    └─ 检查 @RequirePermission
    │
    ▼
继续处理请求
```

---

## 4. 配置示例

### 4.1 默认配置（无需配置文件）

```yaml
# 无需编写配置，使用默认值
# 默认行为：
#   - 拦截 /** 所有路径
#   - 排除 /error、/actuator/**
```

### 4.2 仅保护 API 路径

```yaml
cartisan:
  security:
    interceptor:
      path-patterns:
        - "/api/**"
```

### 4.3 完整配置

```yaml
cartisan:
  security:
    interceptor:
      path-patterns:
        - "/api/**"
        - "/admin/**"
        - "/internal/**"
      exclude-path-patterns:
        - "/api/public/**"
        - "/api/health"
        - "/error"
        - "/actuator/**"
```

---

## 5. 错误处理

| 场景 | 行为 |
|------|------|
| 非 Web 应用 | 自动配置不生效，组件不加载 |
| Sa-Token 不存在 | 自动配置不生效，组件不加载 |
| SecurityInterceptor Bean 不存在 | SecurityInterceptorConfig 不加载 |
| 配置属性格式错误 | Spring Boot 启动失败，提示配置绑定错误 |

---

## 6. 与已有组件的关系

| 组件 | 是否需要自动配置注册 | 原因 |
|------|---------------------|------|
| `SecurityInterceptor` | **需要** | 需要注册到 InterceptorRegistry |
| `TenantContextFilter` | 不需要 | 已有 `@Component`，自动扫描生效 |
| `SaTokenAuthenticationService` | 不需要 | 已有 `@Component`，自动扫描生效 |
| `SecurityExceptionHandler` | 不需要 | 已有 `@ControllerAdvice`，自动扫描生效 |

---

## 7. 技术约束

| 约束项 | 值 |
|--------|-----|
| Java 版本 | 21+ |
| Spring Boot | 3.4+ |
| 配置属性类型 | 可变 `List<String>`（`ArrayList`） |
| 条件装配 | `@ConditionalOnWebApplication` + `@ConditionalOnClass(StpUtil.class)` |
| Bean 条件 | `@ConditionalOnBean(SecurityInterceptor.class)` |

---

## 8. 参考文档

- 需求规格: [01_requirement.md](01_requirement.md)
- Spring Boot AutoConfiguration: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration
- @ConfigurationProperties: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties
