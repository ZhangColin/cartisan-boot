# Feature: F03-09 @CurrentUser 注解 — 测试规格与归档

> **状态**: ✅ 完成
> **完成日期**: 2026-03-18
> **审查结果**: 通过

---

## 测试策略

### 测试分层

| 测试类型 | 工具 | 覆盖内容 |
|---------|------|---------|
| 单元测试 | JUnit 5 + AssertJ + Mockito | Resolver 参数解析逻辑 |
| 配置测试 | JUnit 5 + Spring Test | Config 类 Bean 注册 |
| 集成测试 | MockMvc + Testcontainers | 端到端验证注解、参数注入、异常处理 |

### Mock 策略

使用 `MockedStatic<SecurityContext>` 和 `MockedStatic<StpUtil>`：
- 已登录场景：`SecurityContext.getCurrentUserId()` → 返回 userId
- 未登录场景：`SecurityContext.getCurrentUserId()` → `null`
- `StpUtil.checkLogin()` 异常抛出：mock 抛出 `NotLoginException`

---

## 最终测试用例清单

### 单元测试（7 条，AC1-AC7）

| ID | 测试方法 | 验收标准 | 状态 |
|----|---------|---------|------|
| AC1 | `given_parameterWithCurrentUserAnnotationAndLongType_when_supportsParameter_then_returnTrue` | `@CurrentUser Long` 参数被识别 | ✅ |
| AC2 | `given_parameterWithCurrentUserAnnotationAndOptionalLongType_when_supportsParameter_then_returnTrue` | `@CurrentUser Optional<Long>` 参数被识别 | ✅ |
| AC3 | `given_parameterWithoutCurrentUserAnnotation_when_supportsParameter_then_returnFalse` | 无注解参数不被处理 | ✅ |
| AC4 | `given_parameterWithUnsupportedType_when_supportsParameter_then_returnFalse` | 不支持类型（String）不被处理 | ✅ |
| AC5 | `given_optionalLongWithoutAnnotation_when_supportsParameter_then_returnFalse` | Optional<Long> 无注解不被处理 | ✅ |
| AC6 | `given_authenticatedUser_when_resolveLong_then_returnUserId` | 已登录时正确注入 userId | ✅ |
| AC7 | `given_unauthenticatedUser_when_resolveLong_then_throwNotLoginException` | 未登录 + Long 抛 NotLoginException | ✅ |
| AC8 | `given_authenticatedUser_when_resolveOptional_then_returnPresent` | 已登录时返回 Optional.of(userId) | ✅ |
| AC9 | `given_unauthenticatedUser_when_resolveOptional_then_returnEmpty` | 未登录时返回 Optional.empty() | ✅ |

### 配置测试（2 条，AC10）

| ID | 测试方法 | 验收标准 | 状态 |
|----|---------|---------|------|
| AC10 | `given_contextLoaded_when_resolverRegistered_then_success` | Resolver 被 MVC 容器正确注册 | ✅ |
| AC11 | `given_newInstance_when_getResolver_then_returnsInjected` | 配置类正确构造 | ✅ |

### 集成测试（6 条，AC12-AC17）

| ID | 测试方法 | 验收标准 | 状态 |
|----|---------|---------|------|
| AC12 | `given_authenticatedUser_when_getCurrentUserId_then_200` | 已登录访问 @CurrentUser Long 返回 200 | ✅ |
| AC13 | `given_unauthenticatedUser_when_getCurrentUserId_then_401` | 未登录访问 @CurrentUser Long 返回 401 | ✅ |
| AC14 | `given_authenticatedUser_when_getCurrentUserIdOptional_then_isPresentTrue` | 已登录访问 Optional<Long> 返回 isPresent=true | ✅ |
| AC15 | `given_unauthenticatedUser_when_getCurrentUserIdOptional_then_isPresentFalse` | 未登录访问 Optional<Long> 返回 isPresent=false | ✅ |
| AC16 | `given_authenticatedUser_when_getCurrentUserIdWithAuth_then_200` | @RequireAuth + @CurrentUser 组合正常工作 | ✅ |
| AC17 | `given_unauthenticatedUser_when_getCurrentUserIdWithAuth_then_401` | @RequireAuth + @CurrentUser 未登录返回 401 | ✅ |

---

## 测试执行结果

### 最终测试统计

```
tests="17" skipped="0" failures="0" errors="0"
```

- **总测试数**: 17
- **通过**: 17
- **失败**: 0
- **错误**: 0
- **跳过**: 0

### 测试覆盖率

