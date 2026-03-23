# cartisan-security 增强功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 为 cartisan-security 模块添加自定义登录超时和获取当前用户 ID 的便捷方法，并为框架添加 Lombok 支持。

**架构:** 在 AuthenticationService 接口添加重载方法和 default 方法，由 SaTokenAuthenticationService 实现。Lombok 通过 BOM 统一版本管理，各模块使用 compileOnly 引入。

**技术栈:** Java 21, Sa-Token 1.45.0, Lombok 1.18.34, Spring Boot 3.4.0, JUnit 5, AssertJ, Mockito

---

## 文件结构

### 新增/修改的文件

| 文件 | 职责 |
|-----|------|
| `cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java` | 添加 login(Long, long) 方法和 getCurrentUserId() default 方法 |
| `cartisan-security/src/main/java/com/cartisan/security/authentication/SaTokenAuthenticationService.java` | 实现 login(Long, long) 方法 |
| `cartisan-security/src/test/java/com/cartisan/security/authentication/SaTokenAuthenticationServiceTest.java` | 添加自定义超时登录的单元测试 |
| `cartisan-security/src/test/java/com/cartisan/security/authentication/AuthenticationServiceTest.java` | 新增文件，测试 getCurrentUserId() default 方法 |
| `cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestAuthController.java` | 添加自定义超时登录的测试端点 |
| `cartisan-security/src/test/java/com/cartisan/security/integration/AuthenticationServiceIntegrationTest.java` | 添加自定义超时登录的集成测试 |
| `cartisan-dependencies/build.gradle.kts` | 添加 Lombok BOM 版本 |
| `cartisan-core/build.gradle.kts` | 添加 Lombok 依赖 |
| `cartisan-web/build.gradle.kts` | 添加 Lombok 依赖 |
| `cartisan-security/build.gradle.kts` | 添加 Lombok 依赖 |
| `cartisan-data-jpa/build.gradle.kts` | 添加 Lombok 依赖 |
| `cartisan-data-query/build.gradle.kts` | 添加 Lombok 依赖 |
| `cartisan-event/build.gradle.kts` | 添加 Lombok 依赖 |
| `cartisan-ai/build.gradle.kts` | 添加 Lombok 依赖 |
| `cartisan-test/build.gradle.kts` | 添加 Lombok 依赖 |

---

## Task 1: 添加自定义超时登录方法

### Task 1.1: 编写 login(Long, long) 的单元测试

**文件:**
- Modify: `cartisan-security/src/test/java/com/cartisan/security/authentication/SaTokenAuthenticationServiceTest.java`

- [ ] **Step 1: 在 LoginTests 内部类中添加测试用例**

在 `SaTokenAuthenticationServiceTest.java` 的 `LoginTests` 类末尾（第 85 行后）添加：

