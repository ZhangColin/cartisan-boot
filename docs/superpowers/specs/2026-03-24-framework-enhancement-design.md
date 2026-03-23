# cartisan-boot 框架增强设计文档

> 基于 cartisan-by-spring-boot-2 与 cartisan-boot 的对比分析
> 设计日期：2026-03-24

## 决策原则

1. **统一风格**：不允许混用，要么全部自动包装，要么全部手动
2. **使用成熟方案**：不增加复杂度的情况下使用行业方案
3. **框架边界**：框架层不做业务支撑域的事情
4. **Sa-Token 原则**：不做 Sa-Token 之外的实现，只做友好包装

---

## 功能清单

| 编号 | 功能 | 模块 | 优先级 |
|------|------|------|--------|
| F00-01 | 防重提交 | cartisan-web | 高 |
| F00-03 | @Condition 注解 | cartisan-data-jpa | 高 |
| F00-04 | Jackson 全局配置 | cartisan-web | 高 |
| F00-05 | RedisKey 工具 | cartisan-core | 中 |
| F00-06 | DomainMapper 默认方法 | cartisan-web | 中 |
| F00-07 | TreeNode 树构建器 | cartisan-web | 中 |
| F00-08 | RequestLogFilter | cartisan-web | 中 |
| F00-09 | MDC 集成 | cartisan-web | 中 |
| F00-10 | 自动响应包装 | cartisan-web | 低 |
| F00-11 | 用户踢出包装 | cartisan-security | 低 |

---

## 按模块详细设计

### cartisan-data-jpa 模块

#### F00-03 @Condition 注解

**目的**：简化 JPA 条件查询，通过注解自动生成 Specification

**包结构**：
```
com.cartisan.data.jpa.specification/
├── Condition.java                    # 注解定义
├── ConditionType.java                # 查询类型枚举
├── ConditionSpecifications.java      # Specification 生成器
└── package-info.java
```

**@Condition 注解**：
```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {
    String propName() default "";      // 实体属性名，默认与字段名相同
    ConditionType type() default ConditionType.EQUAL;
    String blurry() default "";        // 多字段模糊搜索，逗号分隔
}
```

**ConditionType 枚举**（11 种）：
- EQUAL, NOT_EQUAL
- GREATER_EQUAL, GREATER, LESS_EQUAL, LESS
- INNER_LIKE, LEFT_LIKE, RIGHT_LIKE
- IN, BETWEEN

**使用示例**：
```java
// 查询条件 DTO
public record UserQuery(
    @Condition(type = ConditionType.INNER_LIKE) String username,

    @Condition(propName = "status", type = ConditionType.EQUAL) Integer status,

    @Condition(blurry = "title,content") String keyword
) {}

// Repository 使用
public interface UserRepository extends BaseRepository<User, Long> {
    default List<User> search(UserQuery query) {
        return findAll(ConditionSpecifications.of(query));
    }
}
```

---

### cartisan-core 模块

#### F00-05 RedisKey 工具

**目的**：统一管理 Redis Key 的前缀和过期时间

**包结构**：
```
com.cartisan.core.util/
└── RedisKey.java
```

**设计**：
```java
public final class RedisKey {
    private final String prefix;
    private final long expireSeconds;  // 0 表示永不过期

    private RedisKey(String prefix, long expireSeconds) {
        this.prefix = prefix;
        this.expireSeconds = expireSeconds;
    }

    public String key(String suffix) {
        return prefix + ":" + suffix;
    }

    public long expireSeconds() {
        return expireSeconds;
    }

    public boolean isPermanent() {
        return expireSeconds == 0;
    }

    public static RedisKey of(String prefix, long expireSeconds) {
        return new RedisKey(prefix, expireSeconds);
    }

    public static RedisKey permanent(String prefix) {
        return new RedisKey(prefix, 0);
    }
}
```

**使用示例**：
```java
// 定义
private static final RedisKey USER_CACHE_KEY = RedisKey.of("user:cache", 3600);

// 使用
String key = USER_CACHE_KEY.key(userId);
redisTemplate.opsForValue().set(key, value, USER_CACHE_KEY.expireSeconds(), TimeUnit.SECONDS);
```

---

### cartisan-web 模块

#### F00-01 防重提交

**目的**：防止用户短时间内重复提交表单

**包结构**：
```
com.cartisan.web.resubmit/
├── PreventResubmit.java              # 注解
├── ResubmitAspect.java               # 切面
├── ResubmitLock.java                 # 锁管理器
└── package-info.java
```

**@PreventResubmit 注解**：
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventResubmit {
    int delaySeconds() default 20;     // 防重提交时间窗口
    String prefix() default "";        // Redis key 前缀
}
```

**ResubmitLock（Redis 版本）**：
```java
public class ResubmitLock {
    private final StringRedisTemplate redisTemplate;

    public String generateKey(String prefix, String argsHash) {
        return "resubmit:" + prefix + ":" + argsHash;
    }

