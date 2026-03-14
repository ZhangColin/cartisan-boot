# Feature: F03-06 AuthenticationService — 测试规格

## 测试策略

采用单元测试覆盖所有核心逻辑，使用 Mockito 静态方法 mock StpUtil。

### 测试框架

- JUnit 5 + AssertJ
- Mockito（静态方法 mock）

### Mock 策略

| 依赖 | Mock 方式 | 原因 |
|------|----------|------|
| StpUtil | mockStatic() | Sa-Token 静态方法 |

## 测试覆盖矩阵

### authenticate

| AC | 测试方法 | 输入 | 预期输出 |
|----|---------|------|---------|
| AC4 | `given_defaultAuthenticate_when_call_then_throwException()` | username/password | UnsupportedOperationException |

### login

| AC | 测试方法 | 输入 | 预期输出 |
|----|---------|------|---------|
| AC1 | `given_validLoginId_when_login_then_returnTokenInfo()` | loginId=123 | TokenInfo |
| - | `given_nullLoginId_when_login_then_throwNullPointerException()` | loginId=null | NullPointerException |

### logout

| AC | 测试方法 | 输入 | 预期输出 |
|----|---------|------|---------|
| AC2 | `given_loggedIn_when_logout_then_sessionDestroyed()` | - | 调用 StpUtil.logout() |
| - | `given_notLoggedIn_when_logout_then_noException()` | - | 无异常 |

### getTokenInfo

| AC | 测试方法 | 输入 | 预期输出 |
|----|---------|------|---------|
| AC3 | `given_loggedIn_when_getTokenInfo_then_returnTokenInfo()` | 已登录 | TokenInfo |
| AC6 | `given_notLoggedIn_when_getTokenInfo_then_returnNull()` | 未登录 | null |

### TokenInfo

| AC | 测试方法 | 输入 | 预期输出 |
|----|---------|------|---------|
| AC5 | `given_tokenInfo_when_create_then_allFieldsNotNull()` | 有效参数 | TokenInfo |
| - | `given_nullToken_when_create_then_throwNullPointerException()` | token=null | NullPointerException |
| - | `given_nullLoginId_when_create_then_throwNullPointerException()` | loginId=null | NullPointerException |
| - | `given_nullExpireTime_when_create_then_throwNullPointerException()` | expireTime=null | NullPointerException |

## 验收标准测试映射

| AC | 描述 | 测试方法 | 状态 |
|----|------|---------|------|
| AC1 | login() 创建会话并返回 TokenInfo | `given_validLoginId_when_login_then_returnTokenInfo()` | ✅ |
| AC2 | logout() 销毁会话 | `given_loggedIn_when_logout_then_sessionDestroyed()` | ✅ |
| AC3 | getTokenInfo() 已登录返回 TokenInfo | `given_loggedIn_when_getTokenInfo_then_returnTokenInfo()` | ✅ |
| AC4 | authenticate() 默认抛异常 | `given_defaultAuthenticate_when_call_then_throwException()` | ✅ |
| AC5 | TokenInfo 字段非空 | `given_tokenInfo_when_create_then_allFieldsNotNull()` | ✅ |
| AC6 | getTokenInfo() 未登录返回 null | `given_notLoggedIn_when_getTokenInfo_then_returnNull()` | ✅ |

## 测试运行命令

```bash
# 运行所有测试
./gradlew :cartisan-security:test

# 只运行 AuthenticationService 测试
./gradlew :cartisan-security:test --tests "SaTokenAuthenticationServiceTest"
```

## 测试统计

- **测试类数量**：1
- **测试方法数量**：11
- **测试通过率**：100%

## 参考

- Requirement Spec: [01_requirement.md](./01_requirement.md)
- Interface Spec: [02_interface.md](./02_interface.md)
- Implementation Plan: [03_implementation.md](./03_implementation.md)