```java
@Nested
@DisplayName("login with custom timeout")
class LoginWithTimeoutTests {

    @Test
    @DisplayName("login(loginId, timeout) 创建会话并返回 TokenInfo")
    void given_validLoginIdAndTimeout_when_loginWithTimeout_then_returnTokenInfo() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // Given
            Long loginId = 123L;
            String token = "test-token-custom-timeout";
            long timeoutSeconds = 86400L; // 1天

            // Mock StpUtil
            stpUtilMock.when(() -> StpUtil.login(loginId, timeoutSeconds)).then(invocation -> null);
            stpUtilMock.when(StpUtil::getTokenValue).thenReturn(token);
            stpUtilMock.when(StpUtil::getTokenTimeout).thenReturn(timeoutSeconds);

            // When
            TokenInfo result = authService.login(loginId, timeoutSeconds);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo(token);
            assertThat(result.loginId()).isEqualTo(loginId);
            assertThat(result.expireTime()).isAfter(Instant.now());
            assertThat(result.expireTime()).isBefore(Instant.now().plusSeconds(timeoutSeconds + 10));

            // Verify login was called with timeout
            stpUtilMock.verify(() -> StpUtil.login(loginId, timeoutSeconds));
        }
    }

    @Test
    @DisplayName("loginId 为 null 时抛出 NullPointerException")
    void given_nullLoginId_when_loginWithTimeout_then_throwNullPointerException() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // When & Then
            assertThatThrownBy(() -> authService.login(null, 3600))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("loginId");
        }
    }

    @Test
    @DisplayName("timeoutSeconds <= 0 时抛出 IllegalArgumentException")
    void given_zeroOrNegativeTimeout_when_loginWithTimeout_then_throwIllegalArgumentException() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // When & Then - zero
            assertThatThrownBy(() -> authService.login(123L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutSeconds must be positive");

            // When & Then - negative
            assertThatThrownBy(() -> authService.login(123L, -100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutSeconds must be positive");
        }
    }

    @Test
    @DisplayName("expireTime 计算正确（1天后）")
    void given_oneDayTimeout_when_loginWithTimeout_then_expireTimeIsCorrect() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            // Given
            Long loginId = 123L;
            String token = "test-token";
            long timeoutSeconds = 86400L; // 1天
            Instant beforeLogin = Instant.now();

            stpUtilMock.when(() -> StpUtil.login(loginId, timeoutSeconds)).then(invocation -> null);
            stpUtilMock.when(StpUtil::getTokenValue).thenReturn(token);

            // When
            TokenInfo result = authService.login(loginId, timeoutSeconds);

            // Then
            Instant afterLogin = Instant.now();
            assertThat(result.expireTime()).isAfter(beforeLogin.plusSeconds(timeoutSeconds - 10));
            assertThat(result.expireTime()).isBefore(afterLogin.plusSeconds(timeoutSeconds + 10));
        }
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-security:test --tests SaTokenAuthenticationServiceTest.LoginWithTimeoutTests
```

预期结果: FAIL - 方法不存在

---

### Task 1.2: 在 AuthenticationService 接口添加方法声明

**文件:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java`

- [ ] **Step 1: 在接口中添加 login(Long, long) 方法声明**

在 `AuthenticationService.java` 的 `login(Long loginId)` 方法后（第 68 行后）添加：

```java
/**
 * 创建登录会话（自定义超时）。
 * <p>
 * 用于"记住我"等场景，如 7 天免登录。
 * </p>
 *
 * @param loginId        用户标识
 * @param timeoutSeconds 超时秒数（> 0）
 * @return Token 信息
 * @throws NullPointerException     loginId 为 null
 * @throws IllegalArgumentException timeoutSeconds <= 0
 */
TokenInfo login(Long loginId, long timeoutSeconds);
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :cartisan-security:compileJava
```

预期结果: 编译失败，实现类缺少方法

---

### Task 1.3: 在 SaTokenAuthenticationService 实现方法

**文件:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/authentication/SaTokenAuthenticationService.java`

- [ ] **Step 1: 添加实现方法**

在 `SaTokenAuthenticationService.java` 的 `login(Long loginId)` 方法后（第 31 行后）添加：

```java
@Override
public TokenInfo login(Long loginId, long timeoutSeconds) {
    Objects.requireNonNull(loginId, "loginId");

    if (timeoutSeconds <= 0) {
        throw new IllegalArgumentException("timeoutSeconds must be positive: " + timeoutSeconds);
    }

    StpUtil.login(loginId, timeoutSeconds);

    String token = StpUtil.getTokenValue();
    Instant expireTime = Instant.now().plusSeconds(timeoutSeconds);

    return new TokenInfo(token, loginId, expireTime);
}
```

- [ ] **Step 2: 运行测试验证通过**

```bash
./gradlew :cartisan-security:test --tests SaTokenAuthenticationServiceTest.LoginWithTimeoutTests
```

预期结果: PASS

- [ ] **Step 3: 运行所有测试确保无破坏**

```bash
./gradlew :cartisan-security:test
```

预期结果: 所有测试 PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java \
        cartisan-security/src/main/java/com/cartisan/security/authentication/SaTokenAuthenticationService.java \
        cartisan-security/src/test/java/com/cartisan/security/authentication/SaTokenAuthenticationServiceTest.java