    public boolean lock(String key, int delaySeconds) {
        Boolean absent = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", delaySeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(absent);
    }
}
```

**使用示例**：
```java
@PostMapping("/users")
@PreventResubmit(delaySeconds = 10)
public Result<Void> createUser(@RequestBody CreateUserRequest request) {
    // ...
}
```

---

#### F00-04 Jackson 全局配置

**目的**：统一 JSON 序列化行为，解决 JavaScript Long 精度问题

**包结构**：
```
com.cartisan.web.config/
└── JacksonConfiguration.java
```

**配置项**：
```java
@Bean
public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
    return builder -> builder
        // Long → 字符串（解决 JS 精度问题）
        .modules(longModule())

        // LocalDateTime → ISO 8601
        .modules(javaTimeModule())

        // BigDecimal → 禁用科学计数法
        .featuresToEnable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)

        // Enum → 字符串
        .featuresToDisable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)

        // 忽略未知属性
        .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
}
```

---

#### F00-06 DomainMapper 默认方法

**目的**：提供批量转换的默认实现

**修改文件**：`cartisan-web/src/main/java/com/cartisan/web/mapper/DomainMapper.java`

```java
public interface DomainMapper<S, T> {
    T convert(S source);

    default List<T> convertList(List<S> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream()
            .map(this::convert)
            .toList();
    }

    default Set<T> convertSet(Set<S> sources) {
        if (sources == null || sources.isEmpty()) {
            return Set.of();
        }
        return sources.stream()
            .map(this::convert)
            .collect(Collectors.toSet());
    }
}
```

---

#### F00-07 TreeNode 树构建器

**目的**：为前端树组件提供统一的树结构数据

**包结构**：
```
com.cartisan.web.support/
├── TreeNode.java                     # 树节点
├── TreeNodeBuilder.java              # 树构建器
└── package-info.java
```

**TreeNode 设计**（泛型版本）：
```java
public class TreeNode<T> {
    private T id;
    private String name;
    private T parentId;
    private List<TreeNode<T>> children;

    // 构造器、getter/setter
}
```

**TreeNodeBuilder**（使用 Stream API）：
```java
public final class TreeNodeBuilder {
    public static <T> List<TreeNode<T>> build(
            List<TreeNode<T>> nodes,
            Function<T, String> idMapper,
            Function<T, String> parentIdMapper,
            T rootParentId) {
        Map<String, List<TreeNode<T>>> grouped = nodes.stream()
            .collect(Collectors.grouping(
                node -> parentIdMapper.apply(node.getParentId())
            ));

        nodes.forEach(node -> {
            String nodeId = idMapper.apply(node.getId());
            node.setChildren(grouped.getOrDefault(nodeId, List.of()));
        });

        return grouped.getOrDefault(String.valueOf(rootParentId), List.of());
    }
}
```

---

#### F00-08 RequestLogFilter

**目的**：记录请求日志，便于排查问题

**包结构**：
```
com.cartisan.web.filter/
├── RequestLogFilter.java
└── package-info.java
```

**设计要点**：
- INFO 级别
- 记录：requestId、IP、方法、URI、Body（POST/PUT）、参数
- 排除：swagger、druid、actuator 等路径
- 使用 ContentCachingRequestWrapper 缓存请求体

---

#### F00-09 MDC 集成

**目的**：将 requestId 放入 MDC，日志格式中可引用

**修改文件**：`cartisan-web/src/main/java/com/cartisan/web/context/RequestContextFilter.java`

**修改要点**：
1. 请求开始时：`MDC.put("requestId", requestId)`
2. 请求结束时：`MDC.clear()`
3. 响应头添加：`X-Request-Id`

**logback 配置示例**：
```xml
<pattern>%d{HH:mm:ss.SSS} [%thread] [%X{requestId}] %-5level %logger{36} - %msg%n</pattern>
```

---

#### F00-10 自动响应包装

**目的**：统一接口返回格式

**包结构**：
```
com.cartisan.web.response/
├── Result.java                       # 统一响应格式
├── ResponseBodyAdvice.java           # 响应包装器
└── package-info.java
```

**Result 设计**：
```java
public record Result<T>(
    int code,
    String message,
    T data
) {
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
```

**ResponseBodyAdvice**：
- 排除已包装的 Result 类型
- 排除 swagger 等路径
- String 类型特殊处理

---

### cartisan-security 模块

#### F00-11 用户踢出包装

**目的**：封装 Sa-Token 的踢出功能

**修改文件**：扩展 `AuthenticationService` 接口

```java
public interface AuthenticationService {
    // 现有方法...

    default void kickout(Long loginId) {
        StpUtil.kickout(loginId);
    }

    default void kickoutByUsername(String username) {
        // 根据业务逻辑实现
    }
}
```

---

## 实现顺序

1. **高优先级**（3 个）：
   - F00-03 @Condition 注解
   - F00-04 Jackson 全局配置
   - F00-01 防重提交

2. **中优先级**（5 个）：
   - F00-05 RedisKey 工具
   - F00-06 DomainMapper 默认方法
   - F00-07 TreeNode 树构建器
   - F00-08 RequestLogFilter
   - F00-09 MDC 集成

3. **低优先级**（2 个）：
   - F00-10 自动响应包装
   - F00-11 用户踢出包装

---

## 参考文档

- 旧框架：`~/workspace/cartisan-by-spring-boot-2/`
- 讨论结果：`docs/superpowers/specs/framework-enhancement-discussion-results.md`
