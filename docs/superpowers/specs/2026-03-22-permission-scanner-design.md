# @RequirePermission 注解改造与权限扫描设计

**日期**：2026-03-22
**状态**：已批准

## 1. 概述

为 cartisan-boot 框架增加权限定义扫描能力，支持业务系统自动采集代码中的权限注解，同步到权限管理界面。

### 1.1 核心改动

1. `@RequirePermission` 注解改造：单值设计，增加 `name` 和 `scope` 可选属性
2. 新增 `Permission` 数据类：承载扫描结果
3. 新增 `PermissionScanner` 接口及实现：扫描 Controller 方法上的权限注解
4. 集成到 `cartisan-security` 模块：自动注册为 Spring Bean

### 1.2 设计原则

- **最小化改动**：不破坏现有 API
- **保持简洁**：满足需求即可，不过度设计
- **Spring 原生**：利用现有基础设施

## 2. 接口设计

### 2.1 改造后的 `@RequirePermission` 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /**
     * 权限 code，格式：{context}:{module}:{action}
     * 示例：admin:user:read
     */
    String value();

    /**
     * 权限显示名称，用于界面展示
     * 未填时使用 code 作为 name
     */
    String name() default "";

    /**
     * 权限作用域，用于区分不同系统/范围
     * 未填时（空字符串）扫描时转为 null，表示全局权限
     */
    String scope() default "";
}
```

**变更点**：
- `value` 从 `String[]` 改为 `String`（单值设计）
- 新增 `name` 属性（可选）
- 新增 `scope` 属性（可选）
- `@Target` 移除 `ElementType.TYPE`，仅支持方法级别

### 2.2 `Permission` 数据类

```java
package com.cartisan.security.permission;

public record Permission(
    String code,      // 权限 code
    String name,      // 显示名称（未填时同 code）
    String scope      // 作用域（未填时 null）
) {}
```

### 2.3 `PermissionScanner` 接口

```java
package com.cartisan.security.permission;

public interface PermissionScanner {
    /**
     * 按 scope 过滤扫描
     * @param scope 作用域，null 表示只扫描未设置 scope 的权限
     */
    List<Permission> scanByScope(String scope);

    /**
     * 扫描全部权限
     */
    List<Permission> scanAll();
}
```

## 3. 实现细节

### 3.1 `DefaultPermissionScanner` 实现

**扫描逻辑**：
- 注入 Spring 的 `RequestMappingHandlerMapping`，获取所有 `HandlerMethod`
- 遍历每个 `HandlerMethod`，检查方法上的 `@RequirePermission` 注解
- 提取注解属性，构造 `Permission` 对象
- 处理默认值：空字符串 `name` → 使用 `code`；空字符串 `scope` → 转为 `null`

**缓存策略**：
- 启动时首次调用时触发扫描
- 按维度缓存：`allPermissions`（全部）、`scopedPermissions`（按 scope）
- 使用 `ConcurrentHashMap` 存储，无刷新机制

### 3.2 自动配置

- 在 `CartisanSecurityAutoConfiguration` 中注册 `PermissionScanner` Bean
- 无需额外配置，默认启用

### 3.3 `SecurityInterceptor` 兼容性

- 更新 `SecurityInterceptor` 适配新的单值注解结构
- 保持原有鉴权逻辑不变（调用 Sa-Token 的 `checkPermissionOr`）

### 3.4 包结构

```
cartisan-security/
└── src/main/java/com/cartisan/security/
    ├── annotation/
    │   └── RequirePermission.java          # 改造
    ├── permission/
    │   ├── Permission.java                 # 新增
    │   ├── PermissionScanner.java          # 新增
    │   └── DefaultPermissionScanner.java   # 新增
    └── config/
        ├── SecurityInterceptor.java        # 适配
        └── CartisanSecurityAutoConfiguration.java  # 注册 Bean
```

## 4. 使用示例

### 4.1 Controller 使用

```java
@RestController
@RequestMapping("/admin/users")
public class UserController {

    @GetMapping
    @RequirePermission(
        value = "admin:user:read",
        name = "平台管理 / 用户管理 / 查看",
        scope = "admin"
    )
    public List<User> list() { ... }

    @PostMapping
    @RequirePermission(
        value = "admin:user:write",
        name = "平台管理 / 用户管理 / 新增",
        scope = "admin"
    )
    public void create(@RequestBody User user) { ... }

    // 简写：不填 name 和 scope
    @DeleteMapping("/{id}")
    @RequirePermission("admin:user:delete")
    public void delete(@PathVariable Long id) { ... }
}
```

### 4.2 业务系统获取权限

```java
@Service
public class PermissionInitService {

    @Autowired
    private PermissionScanner permissionScanner;

    public void initPermissions() {
        // 扫描指定 scope
        List<Permission> adminPermissions = permissionScanner.scanByScope("admin");

        // 扫描全部
        List<Permission> allPermissions = permissionScanner.scanAll();

        // 业务系统自行处理后续逻辑
    }
}
```

## 5. 权限 Code 规范

采用 3 级结构：`{context}:{module}:{action}`

| 部分 | 示例 | 说明 |
|------|------|------|
| context | admin | 限界上下文 |
| module | user | 业务模块 |
| action | read | 操作（read/write/delete） |

示例：`admin:user:read`、`admin:role:write`