git commit -m "feat(security): add custom timeout login method

- Add login(Long loginId, long timeoutSeconds) to AuthenticationService
- Implement in SaTokenAuthenticationService with validation
- Add comprehensive unit tests

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

### Task 1.4: 添加集成测试

**文件:**
- Modify: `cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestAuthController.java`
- Modify: `cartisan-security/src/test/java/com/cartisan/security/integration/AuthenticationServiceIntegrationTest.java`

- [ ] **Step 1: 在 TestAuthController 添加测试端点**

在 `TestAuthController.java` 的 `login` 方法后（第 42 行后）添加：

```java
/**
 * 测试登录并设置超时的端点。
 */
@GetMapping("/login/{userId}/timeout/{timeoutSeconds}")
public ApiResponse<Map<String, String>> loginWithTimeout(@PathVariable Long userId, @PathVariable Long timeoutSeconds) {
    StpUtil.login(userId, timeoutSeconds);
    String token = StpUtil.getTokenValue();
    Map<String, String> result = new HashMap<>();
    result.put("token", token);
    return ApiResponse.ok(result);
}
```

- [ ] **Step 2: 在集成测试中添加测试用例**

在 `AuthenticationServiceIntegrationTest.java` 末尾添加：

```java
@Test
@DisplayName("自定义超时登录成功返回有效 token")
void given_loginIdAndTimeout_when_loginWithTimeout_then_returnToken() throws Exception {
    // When: 调用自定义超时登录端点（7天）
    // Then: 返回有效 token
    mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/888/timeout/604800"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").exists())
            .andExpect(jsonPath("$.data.token").isNotEmpty());
}

@Test
@DisplayName("自定义超时登录后可访问需要认证的接口")
void given_loggedInWithCustomTimeout_when_requestWithToken_then_success() throws Exception {
    // Given: 使用自定义超时登录获取 token（1小时）
    String token = extractToken(mvc.perform(MockMvcRequestBuilders.get("/test/auth/login/999/timeout/3600"))
            .andReturn()
            .getResponse()
            .getContentAsString());

    // When: 使用 token 访问受保护接口
    // Then: 请求成功
    mvc.perform(MockMvcRequestBuilders.get("/test/auth/current-user")
                    .header("satoken", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(999));
}
```

- [ ] **Step 3: 运行集成测试**

```bash
./gradlew :cartisan-security:test --tests AuthenticationServiceIntegrationTest
```

预期结果: PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-security/src/test/java/com/cartisan/security/integration/controller/TestAuthController.java \
        cartisan-security/src/test/java/com/cartisan/security/integration/AuthenticationServiceIntegrationTest.java
git commit -m "test(security): add integration tests for custom timeout login

- Add loginWithTimeout test endpoint in TestAuthController
- Verify custom timeout login works in integration context

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: 添加 getCurrentUserId() 便捷方法

### Task 2.1: 创建 AuthenticationService 接口测试文件

**文件:**
- Create: `cartisan-security/src/test/java/com/cartisan/security/authentication/AuthenticationServiceTest.java`

- [ ] **Step 1: 创建接口测试文件**

创建新文件 `AuthenticationServiceTest.java`：

```java
package com.cartisan.security.authentication;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * AuthenticationService 接口 default 方法测试。
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private final AuthenticationService authService = new SaTokenAuthenticationService();

    @Nested
    @DisplayName("getCurrentUserId")
    class GetCurrentUserIdTests {

        @AfterEach
        void tearDown() {
            try {
                StpUtil.logout();
            } catch (Exception e) {
                // ignore if not logged in
            }
        }

        @Test
        @DisplayName("未登录时返回 Optional.empty()")
        void given_notLoggedIn_when_getCurrentUserId_then_returnEmpty() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

                // When
                var result = authService.getCurrentUserId();

                // Then
                assertThat(result).isEmpty();
            }
        }

        @Test
        @DisplayName("已登录时返回 Optional.of(loginId)")
        void given_loggedIn_when_getCurrentUserId_then_returnLoginId() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                // Given
                Long loginId = 789L;
                String token = "test-token-get-current-user";
                long timeoutSeconds = 3600L;

                stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
                stpUtilMock.when(StpUtil::getTokenValue).thenReturn(token);
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(loginId);
                stpUtilMock.when(StpUtil::getTokenTimeout).thenReturn(timeoutSeconds);

                // When
                var result = authService.getCurrentUserId();

                // Then
                assertThat(result).isPresent();
                assertThat(result.get()).isEqualTo(loginId);
            }
        }
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-security:test --tests AuthenticationServiceTest
```

