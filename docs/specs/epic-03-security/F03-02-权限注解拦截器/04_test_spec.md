# Feature: F03-02 权限注解 + MVC 拦截器 — 测试规格

## 测试策略

### 单元测试
使用 JUnit 5 + AssertJ + Mockito，遵循 `given_when_then` 命名规范（TEST-002）。

### 测试分层

| 测试类 | 覆盖内容 | 测试数 |
|--------|---------|--------|
| `SecurityInterceptorTest` | 拦截器鉴权逻辑 | 11 |
| `SecurityExceptionHandlerTest` | 异常转 ApiResponse | 3 |
| **总计** | | **14** |

---

## 测试用例清单

### SecurityInterceptorTest

| 用例ID | 测试方法 | 验证内容 | 对应AC |
|--------|---------|---------|--------|
| T1 | `given_nonHandlerMethod_when_preHandle_then_returnTrue` | 非 HandlerMethod 直接放行 | AC7 |
| T2 | `given_noAnnotation_when_preHandle_then_returnTrue` | 无注解放行 | AC6 |
| T3 | `given_methodRequireAuth_when_preHandle_then_callCheckLogin` | 方法 @RequireAuth 生效 | AC1 |
| T4 | `given_classRequireAuth_when_preHandle_then_callCheckLogin` | 类 @RequireAuth 生效 | AC1 |
| T5 | `given_classAndMethodRequireAuth_when_methodHasFalse_then_doNotCheckLogin` | 方法 @RequireAuth(false) 覆盖类注解 | AC4 |
| T6 | `given_classRequireAuthAndMethodRequireRole_when_preHandle_then_callBoth` | 类+方法不同注解 AND 逻辑 | AC4 |
| T7 | `given_requireRoleSingle_when_preHandle_then_callCheckRoleOr` | 单值角色检查 | AC2 |
| T8 | `given_requireRoleMultiple_when_preHandle_then_callCheckRoleOrWithAll` | 多值角色 OR 逻辑 | AC5 |
| T9 | `given_requirePermissionSingle_when_preHandle_then_callCheckPermissionOr` | 单值权限检查 | AC3 |
| T10 | `given_requirePermissionMultiple_when_preHandle_then_callCheckPermissionOrWithAll` | 多值权限 OR 逻辑 | AC5 |

### SecurityExceptionHandlerTest

| 用例ID | 测试方法 | 验证内容 | 对应AC |
|--------|---------|---------|--------|
| T11 | `given_notLoginException_when_handleNotLogin_then_return401WithApiResponse` | NotLoginException → 401 | AC8 |
| T12 | `given_notRoleException_when_handleNotRole_then_return403WithApiResponse` | NotRoleException → 403 | AC8 |
| T13 | `given_notPermissionException_when_handleNotPermission_then_return403WithApiResponse` | NotPermissionException → 403 | AC8 |

---

## Mock 策略

### StpUtil 静态 Mock

使用 Mockito 3.4+ 的 `mockStatic()`：

```java
MockedStatic<StpUtil> mockedStpUtil = mockStatic(StpUtil.class);

// 验证调用
mockedStpUtil.verify(StpUtil::checkLogin);

// 验证未调用
mockedStpUtil.verify(StpUtil::checkLogin, never());

// 清理
mockedStpUtil.close();
```

---

## 测试结果

| 指标 | 结果 |
|------|------|
| 测试总数 | 14 |
| 通过 | 14 |
| 失败 | 0 |
| 覆盖率估算 | ≥ 80% |

---

## 交叉审查记录

| 日期 | 审查人/模型 | 范围 | 结论 |
|------|------------|------|------|
| 2026-03-14 | code-reviewer (Opus 4.6) | F03-02 完整实现 | ✅ Ready to Proceed |

**审查亮点**：
- 优秀的测试覆盖率
- 遵循 DDD 架构原则
- 正确实现方法注解优先级
- 完善的文档和注释

**改进建议**：
- 考虑在 F03-07 后添加集成测试（端到端测试）
- 后续可添加 ArchUnit 规则防止循环依赖

---

## 变更记录

| 日期 | 变更内容 |
|------|---------|
| 2026-03-14 | 初始版本，Phase 5 归档 |
