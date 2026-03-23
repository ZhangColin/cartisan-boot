# Web 基础设施使用指南

> **适用范围**：使用 cartisan-boot 框架的 Spring Boot 业务项目

---

## 一、RedisKey 工具

### 1.1 作用

统一管理 Redis Key 的前缀和过期时间，避免在代码中硬编码 Key 前缀和过期时间。

### 1.2 使用示例

```java
import com.cartisan.core.util.RedisKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    private final StringRedisTemplate redisTemplate;

    // 定义带过期时间的 Key（1 小时）
    private static final RedisKey USER_CACHE_KEY = RedisKey.of("user:cache", 3600);

    // 定义永不过期的 Key
    private static final RedisKey SYSTEM_CONFIG_KEY = RedisKey.permanent("system:config");

    public void cacheUser(Long userId, String userData) {
        String key = USER_CACHE_KEY.key(String.valueOf(userId));
        redisTemplate.opsForValue().set(key, userData, USER_CACHE_KEY.expireSeconds(), TimeUnit.SECONDS);
    }

    public String getUserCache(Long userId) {
        String key = USER_CACHE_KEY.key(String.valueOf(userId));
        return redisTemplate.opsForValue().get(key);
    }
}
```

### 1.3 API 说明

| 方法 | 说明 |
|------|------|
| `RedisKey.of(prefix, expireSeconds)` | 创建带过期时间的 Key |
| `RedisKey.permanent(prefix)` | 创建永不过期的 Key |
| `key(suffix)` | 生成完整的 Redis Key（格式：`prefix:suffix`） |
| `expireSeconds()` | 获取过期时间（秒），0 表示永不过期 |
| `isPermanent()` | 判断是否为永久 Key |

---

## 二、DomainMapper 默认方法

### 2.1 作用

提供 MapStruct 批量转换的默认实现，继承后自动获得 List 和 Set 的转换能力。

### 2.2 使用示例

