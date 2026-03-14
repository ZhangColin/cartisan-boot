# Feature: F03-06 AuthenticationService

## 背景

cartisan-security 需要提供认证服务抽象层，将 Sa-Token 的会话管理能力封装为框架接口。业务代码通过 `AuthenticationService` 接口进行登录/登出操作，不直接依赖 Sa-Token。

## 目标

- 定义 `AuthenticationService` 接口，提供会话管理抽象
- 实现 `SaTokenAuthenticationService`，封装 Sa-Token 的登录/登出/Token查询
- 定义 `TokenInfo` Record，封装 Token 信息
- 支持业务层自定义认证逻辑

## 范围

### 包含（In Scope）

- `AuthenticationService` 接口（login/logout/getTokenInfo/authenticate）
- `SaTokenAuthenticationService` 实现
- `TokenInfo` Record
- 单元测试覆盖所有方法

### 不包含（Out of Scope）

- 用户查询和密码验证（业务层实现）
- 认证策略（密码/OAuth/JWT等）—— 业务层自行选择
- 权限数据加载 —— 由 Sa-Token 的 `StpInterface` 实现

## 接口设计

### AuthenticationService 接口

```java
public interface AuthenticationService {
    /**
     * 认证用户身份（业务层实现）。
     * 框架提供默认实现：抛出 UnsupportedOperationException。
     *
     * @param username 用户名
     * @param password 密码
     * @return 认证成功返回 loginId
     * @throws UnsupportedOperationException 框架默认实现抛出
     */
    default Long authenticate(String username, String password) {
        throw new UnsupportedOperationException(
            "Authentication not implemented. Override this method in your service.");
    }

    /**
     * 创建登录会话。
     *
     * @param loginId 用户标识（由业务层认证后提供）
     * @return Token 信息
     */
    TokenInfo login(Long loginId);

    /**
     * 销毁当前登录会话。
     */
    void logout();

    /**
     * 获取当前 Token 信息。
     *
     * @return Token 信息，未登录返回 null
     */
    TokenInfo getTokenInfo();
}
```

### TokenInfo Record

```java
public record TokenInfo(
    String token,
    Long loginId,
    Instant expireTime
) {}
```

## 验收标准（Acceptance Criteria）

### 主流程

- **AC1**: `login(loginId)` 调用 `StpUtil.login(loginId)` 并返回包含 token、loginId、expireTime 的 TokenInfo
- **AC2**: `logout()` 调用 `StpUtil.logout()`
- **AC3**: `getTokenInfo()` 已登录时返回 TokenInfo，未登录时返回 null

### 边界场景

- **AC4**: `authenticate()` 默认实现抛出 `UnsupportedOperationException`，错误消息提示业务层实现
- **AC5**: TokenInfo 的三个字段均不为 null（token、loginId、expireTime）
- **AC6**: `getTokenInfo()` 在未登录时返回 null，不抛异常

## 使用示例

### 业务层实现认证

```java
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public TokenInfo login(String username, String password) {
        // 1. 业务层验证用户
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new DomainException("用户不存在"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new DomainException("密码错误");
        }
        
        // 2. 调用框架的 login() 创建会话
        return authenticationService.login(user.getId());
    }
}
```

### 框架默认行为

```java
// 未实现 authenticate() 时调用会抛异常
authenticationService.authenticate("admin", "123");
// 抛出 UnsupportedOperationException: Authentication not implemented...
```

## 约束

### 技术约束

- **依赖 Sa-Token**：实现类调用 StpUtil 的静态方法
- **不依赖 SecurityContext**：直接调用 StpUtil，两者并行

### 设计约束

- **单一职责**：只管理会话，不做用户查询和密码验证
- **扩展性**：业务层可覆盖 `authenticate()` 实现自定义认证

## 技术决策

### 决策 1：分离 authenticate 和 login

**问题**：Backlog 描述 `login(username, password)` "委托业务层验证密码"存在职责混淆。

**决策**：
- `authenticate(username, password)` - 业务层实现，验证身份
- `login(loginId)` - 框架实现，创建会话

**理由**：
1. 职责单一：框架只管会话，业务层管认证
2. 灵活性：业务层可选择任意认证策略
3. 清晰：方法名直接表达意图

### 决策 2：authenticate 使用 default 方法

**决策**：`authenticate()` 提供 default 实现，抛出 `UnsupportedOperationException`。

**理由**：
1. 接口不能强制业务层实现（否则失去灵活性）
2. 抛异常明确提示"需要实现"
3. 符合"框架提供扩展点"的设计模式

## 风险评估

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 业务层忘记实现 authenticate | 中 | 低 | 错误消息明确提示 |
| Token 过期时间获取失败 | 低 | 中 | try-catch 返回 null |
| loginId 为 null | 低 | 中 | 参数校验抛异常 |

## 参考

- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- Sa-Token 文档: https://sa-token.cc/doc.html#/
