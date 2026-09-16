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

### Issue 03 — GlobalExceptionHandler 处理 DB 完整性冲突（重复键→409，其余→400）（2026-07-28）

**来源**：aieducenter-app-registry 设计 `app_code` 全局唯一撞名时浮现
（`.scratch/data-integrity-violation-handling/issues/01-data-integrity-violation-handling.md`）

**判定**：✅ **是框架问题**。`GlobalExceptionHandler` 没接 `DataIntegrityViolationException`
体系，任何 DB 唯一/外键/check/not-null 约束冲突都冒泡到兜底 `handleException(Exception)` →
**HTTP 500**。500 是**语义错误**：约束冲突是**客户端冲突（4xx）**，不是服务故障；500 触发监控告警噪音、
误导调用方、丢失"是哪类约束"的信息。这是**通用缺口**——任何用唯一约束/外键的消费应用，
并发 race 或漏查重时都会撞到（"先查再存"关不掉并发 race，DB 唯一约束才是真保证）。

**采纳方案**：C1 精确分级，复用 Spring 已做的异常分类，新增两个 `@ExceptionHandler`：

```java
@ExceptionHandler(DuplicateKeyException.class)            // 最具体，优先匹配
public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException ex) {
    log.warn("Duplicate key violation: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(BaseCodeMessage.CONFLICT).withRequestId(currentRequestId()));
}

@ExceptionHandler(DataIntegrityViolationException.class)   // 父类：外键/check/not-null 等
public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(BaseCodeMessage.BAD_REQUEST).withRequestId(currentRequestId()));
}
```

Spring MVC 按"最具体匹配"——`DuplicateKeyException` handler 优先于父类，二者共存不冲突。
**响应体只给通用文案**（`CONFLICT`="Resource conflict" / `BAD_REQUEST`="Invalid request"），
DB 原始消息（含 constraint / 列名等 schema 细节）**只 WARN 入日志**供运维排查，不进响应体
（避免信息泄漏）。与现有"4xx→WARN、5xx→ERROR"日志策略一致。

**否决方案**：
- ❌ C3（仅文档化"消费方先查再存"）：关不掉并发 race，治标不治本。
- ❌ C2（只接父类 `DataIntegrityViolationException` 统一一个码）：丢失"重复 vs 外键/check"的区分，
  而 Spring 已免费做了分类——没必要降级。
- ❌ 复用 `BaseCodeMessage.DUPLICATE`（"Duplicate resource: {0}"）：框架层接 `DuplicateKeyException`
  时**填不出 `{0}`**（不知是哪个业务字段），`ApiResponse.error(DUPLICATE)` 会渲染出字面 `{0}`。
- ❌ 透出 `ex.getMessage()` 到响应体：泄漏 DB schema 细节（表/列/constraint 名）到 API 响应，
  对业务无关框架是信息泄漏 smell；细节 WARN 入日志即可。
- ❌ 新增 `BaseCodeMessage.DATA_INTEGRITY_VIOLATION` 专用码：`CONFLICT` / `BAD_REQUEST` 已是
  无占位符的通用文案，复用即可，YAGNI。

**实施备注**：
- **依赖**：`DataIntegrityViolationException` / `DuplicateKeyException` 位于 `spring-tx` jar
  （不在 spring-context / spring-jdbc）。cartisan-web 原本经 `spring-boot-starter-data-redis`
  传递性拿到 spring-tx；本模块现直接引用这些类型，**显式声明 `spring-tx` 依赖**（版本由 Spring Boot BOM 管理）。
- **范围**：仅 `cartisan-web` 的 `GlobalExceptionHandler`；不动消费方"先查再存"主路径（仍返具体字段消息）。
- **向后兼容**：纯新增 handler，无签名变更；非完整性异常仍走原 500 兜底，行为不变。

**验收**：
- 唯一/重复键冲突 → HTTP **409**（非 500），`code=409`、`message="Resource conflict"`。
- 其余完整性冲突（外键/check/not-null）→ HTTP **400**（非 500），`code=400`、`message="Invalid request"`。
- DB 细节不进响应体；消费方"先查再存"主路径不受影响（仍返具体字段消息）。
- `GlobalExceptionHandlerTest` 覆盖 `DuplicateKeyException → 409` 与 `DataIntegrityViolationException → 400`。

**消费方落地**：app-registry 等无需改动；DB 唯一约束兜底路径从 500 自动变 409。

### Issue 04 — cartisan-openapi 去 signature permissions（机机 ACL 是臆想需求）（2026-07-28）

**来源**：aieducenter-app-registry 签名 facet 设计审视
（`.scratch/apikey-info-permissions/issues/01-apikey-info-permissions-necessity.md`）

