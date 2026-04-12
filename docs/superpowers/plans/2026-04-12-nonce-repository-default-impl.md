# NonceRepository 默认实现 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 NonceRepository 提供 Redis 和 InMemory 两个默认实现，业务方引入 cartisan-openapi 后自动生效。

**Architecture:** 在 cartisan-openapi 模块内新增两个 NonceRepository 实现，通过 Spring Boot 条件装配自动选择：classpath 有 Redis → RedisNonceRepository，否则 → InMemoryNonceRepository。用户自定义实现优先。

**Tech Stack:** Java 21, Spring Data Redis (optional), ConcurrentHashMap

**Spec:** `docs/superpowers/specs/2026-04-12-nonce-repository-default-impl-design.md`

---

### Task 1: 添加 spring-data-redis optional 依赖

**Files:**
- Modify: `cartisan-openapi/pom.xml`

- [ ] **Step 1: 在 pom.xml 的 dependencies 中添加 spring-data-redis**

在 `</dependencies>` 前新增：

```xml
<!-- Spring Data Redis (optional, for RedisNonceRepository) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <optional>true</optional>
</dependency>
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl cartisan-openapi`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add cartisan-openapi/pom.xml
git commit -m "build: add spring-data-redis as optional dependency for NonceRepository"
```

---

### Task 2: InMemoryNonceRepository (TDD)

**Files:**
- Create: `cartisan-openapi/src/test/java/com/cartisan/openapi/nonce/InMemoryNonceRepositoryTest.java`
- Create: `cartisan-openapi/src/main/java/com/cartisan/openapi/nonce/InMemoryNonceRepository.java`

- [ ] **Step 1: 写失败测试**

测试类：`InMemoryNonceRepositoryTest`
测试用例（参考项目测试风格，用 AssertJ）：
- `shouldReturnTrue_whenFirstAcquire` — 首次获取 nonce 成功
- `shouldReturnFalse_whenDuplicateAcquire` — 重复获取同一 nonce 返回 false
- `shouldReturnTrue_whenExpiredNonceReacquired` — 过期后重新获取成功（用极短 TTL 如 10ms + 短暂等待）

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl cartisan-openapi -Dtest=InMemoryNonceRepositoryTest`
Expected: FAIL (class not found)

- [ ] **Step 3: 实现 InMemoryNonceRepository**

要点：
- `ConcurrentHashMap<String, Long>` 存储，value 为过期时间戳（毫秒）
- `tryAcquire` 逻辑：先检查是否存在且未过期 → 不存在或已过期则 `putIfAbsent` → 再次检查并发竞争
- 不做定时清理，懒淘汰即可

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -pl cartisan-openapi -Dtest=InMemoryNonceRepositoryTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-openapi/src/main/java/com/cartisan/openapi/nonce/InMemoryNonceRepository.java cartisan-openapi/src/test/java/com/cartisan/openapi/nonce/InMemoryNonceRepositoryTest.java
git commit -m "feat: add InMemoryNonceRepository as fallback default"
```

---

### Task 3: RedisNonceRepository (TDD)

**Files:**
- Create: `cartisan-openapi/src/test/java/com/cartisan/openapi/nonce/RedisNonceRepositoryTest.java`
- Create: `cartisan-openapi/src/main/java/com/cartisan/openapi/nonce/RedisNonceRepository.java`

- [ ] **Step 1: 写失败测试**

测试类：`RedisNonceRepositoryTest`（mock `StringRedisTemplate`，不依赖真实 Redis）
测试用例：
- `shouldReturnTrue_whenKeyNotExists` — mock `setIfAbsent` 返回 true
- `shouldReturnFalse_whenKeyAlreadyExists` — mock `setIfAbsent` 返回 false
- `shouldReturnFalse_whenSetIfAbsentReturnsNull` — mock 返回 null

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl cartisan-openapi -Dtest=RedisNonceRepositoryTest`
Expected: FAIL (class not found)

- [ ] **Step 3: 实现 RedisNonceRepository**

要点：
- 构造函数注入 `StringRedisTemplate`
- key 前缀 `openapi:nonce:` + nonce
- `tryAcquire` 调用 `opsForValue().setIfAbsent(key, "1", ttl)`，返回 `Boolean.TRUE.equals(result)`

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn test -pl cartisan-openapi -Dtest=RedisNonceRepositoryTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-openapi/src/main/java/com/cartisan/openapi/nonce/RedisNonceRepository.java cartisan-openapi/src/test/java/com/cartisan/openapi/nonce/RedisNonceRepositoryTest.java
git commit -m "feat: add RedisNonceRepository as production default"
```

---

### Task 4: 自动装配改造

**Files:**
- Modify: `cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiAutoConfiguration.java`

- [ ] **Step 1: 改造 AutoConfiguration**

改动要点：
1. 构造函数移除 `NonceRepository` 参数（不再是必选依赖）
2. 新增嵌套 static `@Configuration` 类 `RedisNonceRepositoryConfiguration`：
   - `@ConditionalOnClass(StringRedisTemplate.class)`
   - `@ConditionalOnMissingBean(NonceRepository.class)`
   - 提供 `redisNonceRepository` Bean（注入 `StringRedisTemplate`）
3. 新增嵌套 static `@Configuration` 类 `InMemoryNonceRepositoryConfiguration`：
   - `@ConditionalOnMissingBean(NonceRepository.class)`
   - 提供 `inMemoryNonceRepository` Bean
4. `signatureVerificationFilter` 方法改为通过参数注入 `NonceRepository`（Spring 会自动装配默认实现）
5. 删除类字段 `private final NonceRepository nonceRepository`

- [ ] **Step 2: 全量编译 + 测试**

Run: `mvn test -pl cartisan-openapi`
Expected: ALL TESTS PASS

- [ ] **Step 3: Commit**

```bash
git add cartisan-openapi/src/main/java/com/cartisan/openapi/config/CartisanOpenapiAutoConfiguration.java
git commit -m "feat: auto-configure Redis or InMemory NonceRepository"
```

---

### Task 5: 全量验证

- [ ] **Step 1: 全模块编译**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 全量测试**

Run: `mvn test`
Expected: ALL TESTS PASS
