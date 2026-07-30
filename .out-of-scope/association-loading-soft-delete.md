# 关联加载读过滤（软删 to-one 关联指向已删记录）非框架保证

**Decision**：框架不为软删聚合的关联级（懒加载 to-one）读过滤提供保证、亦不固化其行为——正式移入 Out of Scope。

## Why this is out of scope

软删读过滤由 Hibernate 的 `@SQLRestriction("deleted = false")` 等价机制实现——
`SoftDeletableRestrictionContributor` 在元模型构建期对实现 `SoftDeletable` 的实体调用
`RootClass.setWhere("deleted = false")`，与在实体类上声明 `@SQLRestriction` 是**同一机制**。
where 片段附加在根实体的持久化映射上，至于该 where 是否作用于**关联级加载**
（如懒加载的 to-one 关联解析到一个 `deleted = true` 的目标记录时，解析为空 / 抛 `EntityNotFoundException` /
仍返回对象），是 **Hibernate 自身的语义**，由其关联加载路径（`EntityPersister` / `Association`
加载策略 / 二级缓存等）决定，而非本框架额外注入或兜底的结果。

本框架**只承诺实体级读路径**过滤已删记录：

- `findById` / `findAll`
- Specification 查询
- 派生查询（如 `findByName`）
- 显式 `@Query` JPQL
- `count` / `existsById` 等派生读方法

这些路径已由 restriction 覆盖并经回归测试固化（见 spec #3 的 Testing Decisions 行为矩阵、
CONTEXT.md Issue 05）。**关联级 to-one 加载**是否被过滤，框架**不额外保证、不固化其行为**——
按 Hibernate 实际语义即得，不做探针、不写回归测试强行钉死某种结果（钉死反而会在 Hibernate
版本升级、关联策略变更时变成脆弱的假契约）。

需要查已删记录（无论直接查还是经关联）的场景，逃生通道不变：走 jOOQ 读侧（cartisan-data-query，
天然不受 JPA restriction 约束）或原生 SQL。

## Prior requests

- #9 —— code-review 后续：US12 关联加载测试缺口，决定移入 Out of Scope（本文档即其落地）。
- #3（US12）—— 软删读过滤 spec 的关联加载 user story 与 Testing Decisions 的「关联加载」条目，
  均已标注为 Out of Scope（见 issue 正文与 CONTEXT.md Issue 05）。