```java
import com.cartisan.web.mapper.DomainMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper extends DomainMapper<User, UserResponse> {
    // convert 方法由 MapStruct 自动生成实现
    // convertList 和 convertSet 由 DomainMapper 提供
}
```

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id);
        return userMapper.convert(user);  // 单个对象转换
    }

    public List<UserResponse> listUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.convertList(users);  // List 批量转换
    }

    public Set<UserResponse> setUsers() {
        Set<User> users = userRepository.findAllAsSet();
        return userMapper.convertSet(users);  // Set 批量转换
    }
}
```

### 2.3 API 说明

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `convert(S source)` | 转换单个对象（需由 MapStruct 生成） | T |
| `convertList(List<S> sources)` | 批量转换 List | `List<T>` |
| `convertSet(Set<S> sources)` | 批量转换 Set | `Set<T>` |

**注意**：`convertList` 和 `convertSet` 在输入为 null 或空时返回空集合，不会返回 null。

---

## 三、TreeNode 和 TreeNodeBuilder

### 3.1 作用

为前端树组件（如 Element Plus Tree、Ant Design Tree）提供统一的树结构数据。

### 3.2 使用示例

```java
import com.cartisan.web.support.TreeNode;
import com.cartisan.web.support.TreeNodeBuilder;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<TreeNode<Long>> getDepartmentTree() {
        // 1. 从数据库获取扁平的部门列表
        List<Department> departments = departmentRepository.findAll();

        // 2. 转换为 TreeNode
        List<TreeNode<Long>> nodes = departments.stream()
            .map(dept -> new TreeNode<>(
                dept.getId(),
                dept.getName(),
                dept.getParentId()
            ))
            .toList();

        // 3. 构建树形结构
        return TreeNodeBuilder.build(
            nodes,
            id -> String.valueOf(id),           // ID 映射函数
            parentId -> String.valueOf(parentId), // 父 ID 映射函数
            0L                                    // 根节点的父 ID
        );
    }
}
```

### 3.3 TreeNode 结构

```json
{
  "id": 1,
  "name": "部门名称",
  "parentId": 0,
  "children": [
    {
      "id": 2,
      "name": "子部门",
      "parentId": 1,
      "children": []
    }
  ]
}
```

### 3.4 API 说明

**TreeNode 构造方法：**

| 构造方法 | 说明 |
|----------|------|
| `TreeNode()` | 默认构造，children 初始化为空列表 |
| `TreeNode(id, name, parentId)` | 基本构造 |
| `TreeNode(id, name, parentId, children)` | 完整构造 |

**TreeNodeBuilder 静态方法：**

| 方法 | 说明 |
|------|------|
| `build(nodes, idMapper, parentIdMapper, rootParentId)` | 将扁平节点列表构建为树形结构 |

**泛型支持**：`TreeNode<T>` 中的 `T` 可以是 `Long`、`String` 等任意 ID 类型。

---

## 四、@PreventResubmit 防重提交

### 4.1 作用

防止用户在短时间内重复提交表单，基于 Redis 分布式锁实现。

### 4.2 使用示例

```java
import com.cartisan.web.resubmit.PreventResubmit;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreventResubmit(delaySeconds = 10, prefix = "createUser")
    public ApiResponse<Void> createUser(@RequestBody CreateUserRequest request) {
        userService.create(request);
        return ApiResponse.success();
    }

    @PostMapping("/batch")
    @PreventResubmit(delaySeconds = 30)  // 使用默认前缀
    public ApiResponse<Void> batchCreate(@RequestBody List<CreateUserRequest> requests) {
        userService.batchCreate(requests);
        return ApiResponse.success();
    }
}
```

### 4.3 工作原理

1. 拦截带有 `@PreventResubmit` 注解的方法
2. 序列化方法参数生成 MD5 哈希值
3. 使用 Redis SETNX 尝试获取分布式锁
4. 如果获取失败，抛出 `ResubmitException`
5. 如果获取成功，执行目标方法

### 4.4 注解属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `delaySeconds` | int | 20 | 防重提交时间窗口（秒） |
| `prefix` | String | "" | Redis key 前缀，用于区分不同业务场景 |

### 4.5 注意事项

- **Redis 依赖**：需要 Redis 环境支持，否则防重提交功能不生效
- **参数序列化**：使用 FastJSON 序列化参数，确保参数对象可序列化
- **异常处理**：`ResubmitException` 会被全局异常处理器捕获，返回 HTTP 400
- **分布式环境**：基于 Redis 实现，支持分布式部署
- **Key 格式**：`resubmit:{prefix}:{argsHash}`

---

## 五、Jackson 全局配置

### 5.1 作用

统一 JSON 序列化/反序列化行为，解决常见的前后端数据交互问题。

### 5.2 配置项说明

| 配置项 | 作用 | 原因 |
|--------|------|------|
| `Long → String` | 将 Long 类型序列化为字符串 | 解决 JavaScript Long 精度问题（JS Number 最大安全整数是 2^53 - 1） |
| `LocalDateTime → ISO 8601` | 标准日期时间格式 | 统一时间格式，如 `2024-03-24T10:30:00` |
| `BigDecimal → 禁止科学计数法` | 保持金额精度 | 避免 `123456789` 变成 `1.23E+8` |
| `Enum → 字符串` | 枚举值序列化为字符串 | 提高可读性 |
| `忽略未知属性` | 反序列化时忽略未知字段 | 避免字段不匹配导致反序列化失败 |

### 5.3 序列化示例

```json
{
  "id": "123456789012345678",    // Long 转字符串
  "amount": 1234.56,              // BigDecimal 不使用科学计数法
  "status": "ACTIVE",             // Enum 转字符串
  "createTime": "2024-03-24T10:30:00"  // ISO 8601 格式
}
```

### 5.4 自定义配置

如需覆盖默认配置，可以在业务项目中定义自定义的 `Jackson2ObjectMapperBuilderCustomizer`：

```java
@Configuration
public class CustomJacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customJackson() {
        return builder -> builder
            .serializerByType(LocalDateTime.class, new CustomDateTimeSerializer());
    }
}
```

---

## 六、RequestLogFilter

### 6.1 作用

记录 HTTP 请求的基本信息，便于问题排查和日志追踪。

### 6.2 记录内容

| 字段 | 说明 | 来源 |
|------|------|------|
| `requestId` | 请求追踪 ID | 从 `RequestContext` 获取 |
| `clientIp` | 客户端 IP 地址 | 从 `HttpServletRequest` 获取 |
| `method` | HTTP 方法 | GET、POST、PUT、DELETE 等 |
| `uri` | 请求 URI | 包含查询参数的完整路径 |

### 6.3 日志示例

```
INFO  c.c.web.filter.RequestLogFilter - Request: requestId=a1b2c3d4-e5f6-7890-abcd-ef1234567890, ip=192.168.1.100, method=POST, uri=/api/users?page=1&size=10
```

### 6.4 排除路径

以下路径默认不记录日志：

| 路径前缀 | 说明 |
|----------|------|
| `/swagger-ui` | Swagger UI 文档 |
| `/v3/api-docs` | OpenAPI 文档 |
| `/swagger-resources` | Swagger 资源 |
| `/druid` | Druid 监控 |
| `/actuator` | Spring Boot Actuator |

### 6.5 自动注册

`RequestLogFilter` 通过 `CartisanWebAutoConfiguration` 自动注册，无需手动配置。

---

## 七、MDC 集成

### 7.1 作用

将 `requestId` 放入 MDC（Mapped Diagnostic Context），便于在日志中追踪请求链路。

### 7.2 工作原理

1. `RequestContextFilter` 在请求开始时生成 `requestId` 并放入 MDC
2. 请求结束时清理 MDC
3. 日志框架（如 Logback）可以通过 `%X{requestId}` 输出

### 7.3 logback 配置示例

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{requestId}] - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{requestId}, %X{clientIp}] - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

### 7.4 日志输出示例

```
2024-03-24 10:30:00.123 [http-nio-8080-exec-1] INFO  c.e.service.UserService [a1b2c3d4-e5f6-7890-abcd-ef1234567890] - Creating user: John Doe
2024-03-24 10:30:00.456 [http-nio-8080-exec-1] DEBUG c.e.repository.UserRepository [a1b2c3d4-e5f6-7890-abcd-ef1234567890] - Saving user to database
```

### 7.5 在代码中使用 MDC

```java
import org.slf4j.MDC;
import com.cartisan.web.context.RequestContext;