**判定**：✅ **是框架问题，且应"简化去掉"**。`ApiKeyInfo.permissions` + `@RequireSignature(permission)`
的 per-key 机机 ACL **零消费方使用**：
- `hcy_payment`（参考实现）：全部裸用 `@RequireSignature`（无一处传 `permission=`）；其 `ApiKey` 聚合自存
  permissions 字段，但从未接到框架的 ACL 检查上。
- `aieducenter-platform`：grep 到的 `permissions` 全是 admin RBAC（`@RequirePermission` 那套），与 openapi 签名无关。
- `aieducenter-app-registry`（提 issue 方）：尚未落地，明确表示不要 per-key ACL。

属**臆想需求（speculative）**，违背标准 4（SPI 职责要窄，YAGNI）。per-key 权限码的增删改/分配/校验
让 apiKey 管理复杂化；这种粒度的机机 ACL 更像腾讯/AWS 量级多租户不可信接入才需要——对内部
first-party 平台，"已登记应用可调"（签名 = 认证）已足够。

**根因 reframe**：不是"缺一个文档化的空集默认值"，而是 **框架为不存在的需求预留了一等概念**。
permissions 字段、注解属性、拦截器 403 分支、provider JSON 解析——整条 ACL 链路都在维护一个
没人用的能力。删字段不如删概念：signature 回归纯**认证**，ACL 留给应用层（若未来真要）。

**采纳方案**：**硬删，不留 hook**（纯 YAGNI，对齐 Issue 02 破坏性补全先例）。signature = 认证一等用法：

```java
// ApiKeyInfo：5 字段 → 4 字段，删 hasPermission
public record ApiKeyInfo(String apiKey, String appName, String apiSecret, String status) {
    public boolean isActive() { return "ACTIVE".equalsIgnoreCase(status); }
}

// @RequireSignature：去 permission 属性，变裸标记注解
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSignature {}
```

`SignatureVerificationInterceptor` 删 403 权限块，瘦身为"**必须验签**闸"：标了 `@RequireSignature`
的端点，若 request attribute 无验签成功写入的 `ApiKeyInfo` → 401。该闸不可去——否则 `@RequireSignature`
变空操作（`SignatureVerificationFilter` 仅在带 `X-Api-Key` 时验签、不带则放行，需拦截器把"标注即强制"补上）。
`RemoteApiKeyProvider` 不再解析远端 JSON 的 `permissions`（远端若仍返回，直接忽略，wire 兼容）。

**否决方案**：
- ❌ 保留 + 文档化"空集为默认"（issue 疑问 3）：留下臆想概念的一等地位，框架表面积不变、维护负担不减，
  违背标准 4。"传 `Set.of()`"是消费方绕开框架赘肉的 workaround，不是该长期依赖的契约。
- ❌ 硬删但留窄 SPI（如 `SignatureAclResolver`）备用：预留未使用的扩展点本身就是 speculative，与 YAGNI
  取舍直接冲突；未来真有需求时按 `AuthorizationBypassResolver` 模式新增窄 SPI 即可，不必现在留口子。
- ❌ `@Deprecated` 软退场：对一个零消费方的臆想字段，软退场是仪式大于实质（无人在用、无 graceful 期可过渡），
  反而让赘肉多活一个版本。不如一次性删干净。

**实施备注**：
- **破坏性变更**：`ApiKeyInfo` record ctor 签名变更（去 permissions 参数）；`@RequireSignature`
  去 `permission()` 属性；`ApiKeyInfo.hasPermission(...)` 移除。自行构造 `ApiKeyInfo` 或实现
  `ApiKeyProvider` 的消费方须同步去 permissions 参数；用了 `@RequireSignature(permission=...)` 的
  消费方须改为裸 `@RequireSignature`（实测当前无此调用点）。
- **范围**：仅 `cartisan-openapi`。`SignatureVerificationFilter`（认证主路径）不动；`@NoSignature`
  排除注解不动；nonce/timestamp/HMAC 验签链路不动。
- **wire 兼容**：`RemoteApiKeyProvider` 不再读远端响应的 `permissions` 字段，但远端（如 hcy_payment
  的 api-key 管理服务）继续返回也无妨——框架忽略，不报错。hcy_payment 自身 `ApiKey` 聚合的 permissions
  是它**自己的**领域概念，不属框架 scope，不在本次改动内。

**验收**：
- `ApiKeyInfo` 为 4 字段 record，无 `permissions` / `hasPermission`。
- `@RequireSignature` 为无属性裸标记注解。
- 标 `@RequireSignature` 的端点：带有效签名 → 放行；无有效签名（无 `ApiKeyInfo` attribute）→ **401**。
- 不再有 403 "Permission denied" 路径。
- `RemoteApiKeyProvider` 对远端响应含/不含 `permissions` 字段均正常构造 `ApiKeyInfo`（忽略该字段）。
- `cartisan-openapi` 模块测试全绿。

