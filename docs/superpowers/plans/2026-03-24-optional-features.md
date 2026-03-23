# 可选功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现自动响应包装和用户踢出功能

**Architecture:**
- F00-10: ResponseBodyAdvice 统一包装返回值为 ApiResponse
- F00-11: 扩展 AuthenticationService 添加踢出方法
- 两个功能独立，可分别启用/禁用

**Tech Stack:**
- Spring MVC ResponseBodyAdvice
- Sa-Token StpUtil
- JUnit 5 + AssertJ

---

## 文件结构

```
cartisan-web/
├── src/main/java/com/cartisan/web/response/
│   ├── AutoResponseAdvice.java          # 响应自动包装
│   └── AutoResponseConfiguration.java   # 配置类（控制开关）
│
└── src/test/java/com/cartisan/web/response/
    └── AutoResponseAdviceTest.java

cartisan-security/
├── src/main/java/com/cartisan/security/authentication/
│   ├── AuthenticationService.java        # 添加踢出方法
│   └── SaTokenAuthenticationService.java # 实现踢出方法
│
└── src/test/java/com/cartisan/security/authentication/
    └── KickoutIntegrationTest.java
```

---

## F00-10: 自动响应包装

### Task 1: 实现 AutoResponseConfiguration 配置类

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseConfiguration.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseConfigurationTest.java`

- [ ] **Step 1: 编写配置测试**

测试用例：
- shouldBeDisabledByDefault
- shouldBeEnabledWhenPropertySet

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests AutoResponseConfigurationTest
```

Expected: FAIL

- [ ] **Step 3: 实现 AutoResponseConfiguration**

创建配置类：
- @ConfigurationProperties("cartisan.web.auto-response")
- 属性：enabled (boolean, default false)
- @Bean 条件装配：当 enabled=true 时才注册 ResponseBodyAdvice

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests AutoResponseConfigurationTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseConfiguration.java
git add cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseConfigurationTest.java
git commit -m "feat(web): add AutoResponseConfiguration with enable/disable toggle"
```

---

### Task 2: 实现 AutoResponseAdvice

**Files:**
- Create: `cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseAdvice.java`
- Test: `cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseAdviceTest.java`

- [ ] **Step 1: 编写 Advice 测试**

测试用例：
- shouldWrapStringResponse
- shouldWrapObjectResponse
- shouldNotWrapApiResponse
- shouldExcludeSwaggerPaths
- shouldHandleNullResponse

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-web:test --tests AutoResponseAdviceTest
```

Expected: FAIL

- [ ] **Step 3: 实现 AutoResponseAdvice**

实现：
- @RestControllerAdvice
- implements ResponseBodyAdvice<Object>
- supports(): 返回配置的 enabled 值
- beforeBodyWrite(): 包装为 ApiResponse.ok()
- 特殊处理 String 类型（需要序列化）
- 排除 ApiResponse 类型（避免重复包装）
- 排除 swagger 等路径

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-web:test --tests AutoResponseAdviceTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/response/AutoResponseAdvice.java
git add cartisan-web/src/test/java/com/cartisan/web/response/AutoResponseAdviceTest.java
git commit -m "feat(web): add AutoResponseAdvice for automatic response wrapping"
```

---

### Task 3: 更新 CartisanWebAutoConfiguration

**Files:**
- Modify: `cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java`

- [ ] **Step 1: 注册 AutoResponseConfiguration**

添加 @Import(AutoResponseConfiguration.class) 或通过 @EnableConfigurationProperties 注册。

- [ ] **Step 2: 验证编译**

```bash
./gradlew :cartisan-web:compileJava
```

Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add cartisan-web/src/main/java/com/cartisan/web/config/CartisanWebAutoConfiguration.java
git commit -m "feat(web): register AutoResponseConfiguration"
```

---

## F00-11: 用户踢出包装

### Task 4: 扩展 AuthenticationService 接口

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java`

- [ ] **Step 1: 添加踢出方法签名**

在接口中添加默认方法：
```java
default void kickout(Long loginId) { ... }
default void kickoutByUsername(String username) { ... }
```

- [ ] **Step 2: 验证编译**

```bash
./gradlew :cartisan-security:compileJava
```

Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java
git commit -m "feat(security): add kickout methods to AuthenticationService interface"
```

---

### Task 5: 实现 SaTokenAuthenticationService 踢出方法

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/authentication/SaTokenAuthenticationService.java`
- Test: `cartisan-security/src/test/java/com/cartisan/security/authentication/KickoutIntegrationTest.java`

- [ ] **Step 1: 编写踢出测试**

测试用例：
- shouldKickoutUserByLoginId
- shouldKickoutUserByUsername
- shouldHandleNonExistentUser

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-security:test --tests KickoutIntegrationTest
```

Expected: FAIL

- [ ] **Step 3: 实现踢出方法**

在 SaTokenAuthenticationService 中：
- kickout(Long loginId): 调用 StpUtil.kickout(loginId)
- kickoutByUsername(String username): 需要业务层提供 username → loginId 映射，默认抛出 UnsupportedOperationException

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-security:test --tests KickoutIntegrationTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/authentication/SaTokenAuthenticationService.java
git add cartisan-security/src/test/java/com/cartisan/security/authentication/KickoutIntegrationTest.java
git commit -m "feat(security): implement kickout methods in SaTokenAuthenticationService"
```

---

## Task 6: 编写使用文档

**Files:**
- Create: `docs/guide/optional-features.md`

- [ ] **Step 1: 编写文档**

文档内容：
- F00-10: 自动响应包装说明
  - 配置方式（cartisan.web.auto-response.enabled）
  - 注意事项（统一风格、避免混用）
  - 如何禁用
- F00-11: 用户踢出说明
  - kickout(loginId) 使用示例
  - kickoutByUsername 实现要求

- [ ] **Step 2: Commit**

```bash
git add docs/guide/optional-features.md
git commit -m "docs: add optional features usage guide"
```

---

## Task 7: 最终验证

- [ ] **Step 1: 运行 cartisan-web 全量测试**

```bash
./gradlew :cartisan-web:test
```

Expected: 全部 PASS

- [ ] **Step 2: 运行 cartisan-security 全量测试**

```bash
./gradlew :cartisan-security:test
```

Expected: 全部 PASS

- [ ] **Step 3: 最终 Commit**

如有调整，提交最终修改。

---

## 注意事项

### F00-10 自动响应包装风险

1. **统一风格**：启用后所有响应都会被包装，确保全局统一
2. **String 类型处理**：需要特殊序列化处理
3. **排除路径**：swagger、actuator 等路径需要排除
4. **默认禁用**：配置项默认为 false，避免意外影响现有项目

### F00-11 踢出功能说明

1. **kickout(loginId)**：直接调用 Sa-Token，无需额外实现
2. **kickoutByUsername)**：需要业务层提供 username → loginId 映射，框架层默认抛出异常

---

## 参考资料

- 旧框架实现：`~/workspace/cartisan-by-spring-boot-2/cartisan-web/src/main/java/com/cartisan/response/GlobalResponseBodyAdvice.java`
- 设计文档：`docs/superpowers/specs/2026-03-24-framework-enhancement-design.md`
- Sa-Token 文档：https://sa-token.cc/doc.html#/use/kickout
