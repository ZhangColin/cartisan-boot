# 01 — GlobalExceptionHandler 处理数据库完整性冲突（重复键等 → 4xx 而非 500）

Type: task
Status: ready-for-agent
Raised by: aieducenter-app-registry（app_code 全局唯一撞名场景）

## 背景

cartisan-web 的 `GlobalExceptionHandler`（`cartisan-web/src/main/java/com/cartisan/web/exception/GlobalExceptionHandler.java`）覆盖了 `CartisanException`、`ResubmitException`、Bean Validation 系列、常见 MVC 异常，以及一个兜底 `handleException(Exception)`。

但**没有**处理 `org.springframework.dao.DataIntegrityViolationException`（及其子类 `DuplicateKeyException`）。结果：任何数据库唯一/外键/check/not-null 约束冲突，都会一路冒泡到兜底 `handleException(Exception)`，被翻译成 **HTTP 500 `INTERNAL_SERVER_ERROR`**。

## 问题（实证）

aieducenter-app-registry 在设计 `app_code`（全局唯一、不可复用的应用稳定标识）时发现：

- `BaseRepositoryImpl.save()` 不刷盘（字节码证实 = `super.save()`），唯一约束冲突只在事务提交时抛 `DataIntegrityViolationException`。
- 应用层走"先查再存"（`existsByAppCode` + `throw DomainException(DUPLICATE)`）作主路径，能给出**具体字段**的 409。
- 但并发 race（两请求同时查重都过、同时 insert）关不掉——DB 唯一约束才是真保证。当这条 DB 约束触发时，`GlobalExceptionHandler` 把它翻成 **500**，而非 409。

500 是**语义错误**：重复键/约束冲突是**客户端冲突（4xx）**，不是"服务器故障"。500 会：触发监控告警噪音（值班误以为服务故障）、误导调用方、丢失"是哪类约束冲突"的信息。

这是**通用缺口**，不是 app-registry 特有：任何用唯一约束/外键的消费应用都会撞到。

## 根因

框架缺一个"把数据库完整性冲突翻译成正确 4xx"的统一出口。Spring 已经把 JDBC 完整性冲突（SQLSTATE 23xxx）翻译成 `DataIntegrityViolationException` 体系（其中 `DuplicateKeyException` 专门对应唯一/重复键），框架只需接住并映射，不必自己解析 SQLException。

## 候选方案（请框架侧确认 + 择一）

### C1（推荐）：分级映射，复用 Spring 异常分类

- `@ExceptionHandler(DuplicateKeyException.class)` → **409**（重复；复用 `BaseCodeMessage.DUPLICATE`，或新增 `DATA_INTEGRITY_VIOLATION`）。
- （可选）`@ExceptionHandler(DataIntegrityViolationException.class)` → **400/409**（外键/check/not-null 等其余完整性冲突）。
- 优点：精确（重复=409，其余=400）；实现简单（Spring 已分类，不解析 SQLException）；与现有 `BaseCodeMessage.DUPLICATE`（409, "Duplicate resource: {0}"）天然契合。
- Spring MVC 按"最具体匹配"——子类 handler 优先于父类，二者可共存不冲突。

### C2：粗粒度，只接父类

- 只 `@ExceptionHandler(DataIntegrityViolationException.class)` → 统一 409 或 400。
- 优点：最简。缺点：重复键与 FK/check/not-null 混同一状态码。

### C3：不改框架，仅文档化"消费方应先查再存"

- 优点：零改动。缺点：关不掉 race；每个消费应用仍会在并发或漏查重时返 500；治标不治本。

## 影响范围

- `cartisan-web`：`GlobalExceptionHandler` 新增 1~2 个 `@ExceptionHandler`（+ 可能新增一个 `BaseCodeMessage` 常量）。
- `cartisan-web/src/test/java/com/cartisan/web/exception/GlobalExceptionHandlerTest.java`：补用例。
- 所有消费应用受益（唯一约束/外键的 race 与漏查重不再返 500）。
- 向后兼容：消费方"先查再存"的现有主路径不受影响（仍返具体字段消息）；本改动只改善 DB 约束直接触发的兜底路径。

## 验收

- 唯一/重复键冲突 → HTTP **409**（非 500），响应体 `code=409` + 冲突类 message。
- 其它完整性冲突（外键/check/not-null）→ 4xx（400 或 409），非 500。
- 现有消费方行为不变：应用层 `throw DomainException(DUPLICATE)` 主路径仍返具体字段消息、不受影响。
- `GlobalExceptionHandlerTest` 覆盖 `DuplicateKeyException → 409`。

## 来源 / 上下文

- 提出：aieducenter-app-registry 设计 `app_code` 全局唯一撞名时浮现（app-registry `CONTEXT.md` / `docs/adr/0001-application-aggregate-and-identity.md`）。
- 相关代码：`cartisan-web/src/main/java/com/cartisan/web/exception/GlobalExceptionHandler.java`、`cartisan-core` 的 `BaseCodeMessage`（已有 `DUPLICATE` = 409）。
- app-registry 侧决策：app_code 撞名走"先查再存 + `DomainException(APP_CODE_DUPLICATE)`"（框架 idiom），DB 唯一约束仅作并发兜底——本框架缺口正是兜底路径的 500 问题。
- Spring 行为：`DataIntegrityViolationException` 是完整性冲突基类，`DuplicateKeyException` 为其子类（唯一/重复键）。

## Comments

**框架侧 triage + 落地（2026-07-28）**：

- **判定**：✅ 是框架问题——通用缺口（任何用唯一约束/外键的消费应用，并发 race / 漏查重时都会撞 500；500 是语义错误）。详见 `CONTEXT.md` Issue 03。
- **采纳**：C1 精确分级——`DuplicateKeyException`→409（复用 `BaseCodeMessage.CONFLICT`）；其余 `DataIntegrityViolationException`（外键/check/not-null）→400（复用 `BAD_REQUEST`）。响应体只给通用文案，DB 原始消息 WARN 入日志（防 schema 细节泄漏）。不新增枚举常量。
- **否决**：C3（关不掉 race）/ C2（丢失"重复 vs 其余"区分）/ 复用 `DUPLICATE`（`{0}` 占位符填不出，渲染字面 `{0}`）/ 透出 `ex.getMessage()`（泄漏 DB schema）/ 新增 `DATA_INTEGRITY_VIOLATION` 专用码（YAGNI）。
- **改动**：`GlobalExceptionHandler` +2 `@ExceptionHandler`；`cartisan-web/pom.xml` 显式声明 `spring-tx`（`org.springframework.dao.*` 所在 jar，原本仅经 redis 传递性可见）；`GlobalExceptionHandlerTest` +2 用例（AC10/AC11）；`TestController` +2 端点。
- **验证**：`mvn test -pl cartisan-web` → **136 tests, 0 failures**。
- **消费方落地**：app-registry 等无需改动；DB 唯一约束兜底路径从 500 自动变 409，"先查再存"主路径（具体字段消息）不受影响。
