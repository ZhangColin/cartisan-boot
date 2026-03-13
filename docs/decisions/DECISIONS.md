# 架构决策记录（ADR）

## ADR-001：选用 Gradle Kotlin DSL 而非 Maven

- **日期**：2026-03-11
- **决策**：使用 Gradle Kotlin DSL 管理多模块项目
- **理由**：多模块灵活管理，Version Catalog 统一版本，Kotlin DSL 有类型检查和 IDE 补全
- **替代方案**：Maven（社区更大但多模块配置繁琐）

## ADR-002：Java 21 + Virtual Threads

- **日期**：2026-03-11
- **决策**：使用 Java 21，启用 Virtual Threads
- **理由**：AI 平台 SSE 流式输出场景高并发，Virtual Threads 大幅提升吞吐量；Record、Sealed Classes 提升代码质量
- **替代方案**：Java 17（稳定但缺少新特性）

## ADR-003：认证层选用 Sa-Token，通过抽象层封装

- **日期**：2026-03-11
- **决策**：底层用 Sa-Token，但业务代码只依赖 cartisan-security 的抽象接口
- **理由**：Sa-Token 轻量实用；抽象层确保将来可无痛替换为 Spring Security
- **替代方案**：Spring Security（功能强大但配置复杂）

## ADR-004：cartisan-boot 收纳准则

- **日期**：2026-03-11
- **决策**：技术能力进入框架须满足：① 不需要业务数据库表 ② 不同项目调用方式一致 ③ API 足够稳定
- **理由**：保持框架纯粹性，避免混入业务逻辑

## ADR-005：持久化采用 JPA（写）+ jOOQ（读）

- **日期**：2026-03-11
- **决策**：写侧用 Spring Data JPA，读侧用 jOOQ
- **理由**：JPA 与 DDD 聚合根天然适配；jOOQ 类型安全 SQL，编译期捕获错误，AI 生成代码更可靠
- **替代方案**：MyBatis-Plus（读写都行，但类型安全弱）