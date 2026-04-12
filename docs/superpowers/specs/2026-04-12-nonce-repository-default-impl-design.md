# NonceRepository 默认实现

## 背景

`NonceRepository` 是 cartisan-openapi 的防重放接口，目前只有接口定义，业务方必须自行实现。每个项目都要写一遍 Redis `SET NX EX`，属于重复劳动。

## 目标

- 开箱即用：引入 `cartisan-openapi` 后，nonce 防重放自动生效
- 生产就绪：Redis 实现作为首选默认
- 可覆盖：用户自定义实现优先

## 设计

### 1. RedisNonceRepository（生产默认）

- 基于 `StringRedisTemplate`，用 `SET NX EX` 原子操作实现 `tryAcquire`
- key 前缀：`openapi:nonce:`，拼接 nonce 值
- `tryAcquire`：调用 `setIfAbsent(key, "1", ttl)`，返回 `true` 表示首次获取

```java
public class RedisNonceRepository implements NonceRepository {
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(String nonce, Duration ttl) {
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent("openapi:nonce:" + nonce, "1", ttl);
        return Boolean.TRUE.equals(result);
    }
}
```

### 2. InMemoryNonceRepository（兜底）

- 基于 `ConcurrentHashMap<String, Long>`，value 存过期时间戳（毫秒）
- `tryAcquire`：先懒清理过期条目，再 `putIfAbsent`
- 不做定时清理，依赖每次调用时的懒淘汰
- 适用场景：单实例开发/测试、不需要防重放的轻量场景

```java
public class InMemoryNonceRepository implements NonceRepository {
    private final ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String nonce, Duration ttl) {
        long expireAt = System.currentTimeMillis() + ttl.toMillis();
        // 懒清理：如果已存在且未过期，返回 false
        Long existing = store.get(nonce);
        if (existing != null && existing > System.currentTimeMillis()) {
            return false;
        }
        if (existing != null) {
            store.remove(nonce, existing); // 已过期，移除
        }
        Long prev = store.putIfAbsent(nonce, expireAt);
        if (prev != null && prev > System.currentTimeMillis()) {
            return false; // 并发竞争，其他线程先写入了
        }
        return true;
    }
}
```

### 3. 自动装配

**pom.xml 改动**：
- 新增 `spring-data-redis` 为 `<optional>true</optional>`

**装配策略**（通过嵌套 `@Configuration` 类实现）：

```
@ConditionalOnClass(StringRedisTemplate) + @ConditionalOnMissingBean(NonceRepository)
  → RedisNonceRepository（优先）

@ConditionalOnMissingBean(NonceRepository)（兜底）
  → InMemoryNonceRepository
```

**CartesianOpenapiAutoConfiguration 改动**：
- 构造函数移除 `NonceRepository` 参数
- 新增两个嵌套 `@Configuration` 类，分别注册 `RedisNonceRepository` 和 `InMemoryNonceRepository`
- `SignatureVerificationFilter` bean 方法改为方法参数注入 `NonceRepository`

### 4. 文件清单

| 操作 | 文件 |
|---|---|
| 新增 | `nonce/RedisNonceRepository.java` |
| 新增 | `nonce/InMemoryNonceRepository.java` |
| 修改 | `config/CartisanOpenapiAutoConfiguration.java` |
| 修改 | `pom.xml`（加 optional redis 依赖） |
| 新增 | `nonce/RedisNonceRepositoryTest.java` |
| 新增 | `nonce/InMemoryNonceRepositoryTest.java` |

### 5. 测试要求

- `RedisNonceRepositoryTest`：用 mock 的 `StringRedisTemplate` 测试首次获取、重复获取、过期场景
- `InMemoryNonceRepositoryTest`：测试首次获取、重复获取、过期后重新获取、并发安全性