预期结果: FAIL - 方法不存在

---

### Task 2.2: 在 AuthenticationService 接口添加 default 方法

**文件:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java`

- [ ] **Step 1: 在接口末尾添加 getCurrentUserId() default 方法**

在 `AuthenticationService.java` 的 `getTokenInfo()` 方法后（第 83 行后）添加：

```java
/**
 * 获取当前用户 ID。
 *
 * @return 用户 ID，未登录返回 {@link Optional#empty()}
 */
default Optional<Long> getCurrentUserId() {
    return Optional.ofNullable(getTokenInfo()).map(TokenInfo::loginId);
}
```

- [ ] **Step 2: 添加必要的 import**

在文件顶部的 import 区域（第 2 行后）添加：

```java
import java.util.Optional;
```

- [ ] **Step 3: 运行测试验证通过**

```bash
./gradlew :cartisan-security:test --tests AuthenticationServiceTest
```

预期结果: PASS

- [ ] **Step 4: 运行所有测试确保无破坏**

```bash
./gradlew :cartisan-security:test
```

预期结果: 所有测试 PASS

- [ ] **Step 5: 提交**

```bash
git add cartisan-security/src/main/java/com/cartisan/security/authentication/AuthenticationService.java \
        cartisan-security/src/test/java/com/cartisan/security/authentication/AuthenticationServiceTest.java
git commit -m "feat(security): add getCurrentUserId() convenience method

- Add default method to AuthenticationService
- Returns Optional<Long> for null-safe current user access
- Add unit tests for the default method

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: 添加 Lombok 支持

### Task 3.1: 在 cartisan-dependencies BOM 添加 Lombok 版本

**文件:**
- Modify: `cartisan-dependencies/build.gradle.kts`

- [ ] **Step 1: 在 BOM 中添加 Lombok 版本**

在 `cartisan-dependencies/build.gradle.kts` 的 dependencies 块末尾（第 12 行后）添加：

```kotlin
dependencies {
    // Spring Boot BOM - manages all Spring Boot starter versions
    api(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))

    // jOOQ BOM（F04-01）
    api(platform("org.jooq:jooq-bom:3.19.29"))

    // Sa-Token（F03-01）- 直接用 api() 声明带版本约束
    api("cn.dev33:sa-token-spring-boot3-starter:1.45.0")

    // Lombok - 框架内置，减少样板代码
    api("org.projectlombok:lombok:1.18.34")
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :cartisan-dependencies:build
```

预期结果: 编译成功

- [ ] **Step 3: 提交**

