# Spec — GlobalExceptionHandler 处理 DB 完整性冲突（重复键→409，其余→400）

## Problem Statement

作为消费 `cartisan-web` 的应用开发者，当数据库唯一 / 外键 / check / not-null 约束被违反时
（典型场景：并发 race 下两个请求同时通过"先查再存"的查重、同时 insert，撞上 DB 唯一约束），
框架返回 **HTTP 500 `INTERNAL_SERVER_ERROR`**。

原因是 `GlobalExceptionHandler` 覆盖了业务异常、Bean Validation、常见 MVC 异常与一个兜底
`handleException(Exception)`，却**没有**接住 `org.springframework.dao.DataIntegrityViolationException`
体系（其中 `DuplicateKeyException` 专门对应唯一 / 重复键）。于是任何 DB 完整性冲突都一路冒泡到兜底，
被翻译成 500。

500 是**语义错误**：约束冲突是**客户端冲突（4xx）**，不是"服务器故障"。它造成三个伤害：

1. **监控噪音**：触发 ERROR 级告警，值班误以为服务故障，真正的 500 被淹没。
2. **误导调用方**：前端 / 下游以为服务出错而重试或报障，实则只需提示"资源已存在 / 数据非法"。
3. **信息丢失**：丢掉"是哪类约束冲突（重复 vs 外键 / check）"的区分，调用方无法据此做差异化处理。

"先查再存"关不掉并发 race——DB 唯一约束才是真保证，而当这条 DB 约束触发时，框架却返 500。
这是**通用缺口**，不是某个应用特有：任何用唯一约束 / 外键的消费应用都会撞到。

## Solution

在 `GlobalExceptionHandler` 新增两个 `@ExceptionHandler`，复用 Spring 已经做好的异常分类：

- `DuplicateKeyException` → **409**（复用 `BaseCodeMessage.CONFLICT`）。
- 其余 `DataIntegrityViolationException`（外键 / check / not-null）→ **400**（复用 `BaseCodeMessage.BAD_REQUEST`）。

Spring MVC 按"最具体匹配"——子类 handler 优先于父类，二者与既有 `Exception` 兜底共存、互不干扰。

**响应体只给通用文案**（`CONFLICT` / `BAD_REQUEST` 的标准 message），DB 原始消息（含 constraint / 列名等
schema 细节）**只 WARN 入日志**供运维排查，不进响应体（避免信息泄漏）。不新增 `BaseCodeMessage` 常量。
框架只接住并映射，不自己解析 `SQLException` / SQLSTATE——Spring 已从 SQLSTATE 23xxx 翻译成上述异常体系。

## User Stories