**消费方落地**：
- app-registry：`ApiKeyInfo` 构造少传一字段（本就打算传 `Set.of()`），无需建 permissions 列/管理 UI。
- hcy_payment：controller 处裸 `@RequireSignature` 无需改；若迁到框架 `ApiKeyInfo`，去 permissions 参数。
  其自身 `ApiKey` 聚合的 permissions 字段保留与否由 hcy_payment 自行决定（非框架约束）。

### Issue 05 — cartisan-data-jpa 软删读过滤修复：编程式注册 restriction（2026-07-30）

**来源**：#2（软删读过滤整体失效——`@SQLRestriction` 在 `@MappedSuperclass` 上不被实体继承），
下游 `aieducenter-admin` 前端 E2E 发现（admin#7，含 curl 复现脚本）。完整 spec 见 #3，拆分为 #4–#7 落地。

**判定**：✅ **是框架问题**。`AuditableSoftDeletable`（`@MappedSuperclass`）上声明的
`@SQLRestriction("deleted = false")` 从不生效——该注解的元注解无 `@Inherited`，Hibernate 也不从
`@MappedSuperclass` 拾取类级注解到具体实体子类（javap 与生成 SQL 双重实证）。直接实现 `SoftDeletable`
但未自声明注解的实体同样无读过滤。读路径除该注解外无任何兜底（全模块 grep 确认无 Filter /
Specification 包装）。结果：`findById` / `findAll` / Specification / 派生查询 / JPQL 全部泄漏
`deleted = true` 记录——框架文档承诺的「所有查询自动排除已删记录」整体不成立。属通用关注点，
一处框架修复全愈（所有继承该基类的下游聚合零改动获益）。

**根因 reframe**：不是「注解写错了位置」这一处笔误，而是**读过滤缺少一处真正生效的注册点**。
`@SQLRestriction` 贴在 MappedSuperclass 上是死代码——既不生效，又误导维护者以为读侧有保护。
要补的不是注解，是「在元模型构建期为每个 `SoftDeletable` 实体真正注入 where 片段」的注册机制。

**采纳方案 A——编程式注册 restriction**：新增 Hibernate `AdditionalMappingContributor`
（`SoftDeletableRestrictionContributor`），在元模型构建期（所有实体绑定完成之后、SessionFactory
构建之前）遍历根实体，对**实现 `SoftDeletable` 且未显式声明 restriction** 的实体设置等价于
`@SQLRestriction("deleted = false")` 的 where 片段（`RootClass.setWhere(...)`）：

```java
if (SoftDeletable.class.isAssignableFrom(mappedClass)) {
    requireDeletedColumn(rootClass, mappedClass);    // 启动期 fail-fast（见下）
    if (isNotBlank(rootClass.getWhere())) continue;  // 显式优先：不覆盖、不叠加
    rootClass.setWhere("deleted = false");
}
```

**注册方式（对 spec 的简化）**：spec 设想经 `HibernatePropertiesCustomizer`
（`hibernate.additional_mapping_contributors`）装配；实现采用更底层的 **Java ServiceLoader**
（`META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor`）——无需 Spring 自动配置、
无需 Hibernate 属性，**只要 cartisan-data-jpa 在 classpath 即全局生效**，并覆盖纯 Hibernate（非 Spring）
场景。restriction 片段固定 `deleted = false`；`SoftDeletable` 接口契约同步收紧：实现者必须映射
`deleted` boolean 列（写入接口 javadoc）。

**否决方案**：
- ❌ B（Hibernate `@Filter` + 自动启用）：`@Filter` **不作用于按 id 加载**——`find` / `getReference`
  走 `EntityPersister` 主键路径、绕过 Filter，堵不住 `findById`，而 id 查询正是详情接口的主路径；
  且 Filter 需每次 session 手动 `enableFilter(...)`、易漏。
- ❌ C（`BaseRepositoryImpl` 读路径对 `SoftDeletable` 统一补 `deleted = false` 谓词）：只覆盖基类
  重写的方法，**拦不住应用自定义的派生查询（`findByName`）与显式 `@Query` JPQL**——这些走
  `SimpleJpaRepository` 之外的查询路径，基类插不进谓词，会留下「框架方法安全、自定义查询泄漏」的
  不一致半成品。与写侧覆盖同风格的诱惑大，但覆盖面先天不全。
- ❌ 修注解继承（让 `@SQLRestriction` 从 MappedSuperclass 生效）：需改 Hibernate 核心，或退回逐实体
  重复声明注解——后者正是本次 bug 的遮挡源（见下），随新聚合接入而漂移，违背「全局生效、零逐实体配置」。

