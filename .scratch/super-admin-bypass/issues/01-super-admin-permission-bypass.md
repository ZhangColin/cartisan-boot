# 01 — cartisan-security 超管（super-admin）权限 bypass 机制

Type: task
Status: resolved
Raised by: aieducenter-admin（Phase 0 RBAC 修复）

## 背景

cartisan-security 的 `SecurityInterceptor` 强制 `@RequirePermission` / `@RequireRole`，但**没有任何"超管放行"概念**。任何基于 cartisan-security 做 RBAC 的应用，只要有"超级管理员"角色（几乎每个后台都有），都得各自重新发明 bypass 机制。

## 问题（实证）

aieducenter-admin 已踩坑：

- 应用层 `AdminUserPermissionAppService.getPermissions()` 对超管返回**空列表**，代码注释写"由 SaToken 拦截器直接放行"——即应用**预期框架拦截器会对超管短路**。
- 但 `SecurityInterceptor`（`cartisan-security/.../config/SecurityInterceptor.java`）的实现里**没有**任何超管短路：`@RequirePermission` 一律走 `StpUtil.checkPermission(...)`。
- 结果：超管拿到空权限列表 → `checkPermission` 任意权限都失败 → **超管通不过任何 `@RequirePermission` 接口**。内置 `admin` 账号（超管）登录后等于废的。

应用侧能想到的绕法都有缺陷：

1. 在 `StpInterface.getPermissionList` 对超管返回 Sa-Token 通配符 `["*"]`：无文档约定；且 **`@RequireRole` 不覆盖**（Sa-Token 角色校验是精确成员匹配、无通配符）；`["*"]` 还会渗入 `/current-admin` 响应、污染前端权限契约。
2. 整个替换 `SecurityInterceptor` bean（框架提供了 `@ConditionalOnMissingBean` 扩展点）：每个应用重写一遍拦截器、复制维护，违背框架复用初衷。

## 根因

框架缺一个**"谁是超管 + 超管放行"的一等机制**。"超管 bypass 所有权限/角色检查（但仍须登录）"是**通用 RBAC 关注点**，应归框架统一提供，不应每个消费应用各搞一套。

## 候选方案（请框架侧确认问题 + 择一）

### F1（SPI，推荐）

- 新增接口 `SuperAdminResolver { boolean isSuperAdmin(Object loginId); }`。
- `SecurityInterceptor` 注入 `Optional<SuperAdminResolver>`；命中（`isSuperAdmin` 为 true）则跳过 `@RequireRole` / `@RequirePermission` 检查，**但仍保留 `@RequireAuth` 的登录要求**（超管也得先登录）。
- 应用只声明"谁是超管"（admin：有 `SUPER_ADMIN` 角色 → resolver 返回 true），bypass 语义归框架。
- 优点：应用判定标准灵活（角色 / 标记 / 任意条件）；语义清晰；不污染权限列表；向后兼容（resolver bean 不存在时行为不变）。

### F3（配置驱动）

- 属性 `cartisan.security.super-admin-roles=[SUPER_ADMIN]`（角色码列表）。
- 拦截器据用户 `StpInterface.getRoleList` 是否命中该列表决定放行。
- 优点：零应用代码、纯配置。缺点：假定"超管 = 角色码"，耦合角色概念，判定不够灵活。

### F2（仅文档化约定）

- 只文档化"超管在 `StpInterface` 返回 `*` 通配符"。
- 优点：零代码。缺点：不覆盖 `@RequireRole`；`["*"]` 污染前端契约；靠应用记得、无强制。**仅作为 F1/F3 之前的临时说明，不建议作为长期方案。**

## 影响范围

- `cartisan-security`：新增 SPI（F1）或配置（F3）+ `SecurityInterceptor` 改动 + 单元/集成测试。
- 所有消费方应用受益（admin、未来的支付 / identity 管理面等）。
- 向后兼容：`Optional<SuperAdminResolver>` 不存在时行为不变（现有应用零影响）。

## 验收

- 超管登录后，任意 `@RequirePermission` / `@RequireRole` 接口可访问。
- 非超管行为不变（权限 / 角色仍严格校验）。
- 超管未登录时 `@RequireAuth` 仍拦截（不因超管身份免登录）。

## 来源 / 上下文

- 提出：aieducenter-admin Phase 0 修 RBAC bug（起步包必修 Bug ②）。
- admin 侧决策：admin **不在应用层绕**，等本框架完成；见 admin 仓库 `docs/adr/0002-super-admin-bypass-is-framework-gap.md`。
- 相关代码：`cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptor.java`、`.../config/CartisanSecurityAutoConfiguration.java`、`.../config/properties/CartisanSecurityProperties.java`。

## Comments

**框架侧 triage 完成（2026-07-27）**：✅ 接受为框架缺口。但方案**不是** F1/F3/F2 任何一个，
而是经 grilling 修正后的新设计——详见 `CONTEXT.md` 决策日志 Issue 01。

要点：
- **采纳**：中性 SPI `AuthorizationBypassResolver { boolean shouldBypass(Long loginId); }`，
  `SecurityInterceptor` 注入 `Optional<...>`，`@RequireAuth` 之后判断，命中跳过 `@RequireRole`/`@RequirePermission`，bean 不存在时行为不变。
- **否决 F1（`SuperAdminResolver`）**：签名骨架对，但命名特化"超管"业务概念——改为中性 `AuthorizationBypass`（标准 1）。
- **否决 template method**：组合（resolver bean）优于继承（标准 2）。
- **否决 context object**：边缘场景 YAGNI（标准 4）。
- **否决 F3 / F2**：见 CONTEXT.md。

Status → `ready-for-agent`，等框架侧实施 SPI + 拦截器改动 + 测试。

**Resolved（2026-07-28）**：框架侧实现已提交（`b497fd3`）——中性 SPI `AuthorizationBypassResolver` 已提供、`SecurityInterceptor` 已接入（`ObjectProvider` 注入，bean 不存在时行为不变）、测试通过。本 issue 关闭。应用侧（admin）声明 resolver bean 把"超管 → bypass"接上，由消费应用自行处理，不在框架 scope。
