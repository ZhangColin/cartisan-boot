# 02 — 补全 login 契约：登录时由框架写入 userName

**What to build:** 消费应用经 `AuthenticationService` 登录时传入 `userName`，框架将其写入 Sa-Token Session，
后续请求 `RequestContext.userName` 非空且为所登录用户名；业务层不再需要为 userName 落 session 而直接依赖 `StpUtil`。

两个 `login` 重载（默认超时 / 自定义超时）各增加一个 `String userName` 参数，**破坏性移除**旧的无 `userName`
重载、不 `@Deprecated`。实现内在 `StpUtil.login(...)` 成功之后将 `userName` 写入 Session 的 `"userName"` key
（`SecurityFilter` 已读此 key）；`userName == null` 时不写（等价旧行为，为机器账号等无显示名场景留口子）。
javadoc 删去"业务层应自行 `StpUtil.getSession().set(...)` 将用户名存入 Session"这段把责任外推、且与"不直接依赖
Sa-Token"自相矛盾的措辞，改为说明 `userName` 由框架写入。

测试沿用既有 seam、不开新 seam：integration 为主（扩展测试控制器——登录端点接收 `userName`、当前用户端点在响应中
回显 `RequestContext.getUserName()`，端到端覆盖 *写端 login → Session → 读端 SecurityFilter → RequestContext* 全链路）；
unit（`MockedStatic<StpUtil>` 风格）随签名连带更新、保留 `timeoutSeconds <= 0` / `loginId == null` 等边界测试；
读端 `SecurityFilter` 逻辑不变、其测试维持有效。

完整背景见 `../spec.md` 与 parent `01-populate-user-name-on-login.md`；决策记录见根 `CONTEXT.md` Issue 02。
消费方迁移（admin 等，在其它仓库）不在本 ticket scope。

**Blocked by:** None — 可立即开始

**Status:** resolved

- [x] 两个新重载（默认超时 / 自定义超时）登录传入非空 `userName` 后，后续请求 `RequestContext.userName` 等于所传值
- [x] `userName == null` 时 `RequestContext.userName` 为 `null`（等价旧行为）
- [x] 旧的无 `userName` 重载已移除（漏迁移在编译期暴露，而非运行期潜伏成 `null`）
- [x] 业务层满足 userName 落 session 不再需要直接依赖 `StpUtil`（抽象不泄漏）
- [x] `login` 的 javadoc 不再指引业务层调 `StpUtil.getSession().set(...)`
- [x] 既有 `timeoutSeconds <= 0` / `loginId == null` 等边界测试保持有效

## Resolution

**框架侧已完成并提交（`efcb1e7`，2026-07-28）**：两个 `login` 重载已补全 `String userName` 参数、javadoc 已更新（删除指引业务层调 `StpUtil` 的措辞）、单元 + integration 测试通过。本 issue 关闭。

应用侧（admin 等）的签名迁移由各消费应用自行处理，不在框架 scope。
