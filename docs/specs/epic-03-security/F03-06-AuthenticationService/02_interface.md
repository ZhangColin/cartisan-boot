# Feature: F03-06 AuthenticationService — 接口契约

## 类设计

### AuthenticationService 接口

**包路径：** `com.cartisan.security.authentication`

**类声明（伪代码）：**
```java
public interface AuthenticationService {
    // 扩展点：业务层实现
    default Long authenticate(String username, String password);

    // 框架实现：会话管理
    TokenInfo login(Long loginId);
    void logout();
    TokenInfo getTokenInfo();
}
```

---

### SaTokenAuthenticationService 实现类

**包路径：** `com.cartisan.security.authentication`

**类声明（伪代码）：**
```java
public class SaTokenAuthenticationService implements AuthenticationService {
    @Override
    public TokenInfo login(Long loginId);

    @Override
    public void logout();

    @Override
    public TokenInfo getTokenInfo();

    // authenticate() 使用默认实现（抛异常）
}
```

---

### TokenInfo Record

**包路径：** `com.cartisan.security.authentication`

**类声明：**
```java
public record TokenInfo(
    String token,
    Long loginId,
    Instant expireTime
) {
    // Compact constructor for validation
    public TokenInfo {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(loginId, "loginId");
        Objects.requireNonNull(expireTime, "expireTime");
    }
}
```

---

## 接口定义

### authenticate()

**签名：**
```java
default Long authenticate(String username, String password)
```

**描述：** 认证用户身份（业务层实现）

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| username | String | 用户名 |
| password | String | 密码 |

**返回值：**
| 情况 | 返回值 |
|------|--------|
| 认证成功 | loginId（用户标识） |

**异常：**
| 异常类型 | 触发条件 |
|---------|---------|
| `UnsupportedOperationException` | 使用默认实现（框架不提供认证逻辑） |
| 业务自定义异常 | 业务层实现时可能抛出 |

**默认实现：**
```java
default Long authenticate(String username, String password) {
    throw new UnsupportedOperationException(
        "Authentication not implemented. Override this method in your service.");
}
```

---

### login()

**签名：**
```java
TokenInfo login(Long loginId)
```

**描述：** 创建登录会话

**参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| loginId | Long | 用户标识（由业务层认证后提供） |

**返回值：**
| 情况 | 返回值 |
|------|--------|
| 登录成功 | TokenInfo（token、loginId、expireTime） |

**异常：**
| 异常类型 | 触发条件 |
|---------|---------|
| `NullPointerException` | loginId 为 null |

**行为：**
1. 参数校验：`Objects.requireNonNull(loginId)`
2. 调用 `StpUtil.login(loginId)` 创建会话
3. 获取 token：`StpUtil.getTokenValue()`
4. 获取过期时间：`StpUtil.getTokenTimeout()` 转换为 Instant
5. 返回 TokenInfo

**实现伪代码：**
```java
@Override
public TokenInfo login(Long loginId) {
    Objects.requireNonNull(loginId, "loginId");
    StpUtil.login(loginId);
    String token = StpUtil.getTokenValue();
    long timeoutSeconds = StpUtil.getTokenTimeout();
    Instant expireTime = Instant.now().plusSeconds(timeoutSeconds);
    return new TokenInfo(token, loginId, expireTime);
}
```

---

### logout()

**签名：**
```java
void logout()
```

**描述：** 销毁当前登录会话

**参数：** 无

**返回值：** void

**异常：** 无（即使未登录也不抛异常）

**行为：**
1. 调用 `StpUtil.logout()`
2. 未登录时 Sa-Token 内部静默处理，不抛异常

**实现伪代码：**
```java
@Override
public void logout() {
    StpUtil.logout();
}
```

---

### getTokenInfo()

**签名：**
```java
TokenInfo getTokenInfo()
```

**描述：** 获取当前 Token 信息

**参数：** 无

