# spec：cartisan-security 授权 bypass 扩展点（AuthorizationBypassResolver）

> Feature: `super-admin-bypass`
> 决策依据：`CONTEXT.md` 决策日志 Issue 01 + 判定标准 1-4
> 关联 issue：`issues/01-super-admin-permission-bypass.md`（已 `ready-for-agent`）

## 问题陈述

任何基于 cartisan-security 做后台 RBAC 的业务应用，几乎都有"超级管理员"角色——它需要访问所有受保护接口执行管理操作，但仍须先登录。但框架的 `SecurityInterceptor` 对所有登录用户一视同仁地强制 `@RequirePermission` / `@RequireRole`，没有任何"放行"扩展点。

消费应用因此被迫二选一，两条路都有缺陷：

- 应用层对超管返回空权限列表、注释写"由框架拦截器直接放行"——但框架根本没放行，超管实际通不过任何 `@RequirePermission` 接口（内置超管账号登录即废）。
- 或自行 `@ConditionalOnMissingBean` 重写整个 `SecurityInterceptor`——每个应用复制维护一遍拦截器，违背框架复用初衷。
- 或在 `StpInterface` 对超管返回通配符 `["*"]`——不覆盖 `@RequireRole`（Sa-Token 角色校验是精确成员匹配、无通配符），还会渗入"当前用户权限"响应、污染前端权限契约。

根因：框架缺一个**中性的、可选的授权 bypass 扩展点**。

## 方案

框架提供一个中性 SPI——`AuthorizationBypassResolver`——让消费应用声明"哪些 loginId 跳过授权检查"。`SecurityInterceptor` 在确认用户已登录（`@RequireAuth` 通过）之后、检查角色/权限之前，询问该 resolver；命中则跳过 `@RequireRole` / `@RequirePermission` 检查（**但仍保留 `@RequireAuth` 的登录要求**）。

不实现该 resolver bean 的应用，行为完全不变（向后兼容）。bypass 判定标准（"谁是超管"）完全由应用决定——框架不特化"超管"概念，不强加"超管=某角色码"假定。

## 用户故事

1. 作为框架消费方应用开发者，我想声明"哪些 loginId 可 bypass 授权检查"，以便超管能访问所有受保护接口而无需逐个授权。
2. 作为框架消费方应用开发者，我想用一个简单的 resolver bean 表达 bypass 判定，以便复用框架的 bypass 语义而不重写整个拦截器。
3. 作为框架消费方应用开发者，我想用任意业务策略判定 bypass（角色 / 标记位 / 特定 userId 等），以便框架不强加判定方式假定。
4. 作为框架消费方应用开发者，我想在不实现 resolver 时应用仍正常工作，以便现有应用零改动升级到新版本。
5. 作为框架消费方应用开发者，我想知道 resolver 在每次鉴权请求都会被调用，以便在实现里自行缓存、避免每请求打 DB。
6. 作为超级管理员用户，我想登录后能访问任意 `@RequirePermission` / `@RequireRole` 接口，以便执行管理操作。
7. 作为超级管理员用户，我希望未登录时仍被要求登录，以便超管身份不构成免登录后门。
8. 作为普通（非 bypass）用户，我希望 bypass 机制不影响我的权限校验，以便我不能访问未授权的接口。
9. 作为普通（非 bypass）用户，我希望缺少权限时仍收到 403，以便授权边界保持严格。
10. 作为未登录访客，我希望 bypass 不能让我绕过登录，以便受保护接口仍要求认证。
11. 作为框架维护者，我想让 bypass 只跳过授权、保留认证，以便扩展点不被滥用为免登录后门。
12. 作为框架维护者，我想让 SPI 用中性命名（`AuthorizationBypassResolver`），以便框架不特化"超管"这类业务概念。
13. 作为框架维护者，我想让扩展点用组合（resolver bean）而非继承（template method）实现，以便框架拦截器保持封闭、可独立演化。
14. 作为框架维护者，我想让 SPI 签名保持窄（只接受 loginId），以便不引入臃肿的 context object、不为臆想的未来场景留口子。
15. 作为框架维护者，我想让 resolver 注入是可选的，以便没有 resolver 时行为完全向后兼容。
16. 作为框架维护者，我想让 resolver 结果缓存归应用负责，以便框架不假设缓存失效策略。
17. 作为框架消费方应用开发者，我想在集成测试里验证我的 resolver 配置正确，以便确保超管可访问、非超管被拦截、未登录被拦截。

## 实现决策

**模块**：cartisan-security（新增 SPI + 改 `SecurityInterceptor` + 改 AutoConfiguration 的拦截器注册）。不涉及其它模块。

**新增 SPI**（决策性 prototype，来自 grilling）：

```java
public interface AuthorizationBypassResolver {
    boolean shouldBypass(Long loginId);
}
```

- `loginId` 用 `Long`，对齐框架全局约定（`RequestContext.userId` / `Auditable.createdBy` / `SecurityFilter` 均 `Long`），不用 `Object`——避免 resolver 实现无意义强转，也不为"未来可能用 String/UUID"留口子。
- 命名 `AuthorizationBypassResolver`（bypass 授权），不叫 `SuperAdminResolver`（特化业务概念）、不叫 `AuthBypassResolver`（auth 含义模糊、诱导 bypass 认证）。

**SecurityInterceptor 改动**：

