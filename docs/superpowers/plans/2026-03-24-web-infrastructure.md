# Web 基础设施增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 cartisan-core 和 cartisan-web 模块添加 7 个基础设施功能，提升开发效率

**Architecture:**
- cartisan-core: 添加 RedisKey 工具类
- cartisan-web: 添加防重提交、Jackson 配置、DomainMapper 增强、TreeNode、请求日志、MDC 集成
- 所有组件通过 AutoConfiguration 自动装配
- 遵循现有测试风格（shouldX 命名）

**Tech Stack:**
- Spring AOP（AspectJ）
- Jackson ObjectMapper
- Redis（StringRedisTemplate）
- SLF4J MDC
- JUnit 5 + AssertJ

---

## 文件结构

```
cartisan-core/
└── src/main/java/com/cartisan/core/util/
    └── RedisKey.java                    # Redis Key 管理工具

cartisan-web/
├── src/main/java/com/cartisan/web/
│   ├── config/
│   │   └── JacksonConfiguration.java    # Jackson 全局配置
│   ├── mapper/
│   │   └── DomainMapper.java            # 添加默认方法
│   ├── resubmit/
│   │   ├── PreventResubmit.java         # 防重提交注解
│   │   ├── ResubmitAspect.java          # 切面
│   │   ├── ResubmitLock.java            # 锁管理器
│   │   └── package-info.java
│   ├── support/
│   │   ├── TreeNode.java                # 树节点
│   │   ├── TreeNodeBuilder.java         # 树构建器
│   │   └── package-info.java
│   ├── filter/
│   │   ├── RequestLogFilter.java        # 请求日志 Filter
│   │   └── package-info.java
│   ├── context/
│   │   ├── RequestContextFilter.java    # 扩展 MDC 支持
│   │   └── RequestContext.java          # 添加 MDC 相关方法
│   └── config/
│       └── CartisanWebAutoConfiguration.java  # 注册新组件
│
└── src/test/java/com/cartisan/web/
    ├── config/
    │   └── JacksonConfigurationTest.java
    ├── resubmit/
    │   └── PreventResubmitIntegrationTest.java
    ├── support/
    │   └── TreeNodeBuilderTest.java
    └── filter/
        └── RequestLogFilterTest.java
```

---

## Task 1: 实现 RedisKey 工具（cartisan-core）

**Files:**
- Create: `cartisan-core/src/main/java/com/cartisan/core/util/RedisKey.java`
- Test: `cartisan-core/src/test/java/com/cartisan/core/util/RedisKeyTest.java`

- [ ] **Step 1: 编写 RedisKey 测试**

测试用例：
- shouldCreateKeyWithExpiration
- shouldCreatePermanentKey
- shouldGenerateFullKeyWithSuffix
- shouldReturnCorrectExpireSeconds
- shouldIdentifyPermanentKey

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-core:test --tests RedisKeyTest
```

Expected: FAIL with "Class not found"

- [ ] **Step 3: 实现 RedisKey 类**

实现：
- final 类，不可变
- 私有构造函数
- of(prefix, expireSeconds) 静态工厂方法
- permanent(prefix) 静态工厂方法
- key(suffix) 实例方法
- expireSeconds() getter
- isPermanent() 判断方法

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-core:test --tests RedisKeyTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-core/src/main/java/com/cartisan/core/util/RedisKey.java
git add cartisan-core/src/test/java/com/cartisan/core/util/RedisKeyTest.java
git commit -m "feat(core): add RedisKey utility for Redis key management"
```

---

## Task 2: 扩展 DomainMapper 默认方法

**Files:**
- Modify: `cartisan-web/src/main/java/com/cartisan/web/mapper/DomainMapper.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/mapper/DomainMapperTest.java`

- [ ] **Step 1: 编写默认方法测试**

测试用例：
- shouldConvertListUsingDefaultMethod
- shouldConvertSetUsingDefaultMethod
- shouldHandleNullList
- shouldHandleEmptyList

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests DomainMapperTest
```

Expected: FAIL with "Method not found"

- [ ] **Step 3: 添加默认方法到 DomainMapper**

在 DomainMapper 接口添加：
```java
default List<T> convertList(List<S> sources) { ... }
default Set<T> convertSet(Set<S> sources) { ... }
```

注意：需要 MapStruct 生成 convert 方法，默认方法调用它。

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests DomainMapperTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/mapper/DomainMapper.java
git add cartisan-web/src/test/java/com/cartisan/web/mapper/DomainMapperTest.java
git commit -m "feat(web): add default methods to DomainMapper for batch conversion"
```