**实施备注**：
- **显式优先**：实体已自行声明 `@SQLRestriction`（`RootClass.getWhere()` 非空）时跳过自动注册，
  不覆盖、不叠加——保留自定义限制表达式的逃生空间。
- **启动期 fail-fast**：contributor 校验**每个** `SoftDeletable` 实体存在 `deleted` 持久化列（含已显式
  声明 restriction 的实体），缺失即在元模型构建期抛 `MappingException`（→ 上下文启动失败，错误消息
  指明实体类），而非运行期才因自动过滤的 SQL 找不到列而抛异常。
- **破坏性语义修正**：`findById` 对已删记录返回空。旧文档 AC4「findById 是已删数据的逃生通道」
  作废——该「行为」当年只是 restriction 失效的副产品。查已删数据改走 jOOQ 读侧（cartisan-data-query，
  天然不受 JPA restriction 约束）或原生 SQL。
- **鸭子类型**：仅有 `markAsDeleted()` 方法但未实现 `SoftDeletable` 接口的实体，`BaseRepositoryImpl`
  反射软删保留（写侧），但**不**获得读过滤；javadoc 注明并推荐实现接口。
- **范围**：仅 `cartisan-data-jpa`。写侧软删（`delete` / `deleteById` / `deleteAll` 置 `deleted=true`）
  不变；`cartisan-data-query`（jOOQ 读侧）不动。

**探针实证**（Hibernate 6.6.x，对照实体上 restriction 生效时的真实 SQL）：
- `findById`：`where id=? and (deleted = false)`——按 id 加载**被**过滤（方案 B 的致命缺口正是这里）；
- 派生查询 / 显式 JPQL：`where (deleted = false) and name=?`——均被过滤；
- 原生 SQL：无 `deleted` 片段——不受限（Hibernate 设计如此，也是查已删数据的逃生通道）。

**测试遮挡根因（为何此 bug 长期隐形）**：
1. **注解遮挡**：框架所有测试实体（`TestSoftDeletableEntity` 等）都在**自身类**上重复声明了
   `@SQLRestriction`，恰好让测试走实体自声明而非框架机制——待验证的机制反而从未被验证。
2. **L1 缓存假通过**：两个 findById-still-found 测试在 `@Transactional` 内，persistence context
   直接返回了刚软删的实体（同一事务、未触达 DB），断言「findById 仍返回」居然绿——把失效当成正确语义固化。

摘遮挡（#7）：`TestSoftDeletableEntity` 移除自身重复注解，基线从此验证框架机制；两个 findById 测试改为
flush + clear persistence context 后断言返回空的新语义；L1 纪律（读过滤断言前先 flush + clear）写入测试类
注释。`TestAggregateRootWithSoftDelete` 保持自声明注解——它未实现 `SoftDeletable` 接口，恰好作
「显式优先 / 鸭子类型不获读过滤」的对照。死注解清理与 javadoc 重写见 #6。

**验收**：
- 继承 `AuditableSoftDeletable`、自身无注解（下游真实用法）：`findById` / `findAll` / Specification /
  派生查询 / 显式 JPQL 均过滤已删记录；`count` 不含已删。
- 直接实现 `SoftDeletable` 接口的实体：同样过滤（`DirectSoftDeletableReadFilterTest`）。
- 自身显式声明 `@SQLRestriction` 的实体：按显式表达式过滤、不被覆盖（`SoftDeletableCustomRestrictionTest`）。
- 非 `SoftDeletable` 实体：读写行为完全不变。
- 写侧 `delete` 族软删行为不变；fail-fast：`SoftDeletable` 实体缺 `deleted` 列时上下文启动失败（`SoftDeletableFailFastTest`）。
- triage 复现/探针文件转正并入基线（`SoftDeleteRestrictionInheritanceTest`、`SoftDeleteRestrictionSemanticsTest`），全绿。
- `mvn test -pl cartisan-data-jpa` 全绿（153 tests）。

**消费方落地**：所有继承 `AuditableSoftDeletable` 的下游聚合零改动获益——升级到新 SNAPSHOT 后，
admin 的 AdminUser / Role / Menu 等列表与详情接口即不再泄漏已删数据。下游升级验证后回归关闭 admin#7 与 #2。

**Out of scope**：关联加载读过滤（懒加载 to-one 关联指向已删记录的解析行为）非框架保证——读过滤经
Hibernate `@SQLRestriction` 等价机制（`RootClass.setWhere`）实现，关联级是否过滤由 Hibernate 自身语义
决定，框架不为关联加载额外兜底或固化其行为（不写探针、不钉死回归结果）。已移入 Out of Scope，见
`.out-of-scope/association-loading-soft-delete.md`（收口来源 #9 / spec #3 的 US12）。