**返回值：**
| 情况 | 返回值 |
|------|--------|
| 已登录 | TokenInfo |
| 未登录 | `null` |

**异常：** 无

**行为：**
1. 检查是否登录：`StpUtil.isLogin()`
2. 未登录返回 `null`
3. 获取 token、loginId、过期时间
4. 返回 TokenInfo

**实现伪代码：**
```java
@Override
public TokenInfo getTokenInfo() {
    if (!StpUtil.isLogin()) {
        return null;
    }
    String token = StpUtil.getTokenValue();
    Long loginId = StpUtil.getLoginIdAsLong();
    long timeoutSeconds = StpUtil.getTokenTimeout();
    Instant expireTime = Instant.now().plusSeconds(timeoutSeconds);
    return new TokenInfo(token, loginId, expireTime);
}
```

---

## 核心流程

### 登录流程

```pseudocode
FUNCTION login(loginId)
    // 1. 参数校验
    IF loginId IS NULL THEN
        THROW NullPointerException("loginId")
    END IF

    // 2. 创建会话
    StpUtil.login(loginId)

    // 3. 获取 Token 信息
    token = StpUtil.getTokenValue()
    timeoutSeconds = StpUtil.getTokenTimeout()
    expireTime = Instant.now() + timeoutSeconds

    // 4. 返回
    RETURN new TokenInfo(token, loginId, expireTime)
END FUNCTION
```

### 登出流程

```pseudocode
FUNCTION logout()
    StpUtil.logout()
    // Sa-Token 内部处理未登录场景，不抛异常
END FUNCTION
```

### 获取 Token 信息流程

```pseudocode
FUNCTION getTokenInfo()
    IF NOT StpUtil.isLogin() THEN
        RETURN NULL
    END IF

    token = StpUtil.getTokenValue()
    loginId = StpUtil.getLoginIdAsLong()
    timeoutSeconds = StpUtil.getTokenTimeout()
    expireTime = Instant.now() + timeoutSeconds

    RETURN new TokenInfo(token, loginId, expireTime)
END FUNCTION
```

---

## 技术要点

### 1. Token 过期时间计算

Sa-Token 的 `getTokenTimeout()` 返回的是剩余秒数，需要转换为 `Instant`：

```java
long timeoutSeconds = StpUtil.getTokenTimeout();
Instant expireTime = Instant.now().plusSeconds(timeoutSeconds);
```

### 2. loginId 类型

Sa-Token 支持 `Object` 类型的 loginId，但本框架统一使用 `Long`：
- `StpUtil.login(Long loginId)` ✅
- `StpUtil.getLoginIdAsLong()` ✅

### 3. 未登录场景处理

| 方法 | 未登录行为 |
|------|-----------|
| `logout()` | 静默处理，不抛异常 |
| `getTokenInfo()` | 返回 `null` |
| `login()` | 正常执行（创建新会话） |

### 4. TokenInfo 不可变

使用 `record` 类型确保不可变性：
- 自动生成 equals/hashCode/toString
- Compact constructor 校验非空

---

## 依赖关系

### 当前 Feature 依赖

| 依赖 | 状态 | 说明 |
|------|------|------|
| F03-01（模块骨架） | ✅ 完成 | cartisan-security 模块已创建 |

### 当前 Feature 被依赖

| 被依赖 | 说明 |
|--------|------|
| F03-07（AutoConfiguration） | 自动注册 AuthenticationService Bean |
| F03-08（集成测试） | 验证登录/登出流程 |

---

## 后续工作（不在本 Feature 范围）

1. **F03-07 AutoConfiguration**
   - 注册 `SaTokenAuthenticationService` Bean
   - 条件装配：Sa-Token 在 classpath 时才生效

2. **业务项目**
   - 实现 `authenticate()` 方法
   - 或在 Controller 中自行验证密码后调用 `login()`

---

## 参考

- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- Requirement Spec: [01_requirement.md](./01_requirement.md)
- Sa-Token 文档: https://sa-token.cc/doc.html#/