---

## Task 3: 实现 Jackson 全局配置

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/config/JacksonConfiguration.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/config/JacksonConfigurationTest.java`

- [ ] **Step 1: 编写配置测试**

测试用例：
- shouldSerializeLongAsString
- shouldSerializeLocalDateTimeAsIso8601
- shouldSerializeBigDecimalWithoutScientificNotation
- shouldSerializeEnumAsString
- shouldIgnoreUnknownProperties

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests JacksonConfigurationTest
```

Expected: FAIL

- [ ] **Step 3: 实现 JacksonConfiguration**

创建配置类：
- @Configuration
- @Bean Jackson2ObjectMapperBuilderCustomizer
- 配置项：Long→字符串、LocalDateTime→ISO 8601、BigDecimal→禁止科学计数法、Enum→字符串、忽略未知属性

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests JacksonConfigurationTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/JacksonConfiguration.java
git add cartisan-web/src/test/java/com/cartisan/web/config/JacksonConfigurationTest.java
git commit -m "feat(web): add Jackson global configuration"
```

---

## Task 4: 实现 TreeNode 和 TreeNodeBuilder

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/support/TreeNode.java`
- Create: `cartisan-web/src/main/java/com/cartisan/web/support/TreeNodeBuilder.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/support/TreeNodeBuilderTest.java`

- [ ] **Step 1: 编写 TreeNodeBuilder 测试**

测试用例：
- shouldBuildTreeFromFlatList
- shouldHandleMultipleLevelNesting
- shouldHandleEmptyList
- shouldHandleOrphanNodes

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests TreeNodeBuilderTest
```

Expected: FAIL

- [ ] **Step 3: 实现 TreeNode 泛型类**

实现泛型 TreeNode<T>：
- id, name, parentId 字段（泛型 id/parentId）
- children 列表
- 构造函数、getter/setter

- [ ] **Step 4: 实现 TreeNodeBuilder**

使用 Stream API（非 Guava）：
- build(List<TreeNode<T>> nodes, Function<T, String> idMapper, Function<T, String> parentIdMapper, T rootParentId)

- [ ] **Step 5: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests TreeNodeBuilderTest
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/support/
git add cartisan-web/src/test/java/com/cartisan/web/support/
git commit -m "feat(web): add TreeNode and TreeNodeBuilder for tree structure"
```

---

## Task 5: 实现 @PreventResubmit 注解

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/resubmit/PreventResubmit.java`
- Create: `cartisan-web/src/main/java/com/cartisan/web/resubmit/package-info.java`

- [ ] **Step 1: 创建 @PreventResubmit 注解**

创建注解：
- @Target(ElementType.METHOD)
- @Retention(RetentionPolicy.RUNTIME)
- 属性：delaySeconds (int, default 20), prefix (String, default "")

- [ ] **Step 2: 验证编译**

```bash
./gradlew :cartisan-web:compileJava
```

Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/resubmit/
git commit -m "feat(web): add @PreventResubmit annotation"
```

---

## Task 6: 实现 ResubmitLock（Redis 版本）

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/resubmit/ResubmitLock.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/resubmit/ResubmitLockTest.java`

- [ ] **Step 1: 编写 ResubmitLock 测试**

测试用例：
- shouldLockSuccessfully
- shouldReturnFalseWhenKeyExists
- shouldGenerateKeyFromArgs

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests ResubmitLockTest
```

Expected: FAIL

- [ ] **Step 3: 实现 ResubmitLock**

实现：
- 依赖 StringRedisTemplate
- generateKey(prefix, argsHash) 方法
- lock(key, delaySeconds) 方法（使用 setIfAbsent）
- 不需要 unlock（Redis 自动过期）

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests ResubmitLockTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/resubmit/ResubmitLock.java
git add cartisan-web/src/test/java/com/cartisan/web/resubmit/ResubmitLockTest.java
git commit -m "feat(web): add ResubmitLock with Redis backend"
```

---

## Task 7: 实现 ResubmitAspect 切面

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/resubmit/ResubmitAspect.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/resubmit/PreventResubmitIntegrationTest.java`

- [ ] **Step 1: 编写集成测试**