**软删降级为 opt-in（#10, 2026-08-03）**：`Auditable` 成为聚合根默认推荐基类，
`AuditableSoftDeletable` 降级为显式 opt-in（仅业务需要"删除但可恢复"时使用）。机制不变，
文档与规范已同步更新。详见 spec #10。

### Issue 06（#28）— 分页基建统一收口：全链 1-based（2026-09-15）

**来源**：#28。平台拍板终局（2026-09-15，ZhangColin/aieducenter-admin#61 grill 收口）：
全链 1-based（请求 + 回显），唯一换算点收在框架。

**判定**：✅ **是框架问题**。0-based 是 DB offset 语义（`PageRequest.of(page,size).getOffset() == page*size`）
上溯到 wire 的泄漏；各服务每个列表方法手写 `pageable.getPageNumber() + 1` 回显、前端各页复制
`page - 1` 适配、BFF 两侧 `+1`/`-1` 在 wire 上抵消——漏写一处即静默 0-based，框架层无人拦截。
off-by-one 换算必然存在一次，应收在框架一处。与 framework-review #19 同域，**本票不代关**。

**根因 reframe**：不是"缺一个工具类"，而是**分页的 wire 契约从未被框架定义**——请求侧语义
（0/1-based、clamp、默认值）由每个端点各自临时约定，回显侧由每处手写换算维持。定义契约
（一个类型 + 一个工厂），换算与防御才有唯一落点。

**采纳方案**：三件套，全部落 `cartisan-web`（与 `PageResponse` 同居；请求绑定是 web 概念，
jOOQ-only 项目经 data-query 已依赖 web 也能用）：

```java
// 1. 分页请求（含排序——见否决方案 4 的语义论证）
public record Pagination(int page, int size, List<String> sort) {
    // compact ctor clamp（固定契约，不做配置面）：page<1→1；size<1→1；size>100→100
    // 缺省（参数未传）：page=1、size=20（须与显式传 0 区分，见实施备注）
    public PageRequest toPageRequest() { ... }                          // JPA 写侧/通用
    public PageRequest toPageRequest(Set<String> allowedFields) { ... } // 白名单排序
    public long offset() { ... }  // (page-1)*size，jOOQ 读侧直出
    public int limit() { ... }    // == size
}

// 2. 不分页列表的排序（导出全量等：客户端控排序但不分页）
public record Ordering(List<String> sort) {
    public Sort toSort() { ... }
    public Sort toSort(Set<String> allowedFields) { ... }
}
// Pagination 的排序转换内部复用 Ordering——排序转换全框架唯一实现点。
// 组件保持扁平 List<String> sort（不能嵌套 Ordering 组件，见否决方案 5）。

// 3. 回显工厂（PageResponse record 已存在，补工厂集中换算）
PageResponse.of(Page)  // 内部 page = p.getNumber() + 1，替代各服务手写
```

**组合契约（查询端统一形态）**：不设 Query 基类，参数并列——

- 分页端点：`list(XxxQuery query, Pagination pagination)` → `?filter&page=1&size=20&sort=createdAt,desc`
- 不分页+控排序端点：`list(XxxQuery query, Ordering ordering)` → `?filter&sort=...`
- 固定排序端点：只收 `XxxQuery`，排序是 appservice 业务逻辑（`Sort.by(...)` 硬编码）
- appservice：`findAll(query, pagination)` → `repository.findAll(ConditionSpecifications.of(query), pagination.toPageRequest())`
- wire 全扁平：Spring MVC 多个 record 参数各自按组件名绑定，零魔法；现有各仓 `@Condition`
  record 零改动（迁移 = `Pageable` 参数换 `Pagination`）。

**边界语义**：
- 数值越界静默 clamp（aiplatform `BackofficePages` 先例）；缺省 page=1/size=20（SOP DB-002 已钉）。
- 非数值（`page=abc`）：record 绑定失败走 `BindException` → 现有 400 handler（`GlobalExceptionHandler:136`），
  field-error 信封免费，不撞"路径变量类型不匹配→404"防探测惯例。
- 排序白名单外字段 → **400**（fail loud；静默丢弃 = "传 user_name 实际没排序生效"的隐性 bug）。
  JPA 侧 `Sort.by("属性名")` 走 criteria 属性解析、未知属性 Hibernate 抛错，非注入面；白名单主要
  防 jOOQ 侧字符串拼 `DSL.field(name)` 注入。
- 超尾页：空 items + 原样回显请求页码（Spring `Page` 本就保留请求序号）。

**否决方案**：
- ❌ Spring `spring.data.web.pageable.one-indexed-parameters=true`：只改 page 解释、不管 size
  clamp（契约 3 不满足）；全局配置业务方可漏设/误改；`page=0` 在 one-indexed 下变负页。
