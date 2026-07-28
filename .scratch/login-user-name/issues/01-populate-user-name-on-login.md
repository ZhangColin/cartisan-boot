# 01 — cartisan-security 登录后 userName 自动写入 Session（消除 StpUtil 泄漏 + 防漏写）

Type: task
Status: ready-for-agent
Raised by: aieducenter-admin（Phase 0 RBAC 修复 Bug ④）

## 背景

`RequestContext.userName` 由 `SecurityFilter` 在**每个请求**从 Sa-Token Session 的 `"userName"` key 读取（`cartisan-security/.../context/SecurityFilter.java:47-51`），写入 `RequestContext`。但框架的登录接口 `AuthenticationService.login(...)` **不写入这个 key**——要写入，业务层必须自己在登录后调 `StpUtil.getSession().set("userName", ...)`（接口 javadoc `AuthenticationService.java:66-67` 把这个责任明确推给业务层，且仅是"应"，无强制）。

## 问题（实证）

aieducenter-admin 已踩坑：

- `AdminUserAuthAppService.login()` 只调 `authenticationService.login(adminUser.getId(), timeout)`，**没有**后续 `StpUtil.getSession().set("userName", ...)`。
- 结果：每个 admin 请求的 `RequestContext.userName` 恒为 null。
- 今天是**潜伏**的（admin 暂不读 `userName`，`@CurrentUser` 注入的是 userId）；一旦加审计日志 / 操作日志 / 任何"谁干的（按名）"即踩雷。

应用层能想到的绕法都有缺陷：

1. **按 javadoc 在 login 后补 `StpUtil.getSession().set("userName", ...)`**：能修症状，但要求业务层**直接依赖 Sa-Token**——这与 `AuthenticationService` 接口自身的定位（`AuthenticationService.java:9-10`："业务代码通过此接口管理会话，**不直接依赖 Sa-Token**"）**自相矛盾**。抽象本意是屏蔽 Sa-Token，塞 userName 却逼着调用方越过抽象去摸 `StpUtil`。
2. 每个消费应用自行再封一层"login + setUserName"：重复，且仍绕不开直接依赖 `StpUtil`。

## 根因

框架把"会话建立"（`login`）与"会话身份信息写入"（userName 落 session）**割裂**：前者在抽象内、后者甩给业务层且要求越过抽象去直接操作 `StpUtil`。这是**通用关注点**——任何用 cartisan-security 登录的消费应用都需要 `RequestContext.userName` 非空（审计 / 日志 / "谁干的"），都会踩同一坑。应归框架统一处理，而非每个消费方各自记得。

## 候选方案（请框架侧确认这是否为问题 + 是否框架处理 + 择一）

### F1（login 重载，推荐）

- 新增 `TokenInfo login(Long loginId, long timeoutSeconds, String userName)`；框架在内部 `StpUtil.login(...)` 之后 `getSession().set("userName", userName)`。
- 业务层在登录时（已持有用户名）传入；**永不直接接触 `StpUtil`**，抽象完整。
- 现有两参 / 一参 `login` 重载保留不变（向后兼容），内部可委托新重载（`userName` 传 `null` = 不写，等价旧行为）。
- 优点：最小改动、调用点自然（登录时本就有用户名）、向后兼容、彻底消除"login 后还得记得再 set 一次"。

### F2（抽象上加 session 写入方法）

- `AuthenticationService` 加 `void recordUserName(String userName)`（或更通用的 session-attributes 写入方法）。
- 业务层 login 后调 `authenticationService.recordUserName(name)`——经抽象、不摸 `StpUtil`。
- 优点：解耦（userName 不必在 login 那一刻就有）、可推广到其它 session 属性。缺点：仍是"两步"（login + record），调用方**仍可能漏第二步**（只是漏的代价从"摸 StpUtil"降为"少调一方法"）。

### F3（SPI 自动解析）

- 新增 `UserNameResolver { String resolve(Long loginId); }`；`SecurityFilter`（或 login）据 loginId 自动取 userName 落 session。
- 业务层登录侧**什么都不用做**——不可能漏。
- 优点：防漏最强。缺点：又一 SPI + 登录时一次"按 loginId 查用户名"的开销（resolver 须回调应用的 user 存储）；把"如何查用户名"耦合进框架。

**倾向 F1**：最小、最自然、向后兼容，直接消除反模式与 `StpUtil` 泄漏。**是否采纳、或择 F2/F3、或判定非框架问题，请框架侧确认。**

## 影响范围

- `cartisan-security`：`AuthenticationService` 接口 + `SaTokenAuthenticationService` 实现（F1 加重载 / F2 加方法 / F3 加 SPI + `SecurityFilter` 或 login 改动）+ 单元 / 集成测试。
- 所有消费方受益（admin、identity、未来各管理面）。
- 向后兼容：现有 `login(Long)` / `login(Long, long)` 行为不变（F1 / F2）；F3 为纯叠加。

## 验收

- 业务层经 `AuthenticationService` 登录后（按所选方案传入 / 解析 userName），后续请求 `RequestContext.userName` 非空、为所登录用户名。
- 业务层**不再需要直接依赖 `StpUtil`** 来满足 userName 落 session（抽象不泄漏）。
- 现有 `login(Long)` / `login(Long, long)` 行为不变（向后兼容）。

## 来源 / 上下文

- 提出：aieducenter-admin Phase 0 修 RBAC bug（起步包必修 Bug ④）。
- **admin 侧不被阻塞**：admin 会立即在应用层补 `StpUtil.getSession().set("userName", nickname)` 修症状（Bug ④ admin 侧落地）；本 issue 的目的是**消除根因 + 防止每个消费方重复踩坑**，与 admin 侧修复并行、不互相阻塞。
- 相关代码：`cartisan-security/.../authentication/AuthenticationService.java`（接口 javadoc :9-10 与 :66-67）、`.../authentication/SaTokenAuthenticationService.java`（实现 :34-46，未 set userName）、`.../context/SecurityFilter.java`（:47-51 读 `"userName"`）、`cartisan-web/.../context/RequestContextFilter.java`（网关链路也读 userName 头，同源关切）。
- 工作原则：框架问题在框架修（见 admin 仓库 memory `framework-gaps-raise-requirement`）；需求写明确，框架确认问题 + 同意方案后再改——**不阻塞消费方**，消费方先在应用层修症状（沿用 super-admin-bypass 的先例）。

## Comments

**Triage（2026-07-28）**：✅ 判定为框架问题，方案已定型 → spec 见 `../spec.md`（`ready-for-agent`）。
决策摘要：根因 reframe 为 `login` 契约残缺；采纳**破坏性补全签名**（两个 `login` 重载各加 `String userName`，
不保留旧重载、不 `@Deprecated`）；scope 只锁路径 A（login 写 session），路径 B（`X-User-Name` header 转发）
为下游消费者、另立 issue。完整决策记录见根 `CONTEXT.md` Issue 02。
