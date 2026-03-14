# Feature: F03-02 权限注解 + MVC 拦截器 — 实施计划

## 目标复述

实现 cartisan-security 模块的声明式权限控制能力：
1. 三个权限注解（`@RequireAuth`、`@RequireRole`、`@RequirePermission`）
2. MVC 拦截器（`SecurityInterceptor`）解析注解并调用 Sa-Token 鉴权
3. 异常处理器（`SecurityExceptionHandler`）将 Sa-Token 异常转为 `ApiResponse` 格式
4. 单元测试覆盖率 ≥ 80%

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `com/cartisan/security/annotation/RequireAuth.java` | 登录注解 |
| 新增 | `com/cartisan/security/annotation/RequireRole.java` | 角色注解 |
| 新增 | `com/cartisan/security/annotation/RequirePermission.java` | 权限注解 |
| 新增 | `com/cartisan/security/config/SecurityInterceptor.java` | MVC 拦截器 |
| 新增 | `com/cartisan/security/config/SecurityExceptionHandler.java` | 异常处理器 |
| 新增 | `com/cartisan/security/config/SecurityInterceptorTest.java` | 拦截器单测 |
| 新增 | `com/cartisan/security/config/SecurityExceptionHandlerTest.java` | 异常处理器单测 |

## 核心流程（伪代码）

```
SecurityInterceptor.preHandle():
    1. 非 HandlerMethod → 放行
    2. 提取注解（方法优先）
    3. @RequireAuth → StpUtil.checkLogin()
    4. @RequireRole → StpUtil.checkRoleOr()
    5. @RequirePermission → StpUtil.checkPermissionOr()
    6. 全部通过 → return true

SecurityExceptionHandler:
    1. NotLoginException → 401 + ApiResponse.error()
    2. NotRoleException → 403 + ApiResponse.error()
    3. NotPermissionException → 403 + ApiResponse.error()
```

---

## 原子任务清单

### Step 1: 生成注解代码

**文件**：
- `com.cartisan.security.annotation.RequireAuth`
- `com.cartisan.security.annotation.RequireRole`
- `com.cartisan.security.annotation.RequirePermission`

**内容**：将 02_interface.md 中的注解描述转为 Java 注解代码

**验证**：
- [ ] 编译通过：`./gradlew :cartisan-security:compileJava`
- [ ] 注解可作用于 TYPE 和 METHOD
- [ ] `@RequireAuth` 有 `boolean value() default true`
- [ ] `@RequireRole` 有 `String[] value()`
- [ ] `@RequirePermission` 有 `String[] value()`

**代码量估算**：约 45 行（3 个注解 × 15 行）

---

### Step 2: 编写 SecurityInterceptor 测试（红灯）

**文件**：`com.cartisan.security.config.SecurityInterceptorTest`

**内容**：基于 AC 编写测试，此时 `SecurityInterceptor` 尚未完全实现

**测试用例**：
- `given_nonHandlerMethod_when_preHandle_then_returnTrue`（AC7）
- `given_noAnnotation_when_preHandle_then_returnTrue`（AC6）
- `given_methodRequireAuth_when_preHandle_then_callCheckLogin`（AC1）
- `given_classRequireAuth_when_preHandle_then_callCheckLogin`（AC1）
- `given_methodOverrideClass_when_preHandle_then_useMethodAnnotation`（AC4）
- `given_requireRole_when_preHandle_then_callCheckRoleOr`（AC2）
- `given_requireRoleMultiple_when_preHandle_then_callCheckRoleOrWithAll`（AC5）
- `given_requirePermission_when_preHandle_then_callCheckPermissionOr`（AC3）
- `given_requirePermissionMultiple_when_preHandle_then_callCheckPermissionOrWithAll`（AC5）

**验证**：
- [ ] 编译通过：`./gradlew :cartisan-security:compileTestJava`
- [ ] 测试运行（红灯）：`./gradlew :cartisan-security:test --tests SecurityInterceptorTest`
- [ ] 每个测试方法都有 `assertNotNull(result)` 或类似的断言（禁止无意义断言）

**技术要点**：
- 使用 `Mockito.mockStatic(StpUtil.class)` 静态 Mock
- 使用 `@Mock` 修饰 `HttpServletRequest`、`HttpServletResponse`
- 使用 `Mockito.verify(mockedStpUtil).checkLogin()` 验证调用

**代码量估算**：约 150 行

---

### Step 3: 编写 SecurityInterceptor 实现（绿灯）

**文件**：`com.cartisan.security.config.SecurityInterceptor`

**内容**：实现 `HandlerInterceptor` 接口，使 Step 2 的测试全绿