- ❌ 自定义 one-indexed `Pageable` resolver 替换：签名仍收 `Pageable`，0-based `getPageNumber()`
  仍可被业务拿来算术——"漏写一处"的根因没堵死，只是换了隐身衣。
- ❌ Query 基类 / `PagedQuery` 接口：Java record 不能继承类；接口形态要求每个业务 Query 手写
  page/size 组件 + 各自调 clamp（模板可漏 = bug 模式复活）；且把分页强塞进每个 Query，
  不分页查询（下拉、导出）被迫带页。违背标准 2（组合优于继承）。
- ❌ 排序独立成与 Pagination 平行的第三参数（三参数签名）：分页语义上**依赖**排序——无
  `ORDER BY` 的 `OFFSET/LIMIT` 窗口不稳定，翻页间数据漂移；排序与分页是同一"有序集取窗"
  概念的两半，不是正交维度。Spring `Pageable` 含 sort 同理。不分页场景的**客户端控排序**
  另由 `Ordering` 覆盖（见上），固定排序场景不进 wire。
- ❌ `Pagination` 嵌套 `Ordering` 组件：Spring 嵌套绑定要求 `ordering.sort=` 路径，破坏 wire 扁平。
- ❌ 上限/默认值做成属性配置：clamp 在 record 构造内拿不到环境配置（要么挪出构造破坏
  "record 构造校验不变量"惯例、要么静态 holder）；"统一口径"是本次目的，每 app 各配 = 再分裂。
- ❌ 白名单外排序字段静默丢弃：隐性失效 bug，fail loud 优于 silent degrade。
- ❌ `PageQuery` 命名：Query 词根已被业务过滤条件占用（`AdminUserQuery` 等），分页不是查询是
  呈现参数，词根撞车；`PageRequest` 撞 Spring Data 0-based 同名类（要替换的东西），同名不同义
  是灾难。

**实施备注**：
- **缺省与显式 0 的区分**：record 组件须能区分"未传"与"传 0"，否则"缺省 size=20"会被 clamp
  成 1——用 `Integer` 组件或 Framework 6.1 `@DefaultValue`（Boot 3.4/Framework 6.2 支持），实现时定。
- `cartisan-web` pom **显式声明 `spring-data-commons`**（`Page`/`PageRequest`/`Sort` 类型；Issue 03
  补 spring-tx 同款先例）。
- `cartisan-data-query` package-info 声称"包括分页工具"实为谎言，随 `offset()`/`limit()` 出口兑现。
- `@CartisanMvcTest` 切片对齐（若 record 绑定行为与完整 MVC 有差异）。
- 白名单 400 复用 `BaseCodeMessage.BAD_REQUEST`（无占位符通用文案，Issue 03 先例）。

**验收**：
- `?page=1&size=20&sort=createdAt,desc` 绑定 `Pagination{1,20,[createdAt,desc]}`，
  `toPageRequest()` → `PageRequest.of(0, 20, ...)`。
- clamp：`page=0`/`page=-3`→1；`size=0`→1；`size=1000`→100；参数缺省→page=1、size=20。
- `page=abc` → 400 field-error 信封。
- `toPageRequest(Set)` 白名单外字段 → 400；`Ordering` 独立使用同语义。
- 超尾页 → 空 items + 回显请求页码；`PageResponse.of(Page)` 回显 `getNumber()+1`。
- jOOQ 读侧：`.limit(pagination.limit()).offset(pagination.offset())`。
- 文档三处旧示例替换：使用手册 5.2（`pageable.getOffset()`）、限界上下文规范
  `:820-832`（`getPageNumber()+1`）/`:1137-1140`（`@PageableDefault`）→ 三段式新范式。

**消费方落地**：各服务删手写 `+1`/`-1`，`Pageable` 参数换 `Pagination`，`new PageResponse<>` 换
`PageResponse.of`（平台侧迁移协调票：ZhangColin/aieducenter-architecture#1）；admin-web 删
`page = (params.page ?? 1) - 1` 适配。

**Out of scope**：`@Condition` 的 jOOQ 对称物（Query record → `org.jooq.Condition` 注解驱动构造）
——大活且 TypeSafe 风格与字符串列名注解有张力，需要时单独立 issue 走同样流程；
framework-review #19 的处置另议（本票不代关）。

### Issue 07（#30）— OpenApiClient 二进制下载能力：`download` + `BinaryResponse`（2026-09-15）

**来源**：#30。admin 作 BFF 透传 aiplatform 后台订单源码包 tar.gz
（`application/gzip` + `Content-Disposition` attachment，无 ApiResponse 信封），
阻塞 ZhangColin/aieducenter-admin#64。