- 构造函数注入 `Optional<AuthorizationBypassResolver>`（或等价的 ObjectProvider）。
- 鉴权顺序保持 `@RequireAuth → @RequireRole → @RequirePermission`（AND 逻辑）不变；在 `@RequireAuth` 检查通过**之后**、`@RequireRole` / `@RequirePermission` **之前**插入 resolver 判断。
- resolver 命中（`shouldBypass` 返回 true）→ 跳过 `@RequireRole` 和 `@RequirePermission`，直接放行。
- **`@RequireAuth` 不参与 bypass**——超管也必须先登录。
- resolver bean 不存在（Optional 空）→ 行为与现状完全一致。

**AutoConfiguration 改动**：

- 拦截器 bean 的创建需改为接收 resolver（通过 ObjectProvider / Optional 解析，bean 不存在时传 empty）。
- 不新增配置属性（不做配置驱动的 `super-admin-roles`）。

**架构决策**（遵守 `CONTEXT.md` 标准 1-4）：

- 标准 1（不特化业务概念）：中性 SPI，"是否 bypass"归应用决策。
- 标准 2（组合优于继承）：resolver bean 注入，非 template method。
- 标准 3（bypass 边界=只授权不认证）：保留 `@RequireAuth`。
- 标准 4（窄 SPI / YAGNI）：单参数，无 context object。

**向后兼容**：resolver bean 不存在时所有现有行为不变；现有消费应用零改动。

**性能契约**：框架对 resolver 结果**不缓存**——每个鉴权 HTTP 请求调用一次。缓存（失效策略、TTL）是应用实现的责任，对齐 Sa-Token `getPermissionList` 的 session 缓存模式。

## 测试决策

**好测试的标准**：只验证外部行为（bypass 生效/不生效时的 HTTP 状态码与响应），不验证 `SecurityInterceptor` 内部方法调用顺序或 `StpUtil` 的调用细节。

**主 seam（单一集成层）**：扩展现有注解鉴权集成测试——这是测 bypass 端到端行为的最高 seam，走真实 Spring MVC + 真实 `SecurityInterceptor` + 真实 Sa-Token。

- prior art：现有注解鉴权集成测试（401/403/200 断言 pattern）、其抽象基类（MockMvc + Sa-Token 上下文）、测试用 Controller（登录端点 + `@RequireAuth` / `@RequireRole` / `@RequirePermission` 受保护端点）。
- 通过测试配置注册一个测试用 `AuthorizationBypassResolver` bean，对某个固定超管 userId 返回 true、其余返回 false（跟随现有测试配置 pattern；只对该 userId 生效，不污染其它继承基类的测试）。

**验收用例**（覆盖原 issue 全部验收）：

1. 超管登录（无 `user:create` 权限）访问 `@RequirePermission` 接口 → 200（bypass 生效）。
2. 超管登录（无 ADMIN 角色）访问 `@RequireRole` 接口 → 200（bypass 生效）。
3. 非 bypass 用户登录（无权限）访问 `@RequirePermission` 接口 → 403（bypass 不影响非 bypass 用户）。
4. 未登录访问 `@RequirePermission` 接口 → 401（`@RequireAuth` 仍拦截，bypass 不免登录）。

**向后兼容验证**：`SecurityInterceptorTest` 构造注入返回 `null` 的 `ObjectProvider`（模拟未提供 resolver bean），现有单元用例无变更全绿——证明未提供 resolver 时 `SecurityInterceptor` 行为完全不变（null provider → 跳过 bypass → 鉴权逻辑照常）。这是向后兼容的直接证明。

**验收用例 ③④（继承现有用例）**：非 bypass 用户仍 403、未登录 `@RequireAuth` 仍 401，由现有 `given_userWithoutPermission_…_403`、`given_noAuth_when_getRequireAuth_…401` 在 resolver 注册的上下文下继续全绿覆盖——证明 resolver 注册不误伤非 bypass 用户、`@RequireAuth` 未登录仍拦截。这些是继承现有用例（非新增）。

**不补 implementation-coupled 单测**：不另加"verify `StpUtil.checkPermission` never called"类用例——验证内部调用细节属 implementation-coupled，集成层 HTTP 状态码已覆盖 bypass 行为；向后兼容由上述单元层的 null provider 证明。

## 范围外

- **不在框架层做"超管"角色判定**：判定"谁是超管"是应用业务策略，由 resolver 实现承担。
- **不缓存 resolver 结果**：缓存与失效策略归应用。
- **不支持 bypass `@RequireAuth`**：认证不可 bypass；不需登录的接口应直接不标 `@RequireAuth` 或走 cartisan-openapi 签名。
- **不引入 context object / method 维度参数**：按接口 bypass、按方法注解 bypass 等边缘场景由应用用其它机制实现。
- **不做配置驱动的 `super-admin-roles` 属性**（原 F3）：耦合"超管=角色码"假定。
- **不文档化 `["*"]` 通配符约定**（原 F2）：不覆盖 `@RequireRole`、污染前端契约。
- **不改消费应用代码**：admin 侧落地（实现 resolver 委托 `isSuperAdmin`、删除误导注释、更新 ADR 0002）是单独的应用侧工作。

## 补充说明

- 决策依据与候选方案否决理由：见 `CONTEXT.md` 决策日志 Issue 01 + 判定标准 1-4。
- 原 issue（已 triage 为 `ready-for-agent`）：`issues/01-super-admin-permission-bypass.md`。
- 消费方落地要点（admin）：实现 resolver 委托 `adminUserPermissionAppService::isSuperAdmin`；删除 `AdminUserPermissionAppService.getPermissions()` 注释"由 SaToken 拦截器直接放行"的误导；更新 admin ADR 0002（从"等框架"→"框架已决策，按此实施"）。
- 命名注意：本 feature 目录名 `super-admin-bypass` 沿用原 issue slug（历史遗留，不改）；SPI 本身用中性命名 `AuthorizationBypassResolver`。
