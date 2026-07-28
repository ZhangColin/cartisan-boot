# cartisan-boot 框架 — 上下文与职责边界

> 业务无关的 Java 技术基础框架（DDD 六边形架构），供业务项目复用。
> 本文档记录框架的**职责边界判定标准**，以及每个"这是否属于框架问题"的决策与依据。

## 判定标准：什么算"框架应该解决的通用关注点"

1. **不特化业务概念**：当某个需求表面上是"超管 / 灰度账号 / 调试账号"等业务角色，
   框架**不应**为该角色发明一等概念（如 `SuperAdminResolver`）。"谁是该角色"是业务策略，
   框架无法统一；框架只提供**中性的扩展点**（如"是否 bypass 权限检查"），把决策权交给应用。
   角色名由应用定义，bypass 语义由框架提供。

3. **bypass 边界 = 只授权，不认证**：任何 bypass 扩展点只 bypass 授权检查
   （`@RequireRole` / `@RequirePermission`），**保留认证**（`@RequireAuth` 登录要求）。
   认证是基础设施决策，授权是业务策略；"需要 bypass 认证"的场景应通过"不标 `@RequireAuth`"
   或走签名（cartisan-openapi）解决，而非给扩展点开后门。命名须体现边界：
   `AuthorizationBypassResolver`（bypass 授权），不叫 `AuthBypassResolver`。

4. **SPI 职责要窄，YAGNI**：一个 SPI 只回答一个问题、把它回答好
   （如 `AuthorizationBypassResolver` 只回答"这个 loginId 要不要 bypass 授权"）。
   不要用胖 context object 试图覆盖所有未来场景——讨论时列不出的字段、列得出但"几乎不会碰到"
   的字段，都不进 SPI。偶尔的边缘场景改用其它机制实现。窄 SPI + 多个互补 SPI，优于一个万能 context。

2. **扩展点优先组合，而非继承**：框架给应用留扩展点时，优先用可注入的 bean / SPI
   （`Optional<XxxResolver>`），而非 template method 继承。继承（让应用 `extends SecurityInterceptor`）
   会扩大框架 API 面（private→protected）、造成 bean 覆盖 / `@ConditionalOnMissingBean` 条件匹配的
   管理负担（子类也是基类类型）、把应用强耦合到框架内部演化。组合让框架类保持封闭、应用单一职责。

## 决策日志

### Issue 01 — cartisan-security 超管权限 bypass（2026-07-27）

**来源**：aieducenter-admin Phase 0 RBAC 修复
（`.scratch/super-admin-bypass/issues/01-super-admin-permission-bypass.md`）

**判定**：✅ **是框架问题**。`SecurityInterceptor` 无任何 bypass 扩展点，应用只能
`@ConditionalOnMissingBean` 重写整个拦截器（issue 吐槽的"复制维护"痛点根因在此）。
admin 应用层对超管返回空权限列表、注释写"由 SaToken 拦截器直接放行"——应用预期框架会短路，
但框架没做，导致超管过不了任何 `@RequirePermission`。

**采纳方案**：新增中性 SPI

```java
public interface AuthorizationBypassResolver {
    boolean shouldBypass(Long loginId);
}
```

`loginId` 用 `Long` 对齐框架全局约定（`RequestContext.userId` / `Auditable.createdBy` /
`SecurityFilter` 均 `Long`），不用 `Object`——避免 resolver 实现无意义强转，也不为"未来可能用
String/UUID"的臆想留口子（标准 4）。`SecurityInterceptor` 注入 `Optional<AuthorizationBypassResolver>`，
在 `@RequireAuth` 通过之后判断；命中则跳过 `@RequireRole` / `@RequirePermission`
（**保留 `@RequireAuth`**）。resolver bean 不存在时行为不变（向后兼容）。
符合标准 1（不特化超管）、2（组合注入）、3（只 bypass 授权）、4（窄 SPI）。

**否决方案**：
- ❌ admin 原 F1 `SuperAdminResolver`：特化"超管"业务概念，违背标准 1。
- ❌ template method hook：继承带来 bean 管理 / API 面 / 强耦合负担，违背标准 2。
- ❌ context object（loginId + loginType + method）：边缘场景几乎不会碰到，偶尔需要改用其它方式实现，YAGNI，违背标准 4。
- ❌ admin 原 F3（配置驱动 super-admin-roles）：耦合"超管 = 角色码"假定，判定不灵活。
- ❌ admin 原 F2（仅文档化 `*` 通配符）：不覆盖 `@RequireRole`、污染前端权限契约。

**实施备注**：
- **性能 / 缓存**：resolver 每个鉴权 HTTP 请求被调一次；应用实现须自行缓存
  （参考 Sa-Token `getPermissionList` 的 session 缓存）。框架不缓存——不知失效策略（角色变更后容忍多久延迟是业务决策）。