测试用例：
- shouldAllowFirstRequest
- shouldBlockDuplicateRequest
- shouldAllowAfterDelay

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests PreventResubmitIntegrationTest
```

Expected: FAIL

- [ ] **Step 3: 实现 ResubmitAspect**

实现：
- @Aspect @Component
- @Around("@annotation(PreventResubmit)")
- 读取注解参数
- 序列化请求参数生成 key
- 调用 ResubmitLock.lock()
- 失败时抛出异常

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests PreventResubmitIntegrationTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/resubmit/ResubmitAspect.java
git add cartisan-web/src/test/java/com/cartisan/web/resubmit/PreventResubmitIntegrationTest.java
git commit -m "feat(web): add ResubmitAspect for @PreventResubmit"
```

---

## Task 8: 实现 RequestLogFilter

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/filter/RequestLogFilter.java`
- Create: `cartisan-web/src/main/java/com/cartisan/web/filter/package-info.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/filter/RequestLogFilterTest.java`

- [ ] **Step 1: 编写 RequestLogFilter 测试**

测试用例：
- shouldLogRequestWithBasicInfo
- shouldLogPostRequestBody
- shouldExcludeSwaggerPaths
- shouldExcludeDruidPaths

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests RequestLogFilterTest
```

Expected: FAIL

- [ ] **Step 3: 实现 RequestLogFilter**

实现：
- 继承 OncePerRequestFilter
- INFO 级别日志
- 记录：requestId、IP、方法、URI、Body（POST/PUT）、参数
- 排除：swagger、druid、actuator
- 使用 ContentCachingRequestWrapper 缓存 Body

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests RequestLogFilterTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/filter/
git add cartisan-web/src/test/java/com/cartisan/web/filter/
git commit -m "feat(web): add RequestLogFilter for request logging"
```

---

## Task 9: 扩展 RequestContextFilter 集成 MDC

**Files:**
- Modify: `cartisan-web/src/main/java/com/cartisan/web/context/RequestContextFilter.java`
- Modify: `cartisan-web/src/main/java/com/cartisan/web/context/RequestContext.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/context/RequestContextFilterTest.java`

- [ ] **Step 1: 编写 MDC 集成测试**

测试用例：
- shouldPutRequestIdToMDC
- shouldClearMDCAfterRequest
- shouldAddRequestIdToResponseHeader

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests RequestContextFilterTest
```

Expected: FAIL

- [ ] **Step 3: 修改 RequestContextFilter**

添加：
- doFilterInternal 开始时：MDC.put("requestId", requestId)
- finally 块：MDC.clear()
- 响应头添加：response.setHeader("X-Request-Id", requestId)

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests RequestContextFilterTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/context/RequestContextFilter.java
git add cartisan-web/src/main/java/com/cartisan/web/context/RequestContext.java
git add cartisan-web/src/test/java/com/cartisan/web/context/RequestContextFilterTest.java
git commit -m "feat(web): integrate MDC with RequestContext"
```

---

## Task 10: 更新 CartisanWebAutoConfiguration

**Files:**
- Modify: `cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java`

- [ ] **Step 1: 更新自动配置**

注册新组件：
- JacksonConfiguration（已有 @Configuration，自动扫描）
- ResubmitAspect（已有 @Component，自动扫描）
- RequestLogFilter（需要注册为 Bean）

- [ ] **Step 2: 验证编译**

```bash
./gradlew :cartisan-web:compileJava
```

Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java
git commit -m "feat(web): update AutoConfiguration for new components"
```

---

## Task 11: 编写使用文档

**Files:**
- Create: `docs/guide/web-infrastructure.md`

- [ ] **Step 1: 编写文档**

文档内容：
- RedisKey 使用示例
- DomainMapper 默认方法说明
- TreeNode 使用示例
- @PreventResubmit 使用示例
- Jackson 配置说明
- RequestLogFilter 说明
- MDC 配置说明

- [ ] **Step 2: Commit**

```bash
git add docs/guide/web-infrastructure.md
git commit -m "docs: add web infrastructure usage guide"
```

---

## Task 12: 最终验证

- [ ] **Step 1: 运行 cartisan-core 全量测试**

```bash
./gradlew :cartisan-core:test
```

Expected: 全部 PASS

- [ ] **Step 2: 运行 cartisan-web 全量测试**

```bash
./gradlew :cartisan-web:test
```

Expected: 全部 PASS

- [ ] **Step 3: 最终 Commit**

如有调整，提交最终修改。

---

## 参考资料

- 旧框架实现：`~/workspace/cartisan-by-spring-boot-2/cartisan-web/`
- 设计文档：`docs/superpowers/specs/2026-03-24-framework-enhancement-design.md`
