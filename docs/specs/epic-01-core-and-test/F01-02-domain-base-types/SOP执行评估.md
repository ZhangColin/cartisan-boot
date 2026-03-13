# F01-02: cartisan-core domain 基础类型 — SOP 执行评估

> 评估日期：2026-03-13  
> 评估依据：`docs/sop/AI协作开发SOP.md`  
> 评估范围：Phase 0～Phase 5 执行情况

---

## 一、总体结论

| 维度         | 结论     | 说明 |
|--------------|----------|------|
| 流程完整度   | ✅ 通过   | Phase 0～4 有明确产出，Phase 5 部分未完成 |
| 文档规范     | ✅ 通过   | 01/02/03 齐全且与代码一致，04 缺失 |
| 质量门禁     | ✅ 通过   | 编译、测试、ArchUnit 均通过 |
| 与 SOP 符合度 | ⚠️ 部分符合 | 见下文分项说明 |

**总体评价：** 第一个 Feature 在「先文档后代码、接口与实现分离、原子任务」上执行得较好；Phase 5 归档与交叉审查、测试命名规范、PIT 等有缺口，适合作为后续 Feature 的改进点。

---

## 二、分 Phase 评估

### Phase 0: Epic 拆解 ✅

- **产出**：`00_epic_backlog.md` 存在，F01-02 有清晰描述、依赖（F01-01）、复杂度和开发顺序。
- **符合度**：Epic 目标、Feature 清单、依赖关系图、推荐开发顺序均齐全。
- **建议**：无。

---

### Phase 1: Research — 需求澄清 ✅

- **产出**：`01_requirement.md` 完整，包含背景、目标、In/Out 范围、用户故事、验收标准、边界与异常、非功能需求。
- **符合度**：
  - AC 可测试：每条 US 有对应验收标准，可映射到测试用例。
  - 覆盖主流程 + 失败/边界：如 `registerEvent(null)`、`sameIdentityAs(null)`、null ID 等均有说明。
  - 范围清晰：明确排除 Auditable/SoftDeletable（归属后续模块），避免范围蔓延。
- **建议**：无。

---

### Phase 2: Design — 接口设计 ✅

- **产出**：`02_interface.md` 存在，包结构、接口定义（含伪代码/表格）、类型关系图、命名约定齐全。
- **符合度**：
  - 仅文档、无源代码：接口以伪代码和表格描述，符合「Phase 1–3 只文档」。
  - 与实现一致：`AggregateRoot`、`AbstractAggregateRoot`、`Entity`、`ValueObject`、`Identity`、`DomainEvent` 的签名与 02 描述一致。
  - 小差异：02 中 `AggregateRoot` 无泛型，实现为 `AggregateRoot<T>`，属于合理扩展，未破坏契约。
- **建议**：若后续严格对齐 Spec，可在 02 中补一句「允许泛型形式 `AggregateRoot<T>`」。

---

### Phase 3: Plan — 实施计划 ✅

- **产出**：`03_implementation.md` 存在，原子任务（TASK-01～15）、依赖关系、推荐执行顺序、测试策略均有。
- **符合度**：
  - 第一步为「包结构 + package-info」：与 SOP「第一步为契约/结构」精神一致；SOP 更强调「第一步：生成接口和 DTO 代码」，本项目将「包 + 接口」拆成多步，也可接受。
  - 测试与实现分步：每个类型有独立 Test Task（如 TASK-08 AggregateRootTest、TASK-10 DomainEventTest），测试与实现是分开的 Step。
  - 单步规模：多数 Task 预估 50～150 行，符合原子化要求。
- **建议**：若严格对照 SOP，可显式写出「Step 1：契约代码化（所有接口+Record）→ Step 2：写测试（红灯）→ Step 3：写实现（绿灯）」，便于后续 Feature 复制。

---

### Phase 4: Execute — TDD 红绿循环 ⚠️

- **产出**：源代码与测试均存在，`./gradlew :cartisan-core:check` 通过。
- **符合度**：
  - **契约代码化**：接口与 02 一致，已做。
  - **测试与实现分离**：03 中按类型拆了「实现 + 对应测试」，实际执行顺序是「先实现某类型，再写该类型测试」，与 SOP 推荐的「先集中写所有测试（红灯），再集中写所有实现（绿灯）」不完全一致。当前做法仍是「有测试、有实现」，且测试覆盖充分。
  - **测试质量**：使用 AssertJ（`assertThat`、`assertThatThrownBy`），无 `assertTrue(true)` 等无意义断言；场景覆盖 01 的 AC（事件注册/清空、null 校验、sameIdentityAs、sameValueAs、eventId/occurredAt 等）。
- **缺口**：
  - **测试命名**：SOP 要求「`should_期望行为_when_前置条件`」，当前为「`givenX_whenY_thenZ`」。两种都是常见 BDD 风格，若团队统一采用 SOP 命名，建议后续 Feature 改为 `should_*_when_*`。
