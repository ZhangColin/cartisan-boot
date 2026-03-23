# 可选功能使用指南

本文档介绍 cartisan-boot 框架中的可选功能，包括自动响应包装和用户踢出功能。

## 目录

- [自动响应包装](#自动响应包装-f00-10)
- [用户踢出](#用户踢出-f00-11)

---

## 自动响应包装 (F00-10)

### 功能介绍

自动响应包装功能可以自动将 Controller 返回值包装为统一的 `ApiResponse` 格式，避免在每个接口手动包装。

启用前：
```java
@GetMapping("/user/{id}")
public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return ResponseEntity.ok(ApiResponse.ok(user));
}
```

启用后：
```java
@GetMapping("/user/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}
// 自动包装为: {"code":200,"message":"Success","data":{...},"requestId":null,"errors":null}
```

### 配置方式

在 `application.yml` 中添加配置：

```yaml
cartisan:
  web:
    auto-response:
      enabled: true  # 默认为 false
```

### 排除路径

以下路径默认排除，不会被自动包装：
- `/swagger-ui` - Swagger UI
- `/v3/api-docs` - OpenAPI 文档
- `/actuator` - Spring Boot Actuator

### 使用示例

**Controller 返回简单数据：**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        // 返回: {"code":200,"message":"Success","data":{...}}
        return userService.findById(id);
    }

    @GetMapping
    public List<User> list() {
        // 返回: {"code":200,"message":"Success","data":[...]}
        return userService.findAll();
    }

    @PostMapping
    public User create(@RequestBody CreateUserRequest request) {
        // 返回: {"code":200,"message":"Success","data":{...}}
        return userService.create(request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
        // 返回: {"code":200,"message":"Success","data":null}
    }
}
```

**返回 String 的特殊情况：**

```java
@GetMapping("/hello")
public String hello() {
    // 自动处理 String 类型，避免二次序列化
    return "Hello World";
    // 返回: {"code":200,"message":"Success","data":"Hello World"}
}
```

### 注意事项

1. **统一风格**：启用后应全局使用自动包装，避免部分接口手动包装、部分接口自动包装的混用情况。

2. **避免重复包装**：如果 Controller 已经返回 `ApiResponse`，不会再次包装：

   ```java
   @GetMapping("/manual")
   public ApiResponse<User> manual() {
       // 不会被再次包装
       return ApiResponse.ok(userService.findById(1L));
   }
   ```

3. **异常处理**：异常响应由全局异常处理器统一处理，不受此配置影响。

### 如何禁用

**方式一：配置禁用**

```yaml
cartisan:
  web:
    auto-response:
      enabled: false  # 显式禁用
```

**方式二：不配置（默认禁用）**

如果不配置 `cartisan.web.auto-response.enabled`，功能默认为关闭状态。

---

## 用户踢出 (F00-11)

### 功能介绍

用户踢出功能用于强制用户下线，使其 Token 失效。框架提供两种方式：
- `kickout(loginId)` - 根据 loginId 踢出
- `kickoutByUsername(username)` - 根据用户名踢出（需业务层实现）

### 使用示例

#### 根据 loginId 踢出

```java
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AuthenticationService authenticationService;

    /**
     * 管理员强制用户下线
     */
    public void forceLogout(Long loginId) {
        authenticationService.kickout(loginId);
        // 用户的 Token 将立即失效
    }
}
```

**Controller 示例：**

```java
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/users/{loginId}/kickout")
    public ApiResponse<Void> kickoutUser(@PathVariable Long loginId) {
        adminService.forceLogout(loginId);
        return ApiResponse.ok();
    }
}
```

#### 根据用户名踢出

框架默认实现只提供 `kickout(loginId)`，如需根据用户名踢出，需要业务层实现 username 到 loginId 的映射：

```java
@Service
@RequiredArgsConstructor
public class CustomAuthenticationService extends SaTokenAuthenticationService {

    private final UserRepository userRepository;

    /**
     * 业务层实现：根据用户名查找 loginId 后踢出
     */
    @Override
    public void kickoutByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new DomainException("用户不存在: " + username));

        // 调用框架实现
        kickout(user.getId());
    }
}
```

**使用示例：**

```java
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthenticationService authenticationService;

    @PostMapping("/users/kickout")
    public ApiResponse<Void> kickoutByUsername(@RequestParam String username) {
        authenticationService.kickoutByUsername(username);
        return ApiResponse.ok();
    }
}
```

### Sa-Token 集成说明

框架基于 Sa-Token 的 `StpUtil.kickout(loginId)` 实现踢出功能：
- 踢出后，用户的所有 Token 都会失效
- 踢出不存在的 loginId 不会抛异常（静默处理）
- 被踢出的用户下次请求时会收到认证失败响应

### API 参考

**AuthenticationService 接口：**

```java
/**
 * 踢出指定用户（强制下线）
 * @param loginId 用户标识
 */
void kickout(Long loginId);

/**
 * 根据用户名踢出用户（需业务层实现）
 * @param username 用户名
 */
default void kickoutByUsername(String username) {
    throw new UnsupportedOperationException(
        "Kickout by username not implemented. Override this method in your service.");
}
```

### 注意事项

1. **loginId 为 null**：调用 `kickout(null)` 会抛出 `NullPointerException`。

2. **业务实现**：`kickoutByUsername` 默认抛出 `UnsupportedOperationException`，需要业务层覆盖实现。

3. **权限控制**：踢出操作通常需要管理员权限，建议配合权限注解使用：

   ```java
   @SaCheckPermission("admin:user:kickout")
   @PostMapping("/users/{loginId}/kickout")
   public ApiResponse<Void> kickoutUser(@PathVariable Long loginId) {
       // ...
   }
   ```

---

## 相关文档

- [Web 基础设施指南](web-infrastructure.md) - 统一响应体、异常处理
- [条件注解使用指南](condition-annotation.md) - 条件配置机制
