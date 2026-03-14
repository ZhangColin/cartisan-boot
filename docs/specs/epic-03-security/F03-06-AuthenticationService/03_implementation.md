# Feature: F03-06 AuthenticationService — 实施计划

## 目标复述

定义 `AuthenticationService` 接口并提供 Sa-Token 实现，封装登录/登出/Token查询能力。业务层通过此接口管理会话，不直接依赖 Sa-Token。

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java` | 接口定义 |
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/authentication/SaTokenAuthenticationService.java` | 实现类 |
| 新增 | `cartisan-security/src/main/java/com/cartisan/security/authentication/TokenInfo.java` | Token 信息 Record |
| 新增 | `cartisan-security/src/test/java/com/cartisan/security/authentication/SaTokenAuthenticationServiceTest.java` | 单元测试 |

## 原子任务清单

### Step 1: 创建 TokenInfo Record

- **文件**：`TokenInfo.java`
- **内容**：
  - record 声明：token、loginId、expireTime
  - Compact constructor 校验非空
- **验证**：编译通过

### Step 2: 创建 AuthenticationService 接口

- **文件**：`AuthenticationService.java`
- **内容**：
  - `authenticate()` default 方法（抛异常）
  - `login(Long loginId)` 抽象方法
  - `logout()` 抽象方法
  - `getTokenInfo()` 抽象方法
  - JavaDoc 完整
- **验证**：编译通过

### Step 3: 编写单元测试（红灯）

- **文件**：`SaTokenAuthenticationServiceTest.java`
- **内容**：基于 AC 编写测试：
  - AC1: login() 创建会话并返回 TokenInfo
  - AC2: logout() 销毁会话
  - AC3: getTokenInfo() 已登录返回 TokenInfo，未登录返回 null
  - AC4: authenticate() 默认实现抛异常
  - AC5: TokenInfo 字段非空
  - AC6: getTokenInfo() 未登录返回 null
- **验证**：编译通过 + 测试红灯

### Step 4: 实现 SaTokenAuthenticationService

- **文件**：`SaTokenAuthenticationService.java`
- **内容**：
  - 实现 `login(Long loginId)`
  - 实现 `logout()`
  - 实现 `getTokenInfo()`
- **验证**：测试全绿

### Step 5: 运行完整测试套件

- **命令**：`./gradlew :cartisan-security:test`
- **验证**：所有测试通过

## 测试策略

### 测试命名规范

遵循 SKILL.md TEST-002：`given_{条件}_when_{操作}_then_{预期结果}`

### Mock 策略

| 依赖 | Mock 方式 | 原因 |
|------|----------|------|
| StpUtil | mockStatic() | Sa-Token 静态方法 |

### 测试场景覆盖

| AC | 测试方法 |
|----|---------|
| AC1 | `given_validLoginId_when_login_then_returnTokenInfo()` |
| AC2 | `given_loggedIn_when_logout_then_sessionDestroyed()` |
| AC3 | `given_loggedIn_when_getTokenInfo_then_returnTokenInfo()` |
| AC4 | `given_defaultAuthenticate_when_call_then_throwException()` |
| AC5 | `given_tokenInfo_when_create_then_allFieldsNotNull()` |
| AC6 | `given_notLoggedIn_when_getTokenInfo_then_returnNull()` |

## 预估工作量

| Step | 预估行数 |
|------|---------|
| Step 1 | 15 行 |
| Step 2 | 30 行 |
| Step 3 | 80 行 |
| Step 4 | 40 行 |
| Step 5 | 0 行（验证） |
| **总计** | **~165 行** |

符合复杂度 M 级别（80-200 行）。

## 参考

- Requirement Spec: [01_requirement.md](./01_requirement.md)
- Interface Spec: [02_interface.md](./02_interface.md)
