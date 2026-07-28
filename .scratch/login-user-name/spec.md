# Spec — 登录时由框架写入 userName（补全 login 契约，消除 StpUtil 泄漏）

## Problem Statement

作为消费 `cartisan-security` 的应用开发者，我用框架的 `AuthenticationService.login(...)` 建立会话后，
后续每个请求的 `RequestContext.userName` 恒为 `null`。原因是框架的 `SecurityFilter` 在每个请求从
Sa-Token Session 的 `"userName"` key 读取 userName 写入 `RequestContext`，但 `login(...)` 本身
**不写这个 key**——接口 javadoc 把"写 userName 落 session"的责任甩给业务层，且要求业务层越过抽象
直接调 `StpUtil.getSession().set("userName", ...)`。

这造成两个伤害：

1. **抽象自相矛盾**：`AuthenticationService` 的定位是"业务代码通过此接口管理会话，**不直接依赖 Sa-Token**"，
   可要塞 userName 却逼着调用方去摸 `StpUtil`。抽象本意是屏蔽 Sa-Token，写 userName 却撕开了口子。
2. **潜伏的 null 陷阱**：消费方很容易漏写那一步（aieducenter-admin 已实证踩坑）。今天 `userName` 暂没人读，
   看似无事；一旦加审计日志 / 操作日志 / 任何"谁干的（按名）"，`RequestContext.userName` 就是 `null`。

## Solution

把 `login` 契约补全：登录时直接接收 `userName`，由框架在内部写入 Sa-Token Session。
应用层在登录时本就持有用户名（刚认证完），传入即可，**永不直接接触 `StpUtil`**；
`RequestContext.userName` 在后续请求中自然非空、为所登录用户名。

这是一次**破坏性补全**：直接修改现有两个 `login` 重载的签名以纳入 `userName`，不保留无 `userName`
的旧重载、不加 `@Deprecated` 过渡。理由是 `cartisan-security` 为内部框架、消费方与登录调用点已知且少，
维护"残缺契约的遗物"是纯技术债；直接改让契约干净，且让漏迁移在**编译期**暴露而非运行期潜伏。

## User Stories

1. 作为消费应用开发者，我希望登录时能把用户名传给框架，这样我不必直接依赖 Sa-Token 来满足会话身份写入。
2. 作为消费应用开发者，我希望登录成功后 `RequestContext.userName` 自动非空，这样审计日志 / 操作日志能可靠记录"谁干的"。
3. 作为消费应用开发者，我希望默认超时与自定义超时两种登录方式都能传入 `userName`，这样两种登录路径行为一致。
4. 作为消费应用开发者，我希望 `userName` 允许传 `null`（机器账号 / 内部服务账号等无显示名场景），这样不会被迫编造一个名字、也不会校验失败。
5. 作为消费应用开发者，我希望应用层原先"登录后手动 `StpUtil.getSession().set(...)`"的临时修复能被框架原生能力替代，这样我能删除那处临时代码与对 Sa-Token 的直接依赖。
6. 作为框架维护者，我希望 `login` 写 userName 与 `SecurityFilter` 读 userName 在同一抽象内闭环，这样读写对称、不再"读端在框架、写端甩给业务层"。
7. 作为框架维护者，我希望 `login` 契约完整表达"登录身份"（loginId + userName），这样接口承诺（不依赖 Sa-Token）与 javadoc 指引不再自相矛盾。
8. 作为框架维护者，我希望接口 javadoc 删去"业务层应通过 `StpUtil.getSession().set(...)` 将用户名存入 Session"这段把责任外推的措辞，这样文档与实现一致、不再误导消费方去摸 `StpUtil`。
9. 作为审计 / 日志的消费者，我希望每条请求的 `RequestContext.userName` 反映真实登录用户名，这样审计记录可信、可追责。
10. 作为应用层开发者，我希望登录接口的迁移是"一次性、显式、编译期可见"的（旧签名移除后编译失败），而不是悄悄退化成运行期 `null`——这样破坏性变更是安全的。
11. 作为自行实现 `AuthenticationService` 的应用开发者，我希望框架对"实现类须同步采用新签名"这件事是确定的、文档化的，这样我升级框架时知道要改哪里。
12. 作为网关下游服务的开发者，我希望上游登录写好 `userName` 后，转发下来的 `X-User-Name` header 自动有值，这样下游服务的 `RequestContext.userName` 也非空（本 spec 不改下游链路，但应作为自然收益成立）。
13. 作为消费应用开发者，我希望框架把 `userName` 当作不透明字符串、不去规定"用户名应取 nickname 还是 realName"，这样字段语义由我的业务决定。

## Implementation Decisions

- **模块**：仅 `cartisan-security`。
- **接口变更（破坏性）**：`AuthenticationService` 的两个 `login` 重载签名各增加一个 `String userName` 参数——
  一个对应默认超时、一个对应自定义超时。移除无 `userName` 的旧重载，**不保留、不 `@Deprecated`**。