**判定**：✅ **是框架问题**。`OpenApiClient` 只有 JSON 反序列化路径
（`BodyHandlers.ofString()` → Jackson），二进制响应无处承载，且三处硬阻塞应用侧无法自救：
① ofString 的 `CharsetDecoder` REPLACE 把非 UTF-8 字节替换为 U+FFFD，**不可逆损坏**
（应用层再做任何 byte[] 反序列化都建立在已损坏字符串上）；② `readBody` 只返回 body，
响应头丢弃，BFF 透传文件名无数据来源；③ 五头签名拼装（`buildHeaders`）全 private，
框架外无法自建合法签名——设计使然（签名收口框架，admin ADR-0007），缺口只能框架补。

**根因 reframe**：不是"缺一个 get 重载"，而是 client 的**响应处理机制只有一种**——
"响应 = JSON 信封、头即弃"被焊死在 readBody。二进制透传需要的是第二种机制
（原始字节 + 头保全），从未被定义。补的是**第二种响应机制**，不是 `get` 的变体。

**采纳方案**：`OpenApiClient` 新增一个 public 方法（纯新增，`get`/`post`/`put` 零改动）：

```java
// com.cartisan.openapi.client
public BinaryResponse download(String url)
// GET；五头签名（空 body digest + query 入签）+ RequestContext 透传头，同 get() 既有机制

public record BinaryResponse(int statusCode, HttpHeaders headers, byte[] body)
// headers 为 JDK java.net.http.HttpHeaders（不可变、大小写不敏感、firstValue()）
```

- **命名 `download`（传输意图轴）而非 `getBinary`（HTTP 动词轴）**：分野是响应机制不是
  动词；`download`/`upload` 未来对称自然——`upload(url, bytes, TypeReference)` 为
  binary 入向、JSON 信封回，不会出现 `postBinary`"binary 指请求还是响应"的别扭。
- **headers 用 JDK `HttpHeaders`**：BFF 透传场景 header 名大小写不定，不敏感查找是刚需；
  client API 收 JDK/Jackson 类型（`TypeReference` 先例）不避讳，零新造类型。
- **`BodyHandlers.ofByteArray()` 全量缓冲**：provider 端本就 `ByteArrayResource` 缓冲出口、
  MB 级文件 heap 无压力；不做流式。
- **≥400 复用 `OpenApiClientException`**：body UTF-8 decode 成 String（provider 错误信封
  本就是 JSON 文本，admin `AiplatformUpstreamException` 翻译路径继续吃 String body）；
  异常类型零改动，调用方 catch 一种。
- **不带 Content-Disposition 解析 helper**：BFF 透传 = raw header 值原样 set 回自己的响应，
  无需解析文件名；真解析（RFC 6266 `filename*`）等出现需求再立（YAGNI，标准 4）。
- **全局 `readSeconds` 超时**，无 per-call 参数（与 `get`/`post`/`put` 一致，不开先例）。

**否决方案**：
- ❌ `getBinary`：按 HTTP 动词命名伪装成 `get` 的变体，动词轴挂不住"响应机制"分野；
  未来 `postBinary` 语义别扭。（grill 中提出 `download`，采纳。）
- ❌ 同期加 `postBinary`/`putBinary`：零消费方，YAGNI。
- ❌ `OpenApiBinaryClientException` 带 byte[] body：调用方 catch 两种、API 面翻倍，
  为"错误体也是二进制"的不存在场景买单。
- ❌ `OpenApiClientException` 加 byte[] 构造重载：getter 语义分裂。
- ❌ `DownloadResponse`/`DownloadedFile` 命名：动作焊死进类型名，第二动作出现即打架；
  类型按载荷形态（BinaryResponse）命名寿命长。
- ❌ `InputStream` 真流式：关闭责任 / timeout 覆盖语义复杂化，provider 端本就缓冲，无消费方。
- ❌ headers 用 `Map<String, List<String>>`（大小写敏感查找自理）或扁平
  `Map<String, String>`（丢多值）：JDK `HttpHeaders` 两者皆免费。
- ❌ 载体加 `filename()` 便捷方法：透传场景不需要解析后的文件名，提前固化解析口径。

**实施备注**：
- 仅 `cartisan-openapi` 模块；纯新增方法，现有 JSON 路径行为零改动。
- binary 侧 ≥400 校验复用 `validateResponse` 语义（byte body → UTF-8 decode 进异常）。
- 测试沿用 `com.sun.net.httpserver.HttpServer` 先例（`OpenApiClientPutTest` /
  `TimeoutTest` / `EmptyBodyTest`），无需新依赖。