@Service
public class OrderService {

    public void createOrder(OrderRequest request) {
        // requestId 已由 RequestContextFilter 自动放入 MDC
        String requestId = MDC.get("requestId");
        log.info("Processing order with requestId: {}", requestId);

        // 也可以直接从 RequestContext 获取
        String requestId2 = RequestContext.getRequestId();
    }
}
```

### 7.6 注意事项

- **自动清理**：请求结束后会自动清理 MDC，无需手动处理
- **线程安全**：MDC 基于 ThreadLocal，每个请求线程独立
- **异步场景**：异步线程需要手动传递 MDC 值

---

## 八、自动配置说明

### 8.1 CartisanWebAutoConfiguration

cartisan-web 模块通过 Spring Boot AutoConfiguration 自动注册以下组件：

| 组件 | 说明 | 条件 |
|------|------|------|
| `RequestContextFilter` | 请求上下文初始化 | Web 应用 |
| `RequestLogFilter` | 请求日志记录 | Web 应用 |
| `GlobalExceptionHandler` | 全局异常处理 | Web 应用 |
| `ResubmitLock` | 防重复提交锁 | Redis 可用 |
| `ResubmitAspect` | 防重复提交切面 | ResubmitLock 可用 |

### 8.2 排除自动配置

如需自定义某个组件，可以通过以下方式排除：

```java
@SpringBootApplication(exclude = CartisanWebAutoConfiguration.class)
public class Application {
    // 手动注册自定义组件
}
```

---

## 九、完整示例

### 9.1 用户管理 Controller

```java
import com.cartisan.web.mapper.DomainMapper;
import com.cartisan.web.resubmit.PreventResubmit;
import com.cartisan.web.support.TreeNode;
import com.cartisan.web.support.TreeNodeBuilder;
import com.cartisan.core.util.RedisKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private static final RedisKey USER_CACHE = RedisKey.of("user:info", 600);

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        // 先从缓存获取
        String cacheKey = USER_CACHE.key(String.valueOf(id));
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return ApiResponse.success(JSON.parseObject(cached, UserResponse.class));
        }

        // 从数据库获取
        User user = userService.getById(id);
        UserResponse response = userMapper.convert(user);

        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(response), 600, TimeUnit.SECONDS);

        return ApiResponse.success(response);
    }

    @PostMapping
    @PreventResubmit(delaySeconds = 10, prefix = "createUser")
    public ApiResponse<Void> createUser(@RequestBody CreateUserRequest request) {
        userService.create(request);
        return ApiResponse.success();
    }

    @GetMapping("/tree")
    public List<TreeNode<Long>> getDepartmentTree() {
        return userService.getDepartmentTree();
    }
}
```

---

## 相关文档

- [MapStruct 对象映射使用指南](./mapstruct-mapping.md)
- [jOOQ 代码生成配置](./jooq-code-generation.md)
- cartisan-boot 使用手册：[cartisan-boot-使用手册.md](./cartisan-boot-使用手册.md)