1. 作为消费应用开发者，我希望唯一 / 重复键冲突返回 409 而不是 500，这样调用方知道是"资源冲突"而非"服务故障"。
2. 作为消费应用开发者，我希望并发 race 撞到 DB 唯一约束时框架自动给 4xx，这样我不必为关不掉的 race 在应用层单独兜底。
3. 作为消费应用开发者，我希望外键 / check / not-null 等其它完整性冲突也返回 4xx，这样客户端能识别"是我提交的数据违反了约束"。
4. 作为消费应用开发者，我希望"先查再存"的主路径不受影响、仍返回具体字段消息，这样我的应用层 `DomainException(DUPLICATE)` 提示不退化。
5. 作为消费应用开发者，我希望响应体不泄漏 DB schema 细节（表名 / 列名 / constraint 名），这样我的 API 不暴露内部结构给调用方或潜在攻击者。
6. 作为运维 / 值班，我希望完整性冲突走 WARN 而非 ERROR，这样监控不被"重复键"这类客户端冲突噪音淹没、真正的服务故障不被埋没。
7. 作为调用方（前端 / 下游服务），我希望冲突类错误有清晰的 4xx 状态码与稳定的 `ApiResponse` 结构，这样我能按状态码做相应处理（提示用户 / 放弃 / 换字段重试）。
8. 作为审计 / 日志消费者，我希望 DB 原始异常消息保留在日志里（含具体约束名），这样排查时能看到是哪条约束、哪个键触发。
9. 作为框架维护者，我希望复用 Spring 已做的异常分类（`DuplicateKeyException` vs `DataIntegrityViolationException`），这样框架不必自己解析 `SQLException` / SQLSTATE、也不耦合具体数据库方言。
10. 作为框架维护者，我希望完整性冲突映射在 web 层的 `GlobalExceptionHandler` 统一出口完成，这样所有消费应用、所有写入路径（JPA / jOOQ / 原生 SQL / 批量）都被覆盖。
11. 作为框架维护者，我希望新增的 handler 与既有 `Exception` 兜底共存、互不干扰，这样非完整性异常的 500 行为完全不变。
12. 作为框架维护者，我希望复用既有的 `BaseCodeMessage.CONFLICT` / `BAD_REQUEST`，不为偶尔场景新增枚举常量（YAGNI）。
13. 作为 app-registry 开发者，我希望 `app_code` 全局唯一约束的并发兜底从 500 自动变 409，这样我不必在框架外再补一层异常翻译。
14. 作为消费应用开发者，我希望此次变更是纯新增 handler、无签名变更，这样升级框架不破坏现有代码。
15. 作为框架维护者，我希望直接引用的 `org.springframework.dao.*` 类型由本模块**显式声明**其依赖，这样不靠传递性、消费方运行期类加载也不会失败。

## Implementation Decisions

- **模块**：仅 `cartisan-web`（`GlobalExceptionHandler`）；连带 `cartisan-web` 的 `pom.xml` 显式声明 `spring-tx`。
- **分级映射**：`DuplicateKeyException` → 409（复用 `BaseCodeMessage.CONFLICT`，message "Resource conflict"）；
  `DataIntegrityViolationException`（父类，承接其余外键 / check / not-null）→ 400（复用 `BaseCodeMessage.BAD_REQUEST`，
  message "Invalid request"）。Spring MVC 最具体匹配——子类 handler 优先于父类，二者与 `Exception` 兜底共存。
- **消息策略**：响应体只给上述通用文案；`ex.getMessage()`（含 constraint / 列名等 schema 细节）**只 WARN 入日志**、
  不进响应体（防信息泄漏）。日志级别与既有"4xx → WARN、5xx → ERROR"策略一致。
- **不新增 `BaseCodeMessage` 常量**：`CONFLICT` / `BAD_REQUEST` 已是无占位符的通用文案，复用即可（YAGNI）。
  不复用 `DUPLICATE`——其 message `"Duplicate resource: {0}"` 的占位符在框架层填不出（不知是哪个业务字段），
  会渲染出字面 `{0}`。
- **依赖**：`DataIntegrityViolationException` / `DuplicateKeyException` 位于 `spring-tx` jar。`cartisan-web` 原本经
  `spring-boot-starter-data-redis` 传递性拿到 `spring-tx`；本模块现直接引用这些类型，故**显式声明 `spring-tx`**
  （非 optional，保证消费方运行期类加载不致 `NoClassDefFoundError`）。版本由 Spring Boot BOM 管理。
- **向后兼容**：纯新增 handler，无签名变更；非完整性异常仍走原 500 兜底，行为不变。
- **为什么在 web 层接、不在 data-jpa 包装成 `CartisanException`**：异常在**事务提交时**才抛
  （`BaseRepositoryImpl.save()` 不刷盘，INSERT 在 commit 时执行），repository 适配器返回后才发生、接不到；
  且 data-jpa 包装覆盖不了 jOOQ / 原生 SQL / 批量 / 级联等其它写入路径，web 的 `@ControllerAdvice` 是唯一能
  横切所有路径的位置；此外包装会把"409 vs 400"的 HTTP 状态语义塞进数据层。详见 `CONTEXT.md` Issue 03。