**实现要点**：
- 实现 `preHandle()` 方法
- 私有方法 `findAnnotation(Method, Class<?>, Class<A>)` 实现方法优先逻辑
- 使用 `@Component` 注解
- 非 `HandlerMethod` 直接返回 `true`
- 依次检查 `@RequireAuth`、`@RequireRole`、`@RequirePermission`

**验证**：
- [ ] 编译通过：`./gradlew :cartisan-security:compileJava`
- [ ] 测试全绿：`./gradlew :cartisan-security:test --tests SecurityInterceptorTest`
- [ ] ArchUnit 通过：`./gradlew :cartisan-security:test`（完整测试）

**代码量估算**：约 70 行

---

### Step 4: 编写 SecurityExceptionHandler 测试（红灯）

**文件**：`com.cartisan.security.config.SecurityExceptionHandlerTest`

**内容**：基于 AC8 编写测试，验证异常处理

**测试用例**：
- `given_notLoginException_when_handleNotLogin_then_return401WithApiResponse`（AC8）
- `given_notRoleException_when_handleNotRole_then_return403WithApiResponse`（AC8）
- `given_notPermissionException_when_handleNotPermission_then_return403WithApiResponse`（AC8）

**验证**：
- [ ] 编译通过：`./gradlew :cartisan-security:compileTestJava`
- [ ] 测试运行（红灯）：`./gradlew :cartisan-security:test --tests SecurityExceptionHandlerTest`

**技术要点**：
- 使用 `@Mock` 修饰异常实例
- 验证 `ResponseEntity` 的状态码和 body

**代码量估算**：约 80 行

---

### Step 5: 编写 SecurityExceptionHandler 实现（绿灯）

**文件**：`com.cartisan.security.config.SecurityExceptionHandler`

**内容**：实现 `@ControllerAdvice`，使 Step 4 的测试全绿

**实现要点**：
- 使用 `@ControllerAdvice` 注解
- 三个 `@ExceptionHandler` 方法分别处理三种异常
- 使用 `ApiResponse.error()` 构造响应（方法名以 cartisan-web 实际 API 为准）

**验证**：
- [ ] 编译通过：`./gradlew :cartisan-security:compileJava`
- [ ] 测试全绿：`./gradlew :cartisan-security:test --tests SecurityExceptionHandlerTest`
- [ ] 全量测试通过：`./gradlew :cartisan-security:test`

**代码量估算**：约 40 行

---

## 执行顺序

```
Step 1（注解代码）
    │
    ▼
Step 2（拦截器测试 - 红灯）
    │
    ▼
Step 3（拦截器实现 - 绿灯）
    │
    ▼
Step 4（异常处理器测试 - 红灯）
    │
    ▼
Step 5（异常处理器实现 - 绿灯）
    │
    ▼
最终验证：./gradlew :cartisan-security:build
```

---

## 技术依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| `sa-token-spring-boot3-starter` | BOM 管理 | Sa-Token 核心 API |
| `cartisan-web` | project | `ApiResponse` 响应格式 |
| `spring-boot-starter-test` | BOM 管理 | JUnit 5 + AssertJ + Mockito |
| `mockito-inline` | ≥ 3.4.0 | 静态 Mock 支持 |

---

## 测试命名规范

遵循项目 `docs/skills/SKILL.md` 中的 **TEST-002** 规则：
- 格式：`given_{条件}_when_{操作}_then_{预期结果}`
- 或：`should_{预期结果}_when_{条件}`

本计划统一采用 `given_*_when_*_then_*` 格式。

---

## 实现注意事项

1. **静态 Mock**：Mockito 3.4+ 支持 `mockStatic()`，确保 `build.gradle.kts` 依赖版本正确
2. **ApiResponse 方法名**：实现时按 cartisan-web 实际 API 调整（可能是 `error`/`fail`/`of`）
3. **Sa-Token 异常包名**：`cn.dev33.satoken.exception.*`
4. **preHandle 不声明 throws**：`HandlerInterceptor.preHandle` 接口无 `throws Exception`，实现时不加
5. **StpUtil.checkXxx() 返回 void**：Mock 时使用 `thenNothing()` 或仅对失败用例用 `thenThrow()`

---

## 进度跟踪

| Step | 任务 | 状态 |
|------|------|------|
| Step 1 | 生成注解代码 | ✅ |
| Step 2 | SecurityInterceptor 测试（红灯） | ✅ |
| Step 3 | SecurityInterceptor 实现（绿灯） | ✅ |
| Step 4 | SecurityExceptionHandler 测试（红灯） | ✅ |
| Step 5 | SecurityExceptionHandler 实现（绿灯） | ✅ |

**完成日期**：2026-03-14
**测试结果**：14/14 通过
**代码审查**：✅ Ready to Proceed
