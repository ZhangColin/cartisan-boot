# cartisan-boot 框架增强设计文档

**日期**: 2026-03-23
**状态**: 已实现

## 概述

本文档描述 cartisan-boot 框架的三项增强需求：
1. cartisan-security 支持自定义登录超时
2. cartisan-security 添加获取当前用户 ID 的便捷方法
3. 框架内置 Lombok 支持

## 需求

### 1.1 自定义登录超时

**当前状态**：`AuthenticationService.login(Long loginId)` 使用 Sa-Token 默认超时。

**目标**：支持"记住我"等场景，允许自定义超时时间（如 7 天）。

### 1.2 获取当前用户 ID

**当前状态**：需要 `getTokenInfo().loginId()` 才能获取用户 ID。

**目标**：提供便捷方法直接获取用户 ID。

### 2.1 JPA Auditing 自动配置

**状态**：已实现，无需改动。

### 3 Lombok 支持

**目标**：框架内置 Lombok，减少样板代码。

## 设计

### 1. cartisan-security 模块改动

#### AuthenticationService 接口

**文件**: `cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java`

```java
/**
 * 创建登录会话（自定义超时）。
 * <p>
 * 用于"记住我"等场景，如 7 天免登录。
 * </p>
 *
 * @param loginId        用户标识
 * @param timeoutSeconds 超时秒数（> 0）
 * @return Token 信息
 * @throws NullPointerException     loginId 为 null
 * @throws IllegalArgumentException timeoutSeconds <= 0
 */
TokenInfo login(Long loginId, long timeoutSeconds);

/**
 * 获取当前用户 ID。
 *
 * @return 用户 ID，未登录返回 {@link Optional#empty()}
 */
default Optional<Long> getCurrentUserId() {
    return Optional.ofNullable(getTokenInfo()).map(TokenInfo::loginId);
}
```

#### SaTokenAuthenticationService 实现

**文件**: `cartisan-security/src/main/java/com/cartisan/security/authentication/SaTokenAuthenticationService.java`

```java
@Override
public TokenInfo login(Long loginId, long timeoutSeconds) {
    Objects.requireNonNull(loginId, "loginId");

    if (timeoutSeconds <= 0) {
        throw new IllegalArgumentException("timeoutSeconds must be positive: " + timeoutSeconds);
    }

    StpUtil.login(loginId, timeoutSeconds);

    String token = StpUtil.getTokenValue();
    Instant expireTime = Instant.now().plusSeconds(timeoutSeconds);

    return new TokenInfo(token, loginId, expireTime);
}
```

### 2. Lombok 支持改动

#### cartisan-dependencies BOM

**文件**: `cartisan-dependencies/build.gradle.kts`

```kotlin
dependencies {
    // ... 现有依赖

    // Lombok
    api("org.projectlombok:lombok:1.18.34")
}
```

#### 各模块 build.gradle.kts

**涉及模块**：cartisan-core, cartisan-web, cartisan-security, cartisan-data-jpa, cartisan-data-query, cartisan-event, cartisan-ai, cartisan-test

在每个模块的 `dependencies` 块中添加：

```kotlin
// Lombok（编译时生效，不传递给使用者）
compileOnly("org.projectlombok:lombok")
annotationProcessor("org.projectlombok:lombok")
```

**设计说明**：
- 使用 `compileOnly` 确保不传递给依赖框架的项目
- 框架代码可使用 Lombok 减少样板代码
- 项目使用框架仍需自行添加 Lombok 依赖

## 测试策略

### 单元测试

#### SaTokenAuthenticationServiceTest

| 测试场景 | 预期结果 |
|---------|---------|
| `login(loginId, timeout)` 其中 timeout <= 0 | 抛出 IllegalArgumentException |
| `login(loginId, 86400)` (1天) | TokenInfo.expireTime 约等于 now + 1天 |
| `login(null, timeout)` | 抛出 NullPointerException |

#### AuthenticationService 接口测试（新增）

| 测试场景 | 预期结果 |
|---------|---------|
| `getCurrentUserId()` 未登录 | 返回 Optional.empty() |
| `getCurrentUserId()` 已登录 | 返回 Optional.of(loginId) |

### 集成测试

在 `AuthenticationServiceIntegrationTest` 中添加：
- 验证自定义超时的 token 在指定时间后过期

## 向后兼容性

所有改动都是**新增**，不破坏现有 API：
- 新增 `login(Long, long)` 重载方法
- 新增 `getCurrentUserId()` default 方法
- Lombok 使用 compileOnly，不影响现有项目

## 影响范围

| 模块 | 改动类型 |
|-----|---------|
| cartisan-security | 新增方法 |
| cartisan-dependencies | 新增依赖版本 |
| 所有业务模块 | 新增 Lombok 依赖 |

## 后续工作

- 考虑是否将 `CartisanSecurityProperties` 改为 Lombok @Data
- 评估是否需要更多登录选项（如设备信息、登录地点），届时可重构为参数对象