- **建议**：下一 Feature 可尝试「先写全量测试（红灯）→ 再写实现（绿灯）」以严格贴合 SOP；若保持当前「按类型先实现再测」，建议在 03 中明确写清并保持一致。

---

### Phase 5: Review — 审查与归档 ⚠️

- **产出**：
  - 全量验证：`./gradlew :cartisan-core:check` 已通过（编译 + 测试 + ArchUnit）。
  - 归档文档：**缺少 `04_test_spec.md`**（测试策略与用例清单的归档）。
  - 决策与经验：`docs/decisions/DECISIONS.md` 存在，但无 F01-02 相关条目；`docs/skills/` 目录不存在，无 `SKILL.md`。
- **符合度**：
  - 交叉审查：SOP 要求「换模型审查」。当前未看到单独审查记录；若用 glm 编码、未用其他模型做一次 Review，则此项未满足。
  - PIT 变异测试：SOP 建议 Phase 5 运行 `./gradlew pitest`。项目中未配置 PIT 插件，无法执行；可视为「未做」或「后续补工具」。
  - 变异杀死率 ≥ 70%：因未跑 PIT，无法评估。
- **建议**：
  1. **补 04_test_spec.md**：从 01 的 AC 和现有测试类整理「测试策略 + 用例清单」，便于后续维护与交接。
  2. **可选**：在 `DECISIONS.md` 中增加一条「F01-02：Auditable/SoftDeletable 划出本 Feature，归属后续 JPA 相关 Epic」。
  3. **建立 docs/skills**：创建 `docs/skills/SKILL.md`，本 Feature 若无新增规则可先留空或写「暂无」；后续有踩坑再追加。
  4. **交叉审查**：本 Feature 或下一个 Feature 起，用另一模型/会话做一次「基于 Spec 的代码审查」，并记录结论。
  5. **PIT（可选）**：若希望满足 SOP 的 Phase 5 门禁，可在根 `build.gradle.kts` 或 cartisan-core 中引入 PIT 插件并跑一次，目标杀死率 ≥ 70%。

---

## 三、质量门禁检查

| 检查项           | 结果 | 说明 |
|------------------|------|------|
| 编译通过         | ✅   | `./gradlew :cartisan-core:compileJava` 通过 |
| 测试通过         | ✅   | `./gradlew :cartisan-core:test` 通过 |
| ArchUnit 通过    | ✅   | domain 不依赖第三方/Spring、仅依赖 JDK 等规则通过 |
| 01/02/03 与代码一致 | ✅ | 实现与 01 范围、02 接口、03 任务划分一致 |
| 04 归档          | ❌   | 缺少 04_test_spec.md |
| DECISIONS 更新   | ⚠️  | 无 F01-02 条目（可选） |
| SKILL 更新       | ⚠️  | 无 docs/skills（可建目录并留空） |
| PIT 变异测试     | ⚠️  | 未配置，未执行 |
| 交叉审查         | ⚠️  | 未看到换模型审查记录 |

---

## 四、与 Epic Backlog 的对应关系

Epic 00_epic_backlog 中 F01-02 的验收标准与实现对照：

| Epic 验收项 | 实现情况 |
|-------------|----------|
| AggregateRoot + AbstractAggregateRoot | ✅ 已实现 |
| Entity\<T, ID\> 含 getId、sameIdentityAs | ✅ 已实现，且 sameIdentityAs 含 null 与跨类型防护 |
| ValueObject\<T\> 含 sameValueAs | ✅ 已实现 |
| Identity\<T\> 含 value() | ✅ 已实现 |
| DomainEvent 含 eventId、occurredAt、aggregateId | ✅ 已实现 |
| Auditable、SoftDeletable | ✅ 已按 01 排除，归属后续模块 |
| 单元测试覆盖所有公开 API | ✅ 有对应测试类 |
| 零外部依赖 | ✅ ArchUnit 校验通过 |

---

## 五、改进清单（供后续 Feature 参考）

1. **必须**：补写 `04_test_spec.md`，归档本 Feature 的测试策略与用例清单。
2. **推荐**：建立 `docs/skills/SKILL.md`，本 Feature 可先写「暂无」或第一条通用规则。
3. **推荐**：下一个 Feature 起，Phase 5 用另一模型/会话做一次交叉审查，并留简短记录（如「审查结论：通过/待改」）。
4. **可选**：测试方法命名统一为 `should_期望行为_when_前置条件`。
5. **可选**：在项目或模块中配置 PIT，Phase 5 跑变异测试并保证杀死率 ≥ 70%。
6. **可选**：在 DECISIONS 中为「F01-02 范围排除 Auditable/SoftDeletable」补一条简短说明。

---

## 六、小结

F01-02 在「文档先行、接口与实现分离、原子任务、测试覆盖与断言质量」上执行良好，适合作为 SOP 的首次实践样本。主要缺口在 Phase 5：缺少 04 归档、未做换模型交叉审查、未配置 PIT。建议在本 Feature 补全 04 与 skills 目录，从下一 Feature 起落实交叉审查与（若需要）PIT，以便 SOP 在后续迭代中更完整落地。