- **验收**：超管登录可访问任意 `@RequirePermission` / `@RequireRole`；非超管行为不变；超管未登录 `@RequireAuth` 仍拦截。

**消费方落地**：admin 实现 resolver 委托 `adminUserPermissionAppService.isSuperAdmin(loginId)`；
删除 `AdminUserPermissionAppService.getPermissions()` 注释里"由 SaToken 拦截器直接放行"的误导性说明。

### Issue 02 — cartisan-security 登录写入 userName（消除 StpUtil 泄漏 + 补全 login 契约）（2026-07-28）

**来源**：aieducenter-admin Phase 0 RBAC 修复 Bug ④
（`.scratch/login-user-name/issues/01-populate-user-name-on-login.md`）

**判定**：✅ **是框架问题**。`SecurityFilter` 在框架内从 Sa-Token Session 的 `"userName"` key
读 userName 写入 `RequestContext`（**读端已在框架内**），但 `AuthenticationService.login(...)`
**不写**这个 key，把"userName 落 session"甩给业务层、且要求业务层越过抽象直接调
`StpUtil.getSession().set("userName", ...)`——与接口自身 javadoc"业务代码通过此接口管理会话，
**不直接依赖 Sa-Token**"自相矛盾。读写不对称 + 抽象泄漏，且任何消费应用（审计 / 日志 / "谁干的"）
都需要 `RequestContext.userName` 非空，都会踩同一坑。属通用关注点。

**根因 reframe**：不是"缺一个方便的重载"，而是 **`login` 契约残缺**——userName 是登录身份的一部分
（`RequestContext` 一等字段、`SecurityFilter` 必读），却没进 `login` 签名。补全契约，泄漏与漏写一并消除。

**采纳方案**：**破坏性补全 `login` 签名**——直接改现有两个重载、不保留无 userName 的旧版本：

```java
TokenInfo login(Long loginId, String userName);                     // 默认 timeout
TokenInfo login(Long loginId, long timeoutSeconds, String userName); // 自定义 timeout
```

实现内 `StpUtil.login(...)` 之后 `getSession().set("userName", userName)`。
`userName == null` 时不写 session（等价旧行为），为机器账号等无 displayName 的边缘场景留口子；javadoc 鼓励非空。
符合标准"不特化业务概念"（userName 作 opaque 字符串由调用方传入，框架不解析"用户名取什么字段"）。

**否决方案**：
- ❌ spec 原 F1（加重载 + 旧重载保留不变 / 向后兼容）：留下"残缺契约的遗物"，且 (B) 防漏写非强保证。
- ❌ `@Deprecated` 旧重载 + 加新重载：调用点已知且少（admin / identity），不值得维护两套；**直接改更干净**。
- ❌ F2（`recordUserName` 两步法）：仍可漏第二步，未消除根因。
- ❌ F3（`UserNameResolver` SPI 自动解析）：防漏最强，但把"如何按 loginId 查用户名"耦合进框架——
  "用户名取 nickname 还是 realName"是业务策略，违背标准"不特化业务概念"；且引入登录时一次 user 存储回调。
- ❌ `LoginRequest` record 入参：违背标准"窄 / YAGNI / 反胖 context"，且与现有重载风格不一致。

**实施备注**：
- **破坏性变更**：现有 `login(Long)` / `login(Long, long)` 签名移除。所有消费方登录调用点必须改传 userName；
  自行实现 `AuthenticationService` 的应用须同步改签名。spec 验收第 3 条"向后兼容"**作废**。
- **stale name**：用户改名后 session 内 userName 过期，直到重新登录刷新。可接受（Sa-Token session 本就是临时态；
  强一致需应用在改名时主动重写 session 或踢出重登，不在框架 scope）。
- **scope 只锁路径 A 写入端**：`cartisan-web/RequestContextFilter` 从 `X-User-Name` header 读 userName 属
  **路径 B（网关→下游）**，是路径 A 的下游消费者——上游 `SecurityFilter` 从 session 读 userName 填进
  `RequestContext`，网关转发时取该值注入 header，下游即读到。login 写 session 修好后，B 的输入源自动有值；
  本次不动 header 转发链路。若网关侧 ctx→header 转发机制缺失，另立 issue（不阻塞本次）。

**验收**：
- 消费方经新 `login(loginId, userName)` 登录后（传非空 userName），后续请求 `RequestContext.userName` 非空、为所登录用户名。
- 业务层不再直接依赖 `StpUtil` 来满足 userName 落 session（抽象不泄漏）。
- 破坏性：旧的无 userName 重载移除，调用点一次性迁移。

**消费方落地**：admin 的 `AdminUserAuthAppService.login()` 改为
`authenticationService.login(adminUser.getId(), timeout, adminUser.getNickname())`，
并移除应用层手补 `StpUtil.getSession().set(...)` 的临时修复（Bug ④ admin 侧落地后回收）。