```bash
git add cartisan-dependencies/build.gradle.kts
git commit -m "feat(dependencies): add Lombok to BOM

- Add Lombok 1.18.34 to dependency management
- Framework modules can now use Lombok to reduce boilerplate

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

### Task 3.2: 为各模块添加 Lombok 依赖

**文件:**
- Modify: `cartisan-core/build.gradle.kts`
- Modify: `cartisan-web/build.gradle.kts`
- Modify: `cartisan-security/build.gradle.kts`
- Modify: `cartisan-data-jpa/build.gradle.kts`
- Modify: `cartisan-data-query/build.gradle.kts`
- Modify: `cartisan-event/build.gradle.kts`
- Modify: `cartisan-ai/build.gradle.kts`
- Modify: `cartisan-test/build.gradle.kts`

- [ ] **Step 1: 为 cartisan-core 添加 Lombok**

在 `cartisan-core/build.gradle.kts` 的 dependencies 块末尾（第 17 行后）添加：

```kotlin
    // Lombok（编译时生效，不传递给使用者）
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :cartisan-core:compileJava
```

预期结果: 编译成功

- [ ] **Step 3: 为 cartisan-web 添加 Lombok**

在 `cartisan-web/build.gradle.kts` 的 dependencies 块末尾（第 23 行后）添加：

```kotlin
    // Lombok（编译时生效，不传递给使用者）
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
```

- [ ] **Step 4: 为 cartisan-security 添加 Lombok**

在 `cartisan-security/build.gradle.kts` 的 dependencies 块末尾（第 29 行后）添加：

```kotlin
    // Lombok（编译时生效，不传递给使用者）
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
```

- [ ] **Step 5: 为 cartisan-data-jpa 添加 Lombok**

在 `cartisan-data-jpa/build.gradle.kts` 的 dependencies 块中添加（先读取文件确认位置）

- [ ] **Step 6: 为 cartisan-data-query 添加 Lombok**

在 `cartisan-data-query/build.gradle.kts` 的 dependencies 块中添加（先读取文件确认位置）

- [ ] **Step 7: 为 cartisan-event 添加 Lombok**

在 `cartisan-event/build.gradle.kts` 的 dependencies 块中添加（先读取文件确认位置）

- [ ] **Step 8: 为 cartisan-ai 添加 Lombok**

在 `cartisan-ai/build.gradle.kts` 的 dependencies 块中添加（先读取文件确认位置）

- [ ] **Step 9: 为 cartisan-test 添加 Lombok**

在 `cartisan-test/build.gradle.kts` 的 dependencies 块中添加（先读取文件确认位置）

- [ ] **Step 10: 全量编译验证**

```bash
./gradlew compileJava
```

预期结果: 所有模块编译成功

- [ ] **Step 11: 运行所有测试**

```bash
./gradlew test
```

预期结果: 所有测试 PASS（Lombok 不应影响现有测试）

- [ ] **Step 12: 提交**

```bash
git add cartisan-core/build.gradle.kts \
        cartisan-web/build.gradle.kts \
        cartisan-security/build.gradle.kts \
        cartisan-data-jpa/build.gradle.kts \
        cartisan-data-query/build.gradle.kts \
        cartisan-event/build.gradle.kts \
        cartisan-ai/build.gradle.kts \
        cartisan-test/build.gradle.kts
git commit -m "feat(all): add Lombok to all modules

- Add compileOnly and annotationProcessor dependencies
- Use compileOnly to avoid transitive dependency
- Framework code can now use Lombok annotations

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: 更新设计文档状态

**文件:**
- Modify: `docs/superpowers/specs/2026-03-23-security-enhancements-design.md`

- [ ] **Step 1: 更新 spec 状态**

将 spec 文件第 4 行的 `**状态**: 设计中` 改为 `**状态**: 已实现`

- [ ] **Step 2: 提交**

```bash
git add docs/superpowers/specs/2026-03-23-security-enhancements-design.md
git commit -m "docs: mark security enhancements spec as implemented

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## 验证步骤

完成所有任务后，运行以下命令验证：

```bash
# 1. 全量编译
./gradlew compileJava

# 2. 全量测试
./gradlew test

# 3. 检查 git 状态
git status
```

预期结果：
- 所有模块编译成功
- 所有测试通过
- 无未提交的更改

---

## 注意事项

1. **Sa-Token API 确认**: `StpUtil.login(loginId, timeout)` 方法在 Sa-Token 1.45.0 中可用
2. **Java 21 + Lombok**: Lombok 1.18.34 支持 Java 21，但对 record 有一些限制，避免在 record 上使用 Lombok 注解
3. **测试命名**: 遵循 `shouldX` 或 `shouldX_whenY` 风格（用户偏好）
4. **TDD**: 先写测试，再写实现