- 手册同步：§2.26 补 `download` + `BinaryResponse` 参考；§3.26 补 BFF 透传示例
  （raw header 原样 set 回 + 字节写出，示范"文件名不解析"）。顺带修正同章既有陈旧
  （评审发现的 catch-up，与 #30 无关但同表同节）：§2.26 补 `put` 行（方法存在但漏记）、
  §2.27 `@RequireSignature` 去 permission 属性（Issue 04 决议的文档追补）、
  §3.26 示例 `Xxx.class` → `TypeReference`（原示例与实际签名不符、编译不过）。

**验收**：
- **字节完整性**（ofString 损坏的回归锚点）：server 返回含非 UTF-8 序列的 gzip 字节 +
  `Content-Type: application/gzip` + `Content-Disposition: attachment; filename="...tar.gz"` →
  `body()` 与原字节逐位相等、两头经 `headers()` 可取。
- **签名与上下文**：server 侧捕获请求头，断言五签名头
  （`X-Api-Key`/`X-Timestamp`/`X-Nonce`/`X-Body-Digest`/`X-Sign`）与 RequestContext 透传头在场。
- **≥400 语义**：JSON 错误信封 + 5xx → 抛 `OpenApiClientException`，
  `getStatusCode()` / `getBody()`（UTF-8 decode 后 String）可读。
- **超时 / 网络异常**包装语义与 `get()` 一致（`TimeoutTest` 模式）。
- 手册两节同步；`mvn test -pl cartisan-openapi` 全绿。

**消费方落地**：admin#64 直接 `openApiClient.download(url)` → 把 headers 的
`Content-Type`/`Content-Disposition` 原样 set 回响应 + `body()` 写出；
`AiplatformUpstreamException` 翻译路径零改动。

**Out of scope**：`upload`（binary 入向 POST，出现消费方再立）；Content-Disposition
解析（RFC 6266 `filename*` / 编码）；真流式（`InputStream`）；per-call 超时。

### Issue 08（#31）— Pagination 默认排序出口：`toPageRequest(Sort defaultSort)`（2026-09-16）

**来源**：#31。#29 交付的 `Pagination` 空 sort → `Sort.unsorted()`（无 ORDER BY 的窗口
不稳定，`Pagination` javadoc 自书，也是 sort 并入 Pagination 的理由）。首个迁移消费方
（aieducenter-identity#78）浮现共同形状：端点有既定默认排序契约（如 `createdAt DESC`——
admin BFF 不传 sort 依赖它），迁移后需在 AppService 手写判空回退——三连取值
（`getSort()/getPageNumber()/getPageSize()`）重建 PageRequest，每个有默认排序的端点
复制一份。

**判定**：✅ **是框架问题**。空 sort = 不稳定窗口是分页语义的内在属性，默认排序回退是
分页语义的配套，不是端点业务执念；样板在多端点复制，与 admin#61「唯一换算点收在框架」
的拍板同源。

**根因 reframe**：不是"缺一个重载"，而是**默认排序回退从未有落点**——`Pagination` 定义了
wire 契约与换算出口，但"wire 没传排序时端点该怎么办"没有框架答案，只能各端点手写。

**采纳方案**：`Pagination` 新增单重载（纯新增，既有方法零改动）：

```java
public PageRequest toPageRequest(Sort defaultSort)
// wire 空 sort → 用 defaultSort；wire 传了排序 → 以 wire 为准（defaultSort 不生效）
// defaultSort 是服务端代码（可信，不做白名单校验）；null → requireNonNull fail loud
```

- **端点级默认留在调用点参数**：默认排序是端点级契约（每端点不同），作为方法参数显式
  声明，`Pagination` 保持纯 wire 契约，不知道"搜索端点默认 createdAt DESC"这类业务事实。

**否决方案**：
- ❌ `Pagination` 构造/绑定层声明默认：把端点业务契约渗进 HTTP 绑定语义；且每个端点
  默认不同，wire 层无从收口。
- ❌ 第四重载 `toPageRequest(Set, Sort)`（白名单×默认 2×2 补全）：零消费方（jOOQ 读侧
  走 offset/limit 不消费 PageRequest；JPA 侧白名单本就非必需），YAGNI，出现消费方再立。
- ❌ jOOQ 读侧对称出口（`Ordering` 级默认回退）：#31 消费方是 JPA 写侧端点；读侧出现
  消费方再立。

**验收**：
- `new Pagination(2, 10, null).toPageRequest(DEFAULT)` → `PageRequest.of(1, 10, DEFAULT)`；
  空列表 sort 同。
- wire 传 sort 时返回 wire 排序，DEFAULT 不生效。
- `toPageRequest((Sort) null)` → NPE（fail loud，编程错误非缺省语义）。
- `mvn test -pl cartisan-web` 全绿；手册 §2.39 方法表补新出口行。

**消费方落地**：identity#78 搜索端点删手写判空回退，改 `pagination.toPageRequest(DEFAULT_SORT)`；
admin/payment/app-registry 后续迁移同形适用。