- **否决的替代方案**（理由见 `CONTEXT.md` Issue 03）：
  - C3 仅文档化"消费方先查再存"——关不掉并发 race。
  - C2 只接父类、统一一个状态码——丢失"重复 vs 其余"的区分。
  - 复用 `DUPLICATE`——占位符 `{0}` 填不出。
  - 透出 `ex.getMessage()` 到响应体——泄漏 DB schema。
  - 新增 `DATA_INTEGRITY_VIOLATION` 专用码——YAGNI。
  - data-jpa 层包装成 `CartisanException`——提交时才抛 + 覆盖不全 + HTTP 语义下沉到数据层。

## Testing Decisions

- **好测试的标准**：只测外部可见行为——"抛某类 DB 完整性异常 → HTTP 状态码与响应体结构"，不测 handler 内部
  调用序列、日志调用等实现细节。
- **主验证 seam（integration）**：沿用 `cartisan-web` 既有的 `GlobalExceptionHandlerTest`（`@SpringBootTest` + MockMvc，
  经由测试控制器端点驱动，走完整 DispatcherServlet + `@ControllerAdvice`）。扩展该测试控制器：新增两个端点分别抛
  `DuplicateKeyException` 与 `DataIntegrityViolationException`。端到端覆盖 **异常 → handler → HTTP 响应**。验证点：
  - `DuplicateKeyException` → HTTP 409、`code=409`、`message="Resource conflict"`、`data` 为空；
  - `DataIntegrityViolationException` → HTTP 400、`code=400`、`message="Invalid request"`、`data` 为空。
- **连带维护**：既有 `GlobalExceptionHandlerTest` 的全部用例统一为 `shouldX_whenY` 命名风格；这是既有 seam 的连带
  维护，**不为本 feature 新开 seam**。
- **不开单元测试 seam**：handler 是薄映射层（异常类型 → 状态码 + 通用消息），integration seam 已充分覆盖外部行为，
  再加单元测试只会重复断言映射表、绑定到实现细节。
- **Prior art**：既有 `GlobalExceptionHandlerTest`（`CartisanException` / Bean Validation / 兜底 500 等异常映射测试），
  同一 seam、同一风格。
- **seam 数量**：沿用既有 integration seam，不开新 seam。

## Out of Scope

- **消费方"先查再存"主路径**：仍由应用层 `throw DomainException(DUPLICATE)` 返回具体字段消息，本 spec 不动。
- **DB 原始消息进响应体**：明确不做（信息泄漏）。
- **新增 `BaseCodeMessage` 专用码**：YAGNI，不做。
- **data-jpa 层包装成 `CartisanException`**：经评估否决（见上 / `CONTEXT.md` Issue 03）。
- **把完整性冲突细分为 422、或区分 insert-FK vs delete-FK**：框架层无法从 raw `DataIntegrityViolationException`
  可靠区分；统一 400 已满足"4xx 非 500"的核心目标。如某应用需要更细区分，应由其应用层在已知语义处自行抛
  `DomainException`。
- **`BaseRepositoryImpl.save()` 是否 flush / 事务提交时刷盘策略**：属 `cartisan-data-jpa` 行为，不在本 web 层 spec。

## Further Notes

- **根因 reframe**：本问题不是"缺一个方便的异常处理方法"，而是 `GlobalExceptionHandler` 缺一个"把 DB 完整性冲突
  翻译成正确 4xx"的统一出口——500 是**语义错误**（约束冲突是客户端冲突，不是服务故障）。完整决策记录见
  `CONTEXT.md` Issue 03。
- **来源**：aieducenter-app-registry 设计 `app_code`（全局唯一、不可复用的应用稳定标识）撞名场景
  （`.scratch/data-integrity-violation-handling/issues/01-data-integrity-violation-handling.md`）。
- **消费方落地**：app-registry 等无需改动；DB 唯一约束兜底路径从 500 自动变 409，"先查再存"主路径（具体字段消息）不受影响。
- **工作原则**：通用缺口在框架统一修，避免每个消费应用重复踩坑。
