# cartisan-boot — AI 协作上下文

## 项目是什么

cartisan-boot 是一个**业务无关的 Java 技术基础框架**，为所有 Spring Boot 项目提供统一的技术能力封装。基于 DDD、六边形架构设计，供 aieducenter-platform 等业务项目复用。

## 技术栈

- Java 21 / Spring Boot 3.4.x / Gradle Kotlin DSL
- 持久化：Spring Data JPA（写）+ jOOQ（读）
- 安全：Sa-Token（通过抽象层封装，可替换）
- 测试：JUnit 5 + AssertJ + Mockito + ArchUnit + Testcontainers

## 关键文档

- `docs/cartisan-boot-设计文档.md` — 项目完整蓝图（模块设计、收纳准则、Epic 拆分）
- `docs/AI协作开发SOP.md` — AI 协作开发规范（Phase 0-5 流程）
- `docs/decisions/DECISIONS.md` — 架构决策记录
- `docs/specs/` — 各 Epic 的规格文档（Phase 1-3 产出）

## 模块结构

| 模块 | 状态 | 说明 |
|------|------|------|
| cartisan-core | 进行中 | DDD 基建，零外部依赖 |
| cartisan-test | 进行中 | ArchUnit + Testcontainers |
| cartisan-web | 待开发 | 统一响应、全局异常 |
| cartisan-security | 待开发 | 认证抽象、多租户上下文 |
| cartisan-data-jpa | 待开发 | JPA 封装 |
| cartisan-data-query | 待开发 | jOOQ 封装 |
| cartisan-event | 待开发 | 领域事件基础设施 |
| cartisan-ai | 待开发 | 大模型调用封装 |
| cartisan-storage | 待开发 | 文件存储封装 |
| cartisan-payment | 待开发 | 支付对接封装 |

## AI 协作规范

所有开发严格遵循 `docs/AI协作开发SOP.md`：
- Phase 0：Epic 分解（只产出文档，不写代码）
- Phase 1-3：需求 → 接口设计 → 实现方案（只产出文档）
- Phase 4：先写测试（红）→ 再写实现（绿）
- Phase 5：Review + ArchUnit + PIT

**任务粒度：单次实现 50-150 行代码。超过则继续拆分。**