- **行为**：实现内在 `StpUtil.login(...)` 成功之后，将 `userName` 写入 Sa-Token Session 的 `"userName"` key
  （`SecurityFilter` 已从该 key 读取）。`userName == null` 时**不写** session（等价旧行为），为无显示名的边缘场景留口子。
- **API 契约**：`userName` 是不透明字符串，框架不解析其字段来源；调用方决定传 `nickname` / `realName` / 账号均可。
- **向后兼容**：不保留。所有登录调用点一次性迁移；自行实现 `AuthenticationService` 的应用须同步采用新签名。
  原 spec 验收里"现有 `login(Long)` / `login(Long, long)` 行为不变（向后兼容）"一条**作废**。
- **javadoc 更新**：移除 `login` 文档里"业务层应通过 `StpUtil.getSession().set("userName", ...)` 将用户名
  存入 Session"这段把责任外推、且与"不直接依赖 Sa-Token"自相矛盾的措辞；改为说明 `userName` 由框架写入。
- **否决的替代方案**（理由见 CONTEXT.md Issue 02）：
  - 旧重载 `@Deprecated` + 加新重载（过渡兼容）——调用点已知且少，不值得维护两套。
  - `recordUserName(String)` 两步法——仍可漏第二步，未消除根因。
  - `UserNameResolver` SPI 自动解析——把"如何按 loginId 查用户名"耦合进框架，违"不特化业务概念"。
  - `LoginRequest` record 入参——违"窄 / YAGNI / 反胖 context"，且与现有重载风格不一致。

## Testing Decisions

- **好测试的标准**：只测外部可见行为——"登录传入 `userName` 后，后续请求 `RequestContext.userName` 的取值"，
  不测 `StpUtil` 内部调用序列等实现细节。
- **主验证 seam（integration）**：沿用 `cartisan-security` 既有的 Web 集成测试 seam（`@SpringBootTest` + MockMvc，
  经由测试控制器端点间接驱动）。扩展该测试控制器：登录端点接收 `userName`、回显当前用户信息的端点在响应中
  返回 `RequestContext.getUserName()`。端到端覆盖 **写端(login) → Session → 读端(SecurityFilter) → RequestContext**
  全链路。验证点：
  - 登录传入非空 `userName` → 后续请求 `RequestContext.userName` 等于所传值；
  - 登录传入 `null` `userName` → 后续请求 `RequestContext.userName` 为 `null`（等价旧行为）；
  - 默认超时与自定义超时两个登录重载均覆盖。
- **连带维护（unit）**：既有针对实现类的单元测试（`MockedStatic<StpUtil>` 风格）随签名变更同步更新；
  保留 `timeoutSeconds <= 0`、`loginId == null` 等边界测试。这是既有 seam 的连带维护，**不为本 feature 新开 seam**。
- **读端无需改动**：`SecurityFilter` 读 `"userName"` 的逻辑不变，其既有单元测试维持有效。
- **Prior art**：既有认证服务集成测试（login → 取 token → 带 token 访问回显端点）、既有 `SecurityFilter`
  单元测试（session 有 / 无 `userName` → `RequestContext.userName`）。
- **seam 数量**：沿用既有 integration + unit 两个 seam，不开新 seam；feature 行为的最高验证点上移到 integration。

## Out of Scope

- **网关 → 下游的 `X-User-Name` header 转发链路（路径 B）**：它是路径 A（login 写 session）的下游消费者，
  本次修好 login 后其输入源自动有值；`RequestContext → X-User-Name header` 的转发机制若缺失，另立 issue。
- **userName 改名后的 session 强一致刷新（stale name）**：Sa-Token Session 是临时态，用户改名后 `userName` 过期
  直到重新登录。强一致需应用在改名时主动重写 session 或踢出重登，不在框架 scope。
- **`UserNameResolver` SPI / `recordUserName` 两步法 / `LoginRequest` record**：均经评估否决（见上）。
- **`AuthenticationService.authenticate(...)` 的默认实现**：身份验证是业务层职责，不在本 spec。

## Further Notes

- **根因 reframe**：本问题不是"缺一个方便的重载"，而是 `login` 契约残缺——`userName` 本是登录身份的一部分
  （`RequestContext` 一等字段、`SecurityFilter` 必读），却没进 `login` 签名。补全契约后，"StpUtil 泄漏"与"漏写"
  一并消除。完整决策记录见 `CONTEXT.md` Issue 02。
- **来源**：aieducenter-admin Phase 0 RBAC 修复 Bug ④（`.scratch/login-user-name/issues/01-populate-user-name-on-login.md`）。
  admin 侧不被阻塞：会先在应用层补 `StpUtil.getSession().set(...)` 修症状；本 spec 消除根因、防止每个消费方重复踩坑。
- **消费方落地（示例）**：admin 登录服务迁移到新签名、传入用户昵称，并删除应用层手补 `StpUtil` 的临时修复。
- **工作原则**：框架问题在框架修；需求写明确、方案确认后再改，不阻塞消费方先在应用层修症状（沿用既有 bypass 先例）。