| 类/方法 | 覆盖场景 |
|---------|---------|
| `CurrentUserMethodArgumentResolver.supportsParameter()` | 5 个分支全部覆盖 |
| `CurrentUserMethodArgumentResolver.resolveArgument()` | Long 已登录、Long 未登录、Optional 已登录、Optional 未登录 |
| `CurrentUserArgumentResolverConfig.addArgumentResolvers()` | Resolver 注册验证 |
| 端到端场景 | 已登录/未登录 × Long/Optional × 单独注解/与 @RequireAuth 组合 |

---

## 代码审查结果

### 审查日期
2026-03-18

### 审查方式
使用 `superpowers:code-reviewer` 子代理进行交叉审查

### 审查结论
**✅ 通过审查**

### 优点
- **接口契约符合性**: 100% - 所有 AC 完整实现
- **项目规范遵循**: 100% - 遵循 SKILL.md 所有相关规则
- **代码质量**: 优秀 - JavaDoc 完整、命名清晰、结构合理
- **测试覆盖**: 优秀 - 17 个测试用例，覆盖所有场景和边界
- **TDD 执行**: 严格遵循红绿循环，测试先于实现

### 问题讨论
**Issue**: 审查者指出直接调用 `StpUtil.checkLogin()` 可能违反抽象层

**决议**: 这是与 `SecurityInterceptor` 一致的设计（ADR-015），使用 Sa-Token 推荐的抛出异常方式，确保异常包含完整上下文信息。

---

## 交付物清单

| 类型 | 文件 | 路径 | 说明 |
|------|------|------|------|
| **源代码** | `CurrentUser.java` | `cartisan-security/.../annotation/` | 注解定义（~50 行） |
| | `CurrentUserMethodArgumentResolver.java` | `cartisan-security/.../annotation/` | Resolver 实现（~65 行） |
| | `CurrentUserArgumentResolverConfig.java` | `cartisan-security/.../config/` | 配置类（~50 行） |
| | `CartisanSecurityAutoConfiguration.java` | `cartisan-security/.../config/` | 修改：添加 @Import |
| **测试** | `CurrentUserMethodArgumentResolverTest.java` | `cartisan-security/.../annotation/` | 单元测试（~200 行） |
| | `CurrentUserArgumentResolverConfigTest.java` | `cartisan-security/.../config/` | 配置测试（~50 行） |
| | `CurrentUserIntegrationTest.java` | `cartisan-security/.../integration/` | 集成测试（~140 行） |
| | `TestAuthController.java` | `cartisan-security/.../controller/` | 修改：添加 3 个测试端点 |
| | `CartisanSecurityAutoConfigurationTest.java` | `cartisan-security/.../config/` | 修改：添加 2 个测试方法 |
| **文档** | `01_requirement.md` | `docs/specs/.../F03-09-CurrentUser注解/` | 需求规格 |
| | `02_interface.md` | `docs/specs/.../F03-09-CurrentUser注解/` | 接口契约 |
| | `03_implementation.md` | `docs/specs/.../F03-09-CurrentUser注解/` | 实施计划 |
| | `04_test_spec.md` | `docs/specs/.../F03-09-CurrentUser注解/` | 本文档 |
| | `DECISIONS.md` | `docs/decisions/` | 添加 ADR-015 |

---

## SKILL.md 更新

本次开发未发现新的踩坑经验或需要补充的规则，SKILL.md 无需更新。

---

## 使用示例

### 必需登录场景

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/profile")
    public ApiResponse<UserProfile> getProfile(@CurrentUser Long userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }
}
```

### 可选登录场景

```java
@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

    @GetMapping
    public ApiResponse<Preferences> getPreferences(@CurrentUser Optional<Long> userId) {
        if (userId.isPresent()) {
            return ApiResponse.ok(preferencesService.getForUser(userId.get()));
        }
        return ApiResponse.ok(preferencesService.getDefault());
    }
}
```

### 与 @RequireAuth 组合

```java
@GetMapping("/profile")
@RequireAuth  // 拦截器阶段检查，语义更明确
public ApiResponse<UserProfile> getProfile(@CurrentUser Long userId) {
    return ApiResponse.ok(userService.getProfile(userId));
}
```

---

## 参考文档

- Epic Backlog: [00_epic_backlog.md](../00_epic_backlog.md)
- AI 协作 SOP: [AI协作开发SOP.md](../../../sop/AI协作开发SOP.md)
- SKILL.md: [SKILL.md](../../../skills/SKILL.md)
- 架构决策: [DECISIONS.md](../../../decisions/DECISIONS.md) (ADR-015)
