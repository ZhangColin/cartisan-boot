# AI 协作开发标准作业流程 (SOP)

> 版本：v0.4 | 日期：2026-03-14
> 定位：本文档是团队 AI 辅助开发的标准规范与操作手册，适用于所有项目成员及 AI 工具。
> 存放位置：项目根目录 `docs/sop/AI协作开发SOP.md`；CLAUDE.md 中引用路径与上述一致，便于 AI 加载。

---

## 目录

- [一、理念与原则](#一理念与原则)
- [二、环境与工具链配置](#二环境与工具链配置)
- [三、模型选择指南](#三模型选择指南)
- [四、文档体系](#四文档体系)
- [五、Phase 0: Epic 拆解](#五phase-0-epic-拆解)
- [六、Feature 开发循环 (Phase 1-5)](#六feature-开发循环-phase-1-5)
- [七、质量守护工具体系](#七质量守护工具体系)
- [八、快速参考卡片](#八快速参考卡片)

---

## 一、理念与原则

### 1.1 核心理念

**文档是唯一事实来源（Single Source of Truth）。**

在 AI 辅助开发中，代码是廉价的、可重生成的。真正有价值的是：需求意图、设计决策、接口契约、验收标准。这些必须沉淀为文档，成为人与 AI、AI 与 AI 之间的通信协议。

**Phase 1-3 只产出文档，不产出源代码。** 所有代码（包括接口定义、DTO）都在 Phase 4 的执行阶段生成。设计阶段的接口描述以文档形式（伪代码、签名描述、字段表格）存在，确保设计与实现的关注点分离。

### 1.2 五条铁律

| # | 铁律 | 理由 |
|---|------|------|
| 1 | **先文档，后代码** | 文档锁定上下文，防止 AI 在长对话中漂移 |
| 2 | **先测试，后实现** | 测试基于 Spec 编写，实现基于测试驱动，形成交叉验证，防止 AI 伪造测试。在「按类型分步」实施时，允许某类型的实现与该类型的测试成对推进，但同一类型内仍应先写测试再写实现；全量「先全红灯再全绿灯」为推荐形态 |
| 3 | **原子化任务** | 交给 AI 的每个任务控制在 50-150 行代码（行数指单次变更规模，可含测试代码）；略超 150 行可拆子步或在 03 中说明原因，避免机械执行 |
| 4 | **编译器是第一道防线** | 利用 Java 强类型、接口契约、注解校验，让编译器替你检查 AI 产出 |
| 5 | **换模型交叉审查** | 写代码和审查代码使用不同模型或不同会话，避免自我确认偏差 |

### 1.3 角色分工

| 角色 | 职责 |
|------|------|
| **人类（你）** | 需求决策、架构设计、验收 Sign-off、最终 Review |
| **AI（编码）** | 文档生成、实施计划拆解、测试编写、代码实现 |
| **AI（审查）** | 基于 Spec 审查代码变更、发现潜在缺陷 |
| **编译器/工具链** | 类型检查、架构规则守护（ArchUnit）、变异测试（PIT）、自动化门禁 |

---

## 二、环境与工具链配置

### 2.1 开发环境概览

```
┌─────────────────────────────────────────────────┐
│ IntelliJ IDEA                                    │
│  · 代码阅读、手动修改、重构                        │
│  · 编译、运行测试、调试                            │
│  · Git 操作、代码审查                              │
└─────────────────────┬───────────────────────────┘
                      │ 同一个项目目录
┌─────────────────────▼───────────────────────────┐
│ Claude Code / OpenCode (CLI)                     │
│  · AI 辅助编码（写测试、写实现）                    │
│  · 文档生成（Spec、决策日志）                       │
│  · 代码审查辅助                                    │
└─────────────────────────────────────────────────┘
```

两个工具操作同一个项目目录，IDEA 实时感知文件变更。典型工作方式：在 Claude Code 中让 AI 生成代码 → 切到 IDEA 中阅读、编译、运行测试、手动微调 → 回到 Claude Code 继续下一个任务。

### 2.2 Claude Code 项目初始化

#### 第一步：安装

```bash
npm install -g @anthropic-ai/claude-code
cd /path/to/your-project
claude
```

#### 第二步：创建 CLAUDE.md

在项目根目录创建 `CLAUDE.md`，这是 Claude Code 每次启动时自动加载的上下文文件。它是项目的"宪法"——精简、稳定、核心约束。

**CLAUDE.md 模板：**

```markdown
# 项目概览

{项目名称}是一个{简要描述}。

## 技术栈

- Java 21 + Spring Boot 3.4 + Spring Modulith
- Gradle (Kotlin DSL)
- PostgreSQL + Redis
- JPA（写）+ jOOQ（读）
- JUnit 5 + ArchUnit + Testcontainers

## 架构约束

- DDD 六边形架构，领域层不依赖基础设施层
- 只有聚合根可以拥有 Repository
- 限界上下文之间通过领域事件通信，禁止直接跨上下文调用
- 所有金额/虚拟币使用 BigDecimal / long，禁止浮点数
- API 统一返回 ApiResponse<T> 格式

## 编码规范

- 使用 Java Record 定义 DTO（Command、Query、Response）
- 构造函数校验不变量，只暴露允许修改的字段的 setter
- 测试方法命名：以项目 docs/skills/SKILL.md 为准（如 given_*_when_*_then_* 或 should_*_when_*，命名一致、可读、可映射到 AC）
- 测试使用 AssertJ 断言，禁止无意义断言

## 常用命令

- 编译：./gradlew compileJava
- 测试：./gradlew test
- 全量检查：./gradlew check
- 变异测试：./gradlew pitest

## 开发流程

本项目遵循 AI 协作开发 SOP（docs/sop/AI协作开发SOP.md）。
开发前请阅读 docs/skills/SKILL.md 获取历史踩坑经验。
```

#### 第三步：配置 Superpowers

确认 Superpowers 技能已安装。关键技能及触发时机：

| 技能 | 触发时机 | 作用 |
|------|---------|------|
| `brainstorming` | 开始任何 Feature 之前 | 探索需求意图、明确边界 |
| `writing-plans` | Phase 3 制定实施计划 | 拆解原子任务 |
| `test-driven-development` | Phase 4 每个原子任务 | 先红灯后绿灯 |
| `executing-plans` | Phase 4 按计划逐步执行 | 确保不跳步 |
| `systematic-debugging` | 遇到 Bug 或测试失败 | 系统化排查 |
| `requesting-code-review` | Phase 5 审查 | 结构化代码审查 |
| `verification-before-completion` | 声称完成前 | 强制运行验证 |

#### 第四步：创建项目文档目录

```bash
mkdir -p docs/sop          # 本 SOP 文档
mkdir -p docs/specs        # Epic/Feature 的 Spec 文档
mkdir -p docs/decisions    # 设计决策日志
mkdir -p docs/skills       # 团队规则库
mkdir -p docs/guide        # 归档后的使用手册（冷文档）
```

### 2.3 完整的项目文档目录结构

```
project-root/
├── CLAUDE.md                            # AI 上下文——"宪法"（精简、稳定）
├── docs/
│   ├── sop/
│   │   └── AI协作开发SOP.md             # 本文档
│   ├── specs/                           # 热文档——开发期使用
│   │   └── epic-xxx-{名称}/            # 每个 Epic 一个目录
│   │       ├── 00_epic_backlog.md      # Feature 清单 + 依赖关系（推荐数字前缀，或 epic-backlog.md）
│   │       ├── F01-01-{名称}/          # 每个 Feature 一个子目录，示例：feature-001-xxx 或 F01-01-xxx
│   │       │   ├── 01_requirement.md
│   │       │   ├── 02_interface.md
│   │       │   ├── 03_implementation.md
│   │       │   └── 04_test_spec.md
│   │       └── F01-02-{名称}/
│   │           └── ...
│   ├── decisions/
│   │   └── DECISIONS.md                 # 设计决策日志（为什么这么做）
│   ├── skills/
│   │   └── SKILL.md                     # 团队规则库——"判例法"（持续积累）
│   └── guide/                           # 冷文档——归档后的使用手册
│       └── README.md                    # 模块功能清单 + 使用方式
└── src/
```

**热文档与冷文档的关系：**

| | 热文档（docs/specs/） | 冷文档（docs/guide/） |
|---|---|---|
| 定位 | 开发过程的工作台 | 归档后的使用手册 |
| 粒度 | 细——每个 Feature 四份 Spec | 粗——模块级的功能说明 + 使用示例 |
| 生命周期 | Feature 开发期活跃，完成后冷却 | Feature 合并后生成/更新 |
| 读者 | 当前开发者 + AI | 未来使用者 + AI（如使用 cartisan-boot 的业务项目） |
| 维护 | 开发期实时更新 | 每个 Epic 完成后整理一次 |

**归档流程：** 当一个 Epic 的所有 Feature 完成上线后，将关键信息浓缩进 `docs/guides/`。归档时可让 AI 辅助：

```
请阅读以下 Feature Spec 目录中的所有文档，将其精简为一份使用手册：
1. 本模块提供了哪些能力（功能清单）
2. 核心概念和 API（接口清单 + 简要说明）
3. 使用示例（关键场景的代码片段）
4. 注意事项（从 SKILL.md 中提取相关条目）

Spec 目录：docs/specs/epic-xxx/
```

---

## 三、模型选择指南

模型推荐不按阶段固定，而是按**任务特征**匹配。同一个 Phase 内可能混用不同模型。

### 3.1 按任务特征选择模型

| 任务特征 | 推荐模型 | 理由 |
|---------|---------|------|
| **架构设计、方案权衡、复杂推理** | Claude Opus / GPT-4o / Gemini 2.5 Pro | 需要深度推理，不要省 token |
| **生成 Spec 文档、需求分析** | Claude Sonnet 4 / GPT-4o | 结构化表达能力强 |
| **编写测试代码** | Claude Sonnet 4 | Java 代码质量好 |
| **编写实现代码** | Claude Sonnet 4 | Claude Code 主力模型 |
| **交叉审查（Review）** | **必须不同于编码模型** | 打破自我确认偏差 |
| **模板代码、CRUD、简单修改** | Claude Haiku / GPT-4o-mini | 速度快、成本低 |
| **文档格式化、翻译、润色** | Claude Haiku / GPT-4o-mini | 不需要深度推理 |

### 3.2 交叉审查的模型搭配

| 编码阶段使用 | 审查阶段推荐 | 工具 |
|------------|------------|------|
| Claude Code (Sonnet 4) | GPT-4o | OpenCode 或独立对话 |
| Claude Code (Sonnet 4) | Gemini 2.5 Pro | 独立对话或 API |
| OpenCode (GPT-4o) | Claude Sonnet 4 | Claude Code 或独立对话 |

核心原则：**写和审不能是同一个模型**。就像不能让同一个人既写代码又审代码。

### 3.3 何时用最强模型，何时用快速模型

```
成本高但值得：                    成本低即可：
─────────────                    ──────────
· Epic 拆解（一次决策影响全局）     · Record/DTO 生成（模板化）
· 架构方案选择                     · 简单 CRUD 实现
· 接口契约设计                     · 文档格式化
· 复杂业务逻辑实现                 · import 整理、命名调整
· 交叉审查                        · Git commit message 生成
```

---

## 四、文档体系

### 4.1 文档类型总览

| 文档 | 位置 | 定位 | 写入时机 | 写入方式 |
|------|------|------|---------|---------|
| **CLAUDE.md** | 项目根目录 | 项目"宪法"——核心约束与规范 | 项目初始化时创建，重大变更时更新 | 人类维护 |
| **SKILL.md** | docs/skills/ | "判例法"——踩坑经验持续积累 | 每次踩坑后追加 | Phase 5 归档时 AI 辅助生成，人类审核 |
| **DECISIONS.md** | docs/decisions/ | 设计决策审计——为什么这么做（与 ADR 格式兼容，可采用 ADR-001 等编号） | 每次做出技术决策时追加 | Phase 2/5 中 AI 辅助生成，人类审核 |
| **00_epic_backlog.md** / **epic-backlog.md** | docs/specs/epic-xxx/ | Feature 清单与依赖关系 | Phase 0 产出 | AI 辅助生成，人类审核 |
| **01_requirement.md** | docs/specs/.../feature-xxx/ | 需求意图——做什么、怎样算完成 | Phase 1 产出 | AI 起草，人类 Sign-off |
| **02_interface.md** | docs/specs/.../feature-xxx/ | 接口契约——数据结构、错误码 | Phase 2 产出 | AI 起草，人类 Sign-off |
| **03_implementation.md** | docs/specs/.../feature-xxx/ | 实施计划——原子任务清单 | Phase 3 产出 | AI 起草，人类 Sign-off |
| **04_test_spec.md** | docs/specs/.../feature-xxx/ | 测试策略与用例清单；Phase 5 与 01/02/03 一起作为 Feature 完成时的文档集，便于交接 | Phase 5 归档 | AI 归档 |
| **guide/README.md** | docs/guide/ | 模块使用手册——冷文档 | Epic 完成后归档 | AI 浓缩，人类审核 |

### 4.2 DECISIONS.md 详解

**定位：** 记录"为什么这么做"的技术决策，不是"做了什么"（那是 git log）。

**写入时机：**
- Phase 2 中做出技术方案选择时（为什么选方案 A 而不是方案 B）
- Phase 4 中发现 Spec 需要调整时（为什么要改 Spec，改了什么）
- Phase 5 审查中发现重要取舍时

**格式：**

```markdown
## 2026-03-15 Epic: AI 网关 / Feature: 模型路由

### 决策
采用策略模式实现路由，而非 if-else 链或规则引擎。

### 背景
考虑了三种方案：
1. if-else 链：简单但每加一种策略要改路由核心代码
2. 规则引擎（如 Drools）：灵活但引入重依赖，团队无经验
3. 策略模式 + Spring SPI：新增策略只需加一个类，零侵入

### 理由
当前只有 3 种路由策略，规则引擎过重。策略模式在 Java 中是标准模式，AI 理解度高，
且新增策略只需实现接口、加 @Component 注解，对现有代码零修改。

### 风险
如果策略数量超过 10 种且有复杂组合需求，可能需要升级为规则引擎。
```

**Spec 变更的记录方式：**

```markdown
## 2026-03-18 Epic: AI 网关 / Feature: SSE 流式响应

### Spec 变更
原 02_interface.md 中 SSE 事件格式从自定义 JSON 改为 OpenAI 兼容格式。

### 原因
实现过程中发现多数前端 SDK（Vercel AI SDK）默认解析 OpenAI 格式，
自定义格式会导致前端需要额外适配层。兼容 OpenAI 格式可直接复用生态。

### 影响
- 修改了 02_interface.md 的 SSE 事件定义
- 前端对接零额外成本
- 不影响已有功能
```

**实现阶段对 02_interface.md 的任何修改都应在 DECISIONS 中留一条「Spec 变更」记录，便于审计。**

**区分 Spec 变更与新需求：**

| 情况 | 处理方式 |
|------|---------|
| 开发过程中发现 Spec 有遗漏/错误 | 更新原 Spec + 记录到 DECISIONS.md |
| 已上线功能需要修改行为 | 新建 Feature，走完整 Phase 1-5 |
| 已上线功能需要新增能力 | 新建 Feature（可能归属已有 Epic 或新 Epic） |
| Bug 修复 | LAFR 故障排查 → 修复 → 记录到 SKILL.md |

### 4.3 SKILL.md 详解

**定位：** 团队的"经验手册"——记录所有值得沉淀的踩坑经验和铁律。AI 在开发前会读取它，避免重复犯错。

**格式：**

```markdown
# 团队规则库 (SKILL)

## 数据库
- 规则 DB-001：所有金额字段使用 DECIMAL(19,4)，Java 中使用 BigDecimal，禁止浮点数
- 规则 DB-002：所有列表查询必须有分页，默认 page=1, size=20, 上限 size=100
- 规则 DB-003：软删除字段命名为 deleted (boolean)，使用 @SQLRestriction 过滤

## API
- 规则 API-001：所有 API 响应使用 ApiResponse<T> 包装，禁止直接返回裸对象
- 规则 API-002：错误码使用枚举定义，禁止硬编码数字

## 并发
- 规则 CC-001：余额扣减使用 Redis DECRBY 原子操作，禁止 GET → 判断 → SET
- 规则 CC-002：幂等性通过数据库唯一约束保证，不依赖应用层检查

## 踩坑记录
- PIT-001 (2026-03-20)：jOOQ 代码生成依赖 Flyway 迁移先执行，
  CI 中 generateJooq task 必须 dependsOn flywayMigrate
- PIT-002 (2026-03-22)：Testcontainers 的 PostgreSQL 容器版本要与
  生产一致（postgres:16），否则 JSONB 行为可能不同
```

**无代码 / 纯配置 Feature：** 仅涉及构建、配置或脚本、无业务代码的 Feature（如 Gradle 骨架、BOM 配置），可简化：02 仅描述变更范围与验收方式，03 为步骤清单，04 可为验收方式与手动检查清单，不必强行写「测试用例」。

### 4.4 各 Spec 文档模板

#### 01_requirement.md

```markdown
# Feature: {Feature 名称}

## 背景
{一段话说明为什么要做}

## 目标
- {目标 1}
- {目标 2}

## 范围
### 包含（In Scope）
- {要做的事}

### 不包含（Out of Scope）
- {不做的事，及原因}

## 验收标准（Acceptance Criteria）
- AC1: {可验证的标准}
- AC2: {主流程断言}
- AC3: {失败场景}
- AC4: {边界场景}

## 约束
- 性能：{如 P95 < 200ms}
- 安全：{如 需要鉴权}
```

#### 02_interface.md

```markdown
# Feature: {Feature 名称} — 接口契约

## 接口定义

### POST /api/v1/{resource}
**描述：** {一句话}
**鉴权：** Bearer Token

**Request:**
| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|---------|------|

**Response (成功):**
| 字段 | 类型 | 说明 |
|------|------|------|

**错误码：**
| 错误码 | HTTP Status | 触发条件 |
|--------|-----------|---------|

**示例（成功/失败各一个）：**

## 领域接口描述（伪代码，非源代码）

服务接口：XxxService
- 方法：doSomething(name: String, amount: int) → XxxResult
- 前置条件：name 非空，amount > 0
- 后置条件：持久化成功，返回生成的 ID
- 异常：DUPLICATE_NAME(409)，INVALID_AMOUNT(400)

数据结构：
- XxxCommand { name: String(1-128), amount: int(>0) }
- XxxResult { id: long, createdAt: datetime }
```

**注意：Phase 2 的接口描述使用伪代码和表格，不是 Java 源代码。Java 代码在 Phase 4 第一步生成。**

#### 03_implementation.md

```markdown
# Feature: {Feature 名称} — 实施计划

## 目标复述
{3-5 行对齐 01_requirement.md}

## 变更范围
| 操作 | 文件路径 | 说明 |
|------|---------|------|

## 核心流程（伪代码）
1. ...
2. ...

## 原子任务清单

### Step 1: 生成接口和 DTO 代码
- 文件：{路径}
- 内容：将 02_interface.md 中的接口描述转为 Java 接口 + Record
- 验证：编译通过

### Step 2: 编写单元测试（红灯）
- 文件：{路径}
- 内容：基于 AC 编写测试，此时实现不存在，测试应全红
- 验证：编译通过 + 测试全红

### Step 3: 编写实现（绿灯）
- 文件：{路径}
- 内容：实现接口使测试全绿
- 验证：编译通过 + 测试全绿 + ArchUnit 通过

### Step 4: ...
```

---

## 五、Phase 0: Epic 拆解

> **目标：** 在高视角上分析需求，拆解为独立的 Feature，识别依赖关系。
> **不做：** 不侵入 Task 级别、不讨论具体实现类。Task 拆解交给 Phase 3。
> **推荐模型：** 架构设计/方案权衡类模型（Claude Opus, GPT-4o, Gemini 2.5 Pro）

### 5.1 适用时机

当需求无法在单个 Feature 中完成时，进入 Epic 拆解。判断标准：
- 涉及多个限界上下文
- 预估开发周期超过 3 天
- 需要多个 API 端点协同

### 5.2 步骤

#### Step 1：描述 Epic

用自然语言描述完整需求。包括业务背景、目标用户、期望效果。不需要技术细节。

#### Step 2：AI 辅助 Feature 拆解

```
请以 DDD 架构师的视角分析以下需求：

1. 识别涉及的限界上下文
2. 拆解为独立的 Feature（每个 Feature 对应一个上下文中的一个完整用例）
3. 标注 Feature 之间的依赖关系
4. 评估复杂度（S/M/L）
5. 给出推荐的开发顺序

需求描述：
{Epic 描述}

架构背景：
{CLAUDE.md 架构约束 或 已有模块说明}
```

#### Step 3：人类审核

- [ ] Feature 是否足够独立？
- [ ] 粒度是否合适？（1-3 天可完成）
- [ ] 依赖关系是否正确？有无循环依赖？
- [ ] 是否遗漏非功能性 Feature？

#### Step 4：产出 Epic Backlog

存放位置：`docs/specs/epic-xxx-{名称}/00_epic_backlog.md`（推荐数字前缀，与 01_requirement 等一致）或 `epic-backlog.md`

```markdown
# Epic: {名称}

## 背景
{为什么要做这个 Epic}

## Feature 清单

### Feature 1: {名称}（复杂度：M，无依赖）
{一段话描述目标和范围}

### Feature 2: {名称}（复杂度：S，依赖 Feature 1）
{一段话描述}

## 依赖关系图
Feature 1 → Feature 2 → Feature 3

## 开发顺序
1. Feature 1
2. Feature 2
3. Feature 3
```

**完成后，每个 Feature 独立进入 Phase 1-5 循环。**

---

## 六、Feature 开发循环 (Phase 1-5)

### Phase 1: Research — 需求澄清

> **目标：** 将 Feature 描述转化为精确的、可验收的需求规格。
> **Superpowers 技能：** `brainstorming`
> **推荐模型：** Claude Sonnet 4 / GPT-4o
> **产出：** `01_requirement.md`

#### 操作步骤

**1. 启动 brainstorming：**

```
我需要开发以下 Feature，请先做需求分析。

Feature 描述：
{从 epic-backlog.md 粘贴}

请执行：
1. 用你的话复述这个 Feature 要解决什么问题
2. 列出边界场景和异常情况
3. 列出你需要我澄清的问题
4. 基于分析起草 01_requirement.md
```

**2. 人类回答澄清问题**

**3. AI 生成 01_requirement.md**

**4. 人类 Sign-off**

#### 验收门禁

- [ ] AC 是否可测试？每条 AC 能对应至少一个测试用例
- [ ] AC 是否覆盖：主流程 + 至少 2 个失败场景 + 至少 1 个边界场景
- [ ] In/Out 是否明确
- [ ] 非功能约束是否写清

---

### Phase 2: Design — 接口设计与技术方案

> **目标：** 将需求转化为技术方案和接口契约文档。
> **注意：** 此阶段只产出文档（伪代码、表格、方案描述），不产出 Java 源代码。
> **推荐模型：** 架构决策用强推理模型，文档生成用 Sonnet 4
> **产出：** `02_interface.md` + `DECISIONS.md` 追加

#### 操作步骤

**1. AI 产出技术方案：**

```
基于以下需求文档，请设计技术方案（纯文档，不写代码）：

1. 需要哪些类/接口，各自的职责
2. 如果是 HTTP 接口：端点、方法、请求/响应结构（字段表格）、错误码
3. 领域接口签名描述（伪代码，包括前置/后置条件、异常）
4. 核心流程（伪代码级别）
5. 数据库变更（如有，用 SQL 或表格描述）
6. 考虑过的备选方案及取舍

需求文档：{01_requirement.md 内容}
架构约束：{CLAUDE.md 约束部分}
```

**2. 审讯式推敲（必问三问）：**
- "除了这个方案，有没有更简单的方式？"
- "最大的风险点是什么？"
- "并发场景下会不会出问题？"

**3. 产出 02_interface.md**

接口描述使用表格和伪代码，例如：

```
服务接口：ModelRouter
- 方法：route(request: CompletionRequest) → ModelProvider
- 策略：COST_FIRST / QUALITY_FIRST / SPECIFIED
- 当指定模型不可用时，按 fallback 列表依次尝试
```

**4. 记录设计决策到 DECISIONS.md**

如果做了"方案 A vs 方案 B"的选择，追加到 `docs/decisions/DECISIONS.md`。

#### 验收门禁

- [ ] 接口描述是否完整（端点/方法/字段/错误码/示例）
- [ ] 是否只有文档、没有源代码
- [ ] 重要的技术决策是否记录到 DECISIONS.md

---

### Phase 3: Plan — 实施计划与任务拆解

> **目标：** 将技术方案拆解为原子任务清单。
> **Superpowers 技能：** `writing-plans`
> **推荐模型：** Claude Sonnet 4
> **产出：** `03_implementation.md`

#### 操作步骤

**1. AI 生成实施计划：**

```
基于以下需求和接口文档，请生成实施计划：

1. 列出所有新增/修改的文件（包路径 + 类名）
2. 每个文件的职责（一句话）
3. 核心流程伪代码
4. 拆解为原子任务（Atomic Steps）

原子任务规则：
- 第一步必须是"将接口描述转为 Java 源代码（接口 + Record + 错误码枚举）"
- 然后是"编写测试（红灯）"
- 然后是"编写实现（绿灯）"
- 每步 50-150 行（指单次变更规模，可含测试代码；略超可拆子步或说明原因）
- 测试和实现必须是独立的 Step

需求：{01_requirement.md}
接口：{02_interface.md}
```

**2. 人类审核**

#### 验收门禁

- [ ] 第一个 Step 是"生成接口/DTO 代码"
- [ ] 测试 Step 和实现 Step 是分开的
- [ ] 每个 Step 在 150 行以内（行数指单次变更规模、可含测试代码；略超可拆子步或在 03 中说明原因）
- [ ] 所有 AC 有对应的测试覆盖

---

### Phase 4: Execute — TDD 红绿循环

> **目标：** 按原子任务清单逐步执行。
> **Superpowers 技能：** `test-driven-development` + `executing-plans`
> **推荐模型：** Claude Sonnet 4（编码）
> **产出：** 源代码 + 测试代码

#### 核心机制：三步走

```
Step A: 契约代码化
   将 02_interface.md 中的伪代码描述转为 Java 接口 + Record + 枚举
   验证：编译通过

Step B: 写测试（红灯）
   基于 AC 和接口契约写测试
   验证：编译通过 + 测试全红

Step C: 写实现（绿灯）
   实现接口使测试全绿
   验证：测试全绿 + ArchUnit 通过
```

**Step B 和 Step C 必须分开执行的原因：**

如果让 AI 同时写测试和实现，它会"对着实现凑测试"——写出永远通过但不验证任何逻辑的伪测试。分离后：测试基于 Spec 写，实现基于测试驱动，两者交叉验证。

**执行形态：** 推荐全量「先写齐所有测试（红灯）→ 再写齐所有实现（绿灯）」；若采用「按类型分步」（某类型的实现 + 该类型的测试成对推进），须在 03_implementation.md 中写清并保持团队统一。

#### 操作步骤

**1. 契约代码化（Step A）：**

```
请将以下接口文档中的接口描述转为 Java 源代码：
1. 服务接口（interface）+ JavaDoc（含前置/后置条件）
2. Command Record（含 Jakarta Validation 注解）
3. Response Record
4. 错误码枚举

接口文档：{02_interface.md 内容}
项目规范：{CLAUDE.md 编码规范}
```

在 IDEA 中编译验证：`./gradlew compileJava`

**2. 写测试（Step B）：**

```
请基于以下接口和验收标准编写单元测试。

规则：
- JUnit 5 + AssertJ
- 命名：以项目 docs/skills/SKILL.md 为准（命名一致、可读、可映射到 AC）
- 覆盖以下场景：{AC 列表}
- 实现类尚未存在，测试应编译通过但全部失败
- 禁止 assertTrue(true)、assertNotNull(result) 等无意义断言
- 每个测试方法只验证一个行为

接口定义：{刚才生成的 Java 接口}
验收标准：{01 中的 AC}
```

验证红灯：
```bash
./gradlew compileTestJava    # 编译通过
./gradlew test               # 有失败的测试
```

- [ ] 编译通过
- [ ] 测试是红灯（全绿说明断言无意义，打回）
- [ ] 每个 AC 有对应测试方法
- [ ] 抽查 2-3 个方法，断言有意义

**3. 写实现（Step C）：**

```
请实现以下接口，使所有测试通过。

规则：
- 严格按接口契约实现，不增加额外功能
- 处理所有测试覆盖的异常场景
- 遵循 CLAUDE.md 编码规范

接口：{Java 接口代码}
测试：{红灯测试代码}
```

验证绿灯：
```bash
./gradlew test     # 全绿
./gradlew check    # ArchUnit 通过
```

**4. 更新进度（在 03_implementation.md 中标记）：**

```
### Step 1: 契约代码化 ✅
### Step 2: 单元测试 ✅
### Step 3: 实现 ✅
### Step 4: Controller 层 ⬜
```

**5. 遇到问题 — LAFR 故障排查：**

| 步骤 | 动作 |
|------|------|
| **L**ocate（定位） | Spec + 代码 + 错误日志一起给 AI |
| **A**nalyze（分析） | 判断是代码错还是 Spec 遗漏 |
| **F**ix（修复） | 代码错→修代码；Spec 错→**先改 Spec 再改代码** |
| **R**ecord（留痕） | 追加到 SKILL.md（防复发）+ DECISIONS.md（如果改了 Spec） |

---

### Phase 5: Review — 审查与归档

> **目标：** 交叉审查、归档文档、沉淀经验。
> **Superpowers 技能：** `requesting-code-review` + `verification-before-completion`
> **推荐模型（审查）：** 必须不同于编码模型
> **产出：** `04_test_spec.md` + DECISIONS.md/SKILL.md 更新

#### 操作步骤

**1. 全量验证：**

```bash
./gradlew check           # 编译 + 测试 + ArchUnit
./gradlew pitest          # 变异测试（已配置 PIT 的模块为 Phase 5 必跑项，见下方「Feature 完成检查」）
```

**2. 交叉审查（换模型）：**

使用不同于编码阶段的模型（如 OpenCode + GPT-4o）。**审查结论应留痕**：在 `04_test_spec.md` 末尾增加「交叉审查」小节，或单独建 `review.md`，记录：审查人/模型、审查范围（Spec + 变更 diff）、结论（通过/待改）、待办（若有）。

```
你是 Senior Java 工程师。请基于 Spec 审查代码变更。

重点：
1. 实现是否符合接口契约
2. 是否遗漏错误处理
3. 是否有并发/资源泄漏问题
4. 测试是否充分

Spec：{01 + 02 内容}
代码变更：{git diff}
```

**3. 归档：**

- 整理 `04_test_spec.md`
- 追加 `DECISIONS.md`（如有新决策）
- 追加 `SKILL.md`（如有新踩坑经验）

让 AI 辅助归档：

```
本 Feature 已完成。请帮我：
1. 整理 04_test_spec.md（测试策略 + 最终用例清单）
2. 检查是否有值得记录到 DECISIONS.md 的技术决策
3. 检查是否有值得追加到 SKILL.md 的经验规则
```

#### Feature 完成检查

- [ ] 所有测试绿灯 + ArchUnit 通过
- [ ] PIT：已配置 PIT 的模块必须执行 `./gradlew pitest`（或对应模块），变异杀死率 ≥ 70% 方可关闭；未配置的模块可标注「不适用」
- [ ] 交叉审查已执行且结论已留痕（04 或 review.md），无高优先级未解决问题
- [ ] 01/02/03/04 文档齐全且与代码一致
- [ ] DECISIONS.md 已更新（如有决策）
- [ ] SKILL.md 已更新（如有新规则）

---

## 七、质量守护工具体系

### 7.1 ArchUnit — 架构规则自动化守护

#### 是什么

ArchUnit 是一个 Java 库，用代码编写架构规则，在每次测试时自动验证。

#### 为什么需要

AI 不理解你的架构约定。它可能让领域层依赖 Spring、让 Controller 直接调用 Repository、把业务逻辑写在 Controller 里。ArchUnit 将"口头约定"变成可执行的检查。

#### 何时编写

**ArchUnit 规则是项目基础设施，在 Boilerplate（cartisan-boot 的 cartisan-test 模块）搭建阶段一次性编写。** 业务项目继承这些规则即可，不需要每个 Feature 单独写。当发现新的架构违规模式时，追加新规则。

#### 如何配置

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}
```

#### 规则示例与解释

```java
@AnalyzeClasses(packages = "com.aieducenter")
public class ArchitectureRulesTest {

    /**
     * 规则：分层依赖方向
     * 
     * 领域层（domain）不能依赖基础设施层（infrastructure）和应用层（application）。
     * 这是 DDD 六边形架构的核心约束——领域模型必须是纯粹的业务逻辑，
     * 不包含任何框架、持久化、网络等技术细节。
     *
     * 如果 AI 在领域类中 import 了 Spring 的 @Autowired 或 JPA 的 @Query，
     * 此规则会自动拦截并报错。
     */
    @ArchTest
    static final ArchRule layered_dependencies = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy("..controller..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Controller").mayOnlyBeAccessedByLayers()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers(
                "Application", "Infrastructure");

    /**
     * 规则：禁止字段注入
     *
     * Spring 的 @Autowired 字段注入隐藏了类的依赖关系，
     * 不利于测试（无法通过构造函数注入 Mock），也不利于 AI 理解依赖结构。
     * 强制使用构造函数注入（Lombok @AllArgsConstructor 或手写构造函数）。
     */
    @ArchTest
    static final ArchRule no_field_injection =
        noFields().should().beAnnotatedWith(
            org.springframework.beans.factory.annotation.Autowired.class)
            .because("使用构造函数注入，禁止 @Autowired 字段注入");

    /**
     * 规则：禁止使用 java.util.Date
     *
     * java.util.Date 是可变的、线程不安全的遗留 API。
     * 统一使用 java.time（LocalDateTime, Instant 等）。
     */
    @ArchTest
    static final ArchRule no_java_util_date =
        noClasses().should().dependOnClassesThat()
            .haveFullyQualifiedName("java.util.Date")
            .because("使用 java.time API");

    /**
     * 规则：Controller 命名规范
     *
     * 确保 AI 生成的 Controller 类遵循团队命名约定。
     */
    @ArchTest
    static final ArchRule controllers_naming =
        classes().that().areAnnotatedWith(
            org.springframework.web.bind.annotation.RestController.class)
            .should().haveSimpleNameEndingWith("Controller");
}
```

#### 运行与反馈

```bash
./gradlew test    # ArchUnit 随 JUnit 一起运行
```

违规报告示例：
```
Architecture Violation: Rule 'no classes that reside in package ..domain..
should depend on classes that reside in package ..infrastructure..'
was violated (1 times):
  Class com.aieducenter.billing.domain.BalanceService
  depends on com.aieducenter.billing.infrastructure.RedisClient
```

---

### 7.2 PIT (Pitest) — 变异测试

#### 是什么

PIT 对代码做微小修改（变异），然后检查测试能否检测到。它测试的是"测试本身的质量"。

#### 为什么需要

AI 可能写出"看起来有测试但实际不验证逻辑"的伪测试。PIT 能发现。

#### 工作原理示例

```
原始代码：                     变异后：
if (balance >= amount) {      if (balance > amount) {    ← >= 改为 >
    deduct(amount);               deduct(amount);
}                             }
```

如果你的测试在两种代码上都通过——说明没有测试 `balance == amount` 的边界。PIT 报告该变异"存活"，意味着测试不充分。

常见变异类型：
- `>=` → `>`, `<` → `<=`（条件边界）
- `==` → `!=`（取反）
- `+` → `-`（数学运算）
- 删除一行代码（移除调用）
- 返回 null 代替正常值

#### 何时运行

**Phase 5 的必跑项。** 已配置 PIT 的模块在 Feature 完成审查时必须运行，杀死率达标（如 ≥ 70%）方可关闭。不需要每次编码都跑——它比较耗时（分钟级）。

#### 如何配置

**注意：** Gradle 8+ 或 9 需使用兼容版本（如 1.19.x），具体见项目 `docs/skills/SKILL.md` 或构建文档，避免直接使用旧版导致构建失败。

```kotlin
// build.gradle.kts
plugins {
    id("info.solidsoft.pitest") version "1.15.0"  // Gradle 9 等需 1.19.x，见 SKILL
}

dependencies {
    testImplementation("org.pitest:pitest-junit5-plugin:1.2.1")
}

pitest {
    targetClasses.set(listOf("com.aieducenter.*"))
    targetTests.set(listOf("com.aieducenter.*"))
    mutationThreshold.set(70)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
}
```

#### 运行与解读

```bash
./gradlew pitest
# 报告：build/reports/pitest/index.html
```

报告解读：
- **变异杀死率 < 70%**：测试严重不足，需要补充
- **70%-85%**：基本合格
- **85%+**：良好
- 重点关注存活变异的位置——这些是测试的盲区

---

### 7.3 Testcontainers — 真实环境集成测试

#### 是什么

在测试运行时自动启动 Docker 容器（PostgreSQL、Redis 等），提供真实中间件环境。

#### 为什么需要

单元测试用 Mock 替代外部依赖，但 Mock 可能掩盖真实问题（SQL 方言差异、Redis 命令行为差异）。Testcontainers 确保集成测试在真实环境运行。

#### 前提

开发机器需要安装 Docker。**运行含 Testcontainers 的测试（如全量 `./gradlew test`）前，需确保 Docker 可用。** 若仅做单元测试，可只运行不依赖容器的模块（如 `./gradlew :cartisan-core:test`）。

#### 如何配置

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
}
```

#### 使用方式

创建一个集成测试基类，所有需要数据库的测试继承它：

```java
@SpringBootTest
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

---

### 7.4 测试分层策略

| 测试层 | 工具 | 验证什么 | 速度 | 运行时机 |
|--------|------|---------|------|---------|
| 单元测试 | JUnit 5 + AssertJ | 单个类的逻辑 | 毫秒 | 每次 `./gradlew test` |
| 架构测试 | ArchUnit | 代码结构合规 | 秒 | 随单元测试一起 |
| 契约测试 | MockMvc | API 请求/响应格式 | 毫秒 | 随单元测试一起 |
| 集成测试 | Testcontainers | 真实中间件交互 | 秒~分钟 | 提交前运行；若项目将集成测试并入 `test`，则提交前即全量 `./gradlew test`，需满足 Docker 前提；若有独立 `integrationTest` 任务则运行该任务 |
| 变异测试 | PIT | 测试自身的质量 | 分钟 | Phase 5 审查时必跑（已配置模块）`./gradlew pitest`，杀死率 ≥ 70% |

---

## 八、快速参考卡片

### 8.1 流程总览

```
Epic（大需求）
  │
  ├──→ Phase 0: 拆解为 Feature + 依赖关系
  │       产出: epic-backlog.md
  │
  │    ┌─── 每个 Feature 循环 ──────────────────┐
  │    │                                         │
  │    │  Phase 1: Research → 01_requirement.md  │  ← brainstorming
  │    │  Phase 2: Design  → 02_interface.md     │  
  │    │  Phase 3: Plan    → 03_implementation.md│  ← writing-plans
  │    │  Phase 4: Execute → 代码 + 测试        │  ← TDD
  │    │  Phase 5: Review  → 04_test + 归档     │  ← code-review
  │    │                                         │
  │    └─────────────────────────────────────────┘
  │
  └──→ 取下一个 Feature，重复
```

### 8.2 Phase 1-3 只文档，Phase 4 才写代码

| Phase | 产出形式 | 包含源代码？ |
|-------|---------|------------|
| Phase 0 | 文档 | 否 |
| Phase 1 | 文档 | 否 |
| Phase 2 | 文档（伪代码 + 表格） | **否** |
| Phase 3 | 文档（实施计划） | 否 |
| Phase 4 | **Java 源代码 + 测试** | **是** |
| Phase 5 | 文档（归档） + 审查报告 | 否 |

### 8.3 出了问题怎么办

| 问题 | 应对 |
|------|------|
| AI 生成的测试全绿 | 断言无意义，打回。检查有无 `assertTrue(true)` |
| AI 减少了测试用例 | 对照 01 的 AC 逐条检查 |
| AI 做了 Spec 之外的事 | 回到 03，重申范围 |
| ArchUnit 失败 | 让 AI 修复，不能关闭规则 |
| PIT 杀死率太低 | 补充边界测试 |
| 代码能跑但看不懂 | 不用。让 AI 用更简单的方式重写 |
| Spec 有遗漏 | **先改 Spec，再改代码。** 实现阶段对 02 的任何修改都应在 DECISIONS 中留一条「Spec 变更」记录 |
| 开发中发现设计要调整 | 更新 02_interface.md + 记录到 DECISIONS.md |
| 已上线功能需要变更 | 新建 Feature，走完整 Phase 1-5 |

---

### 8.4 v0.4 修订说明（2026-03-14）

- **测试命名**：明确以项目 `docs/skills/SKILL.md` 为准，SOP 只做原则性描述。
- **PIT**：Phase 5 门禁必跑、杀死率 ≥ 70%；Gradle 8+/9 版本兼容说明（见 SKILL）。
- **交叉审查**：审查结论须留痕（04 末节或 review.md：审查模型/范围/结论/待办）。
- **Epic/Feature 命名**：推荐 `00_epic_backlog.md`、Feature 目录示例 `F01-01-xxx`。
- **无代码 Feature**：02/03/04 简化写法（变更范围、步骤清单、验收与检查清单）。
- **Testcontainers**：全量 test 与 Docker 依赖说明；集成测试并入 test 时的运行时机。
- **原子任务行数**：50–150 行含测试代码、略超可说明或拆子步。
- **「先测试后实现」**：允许按类型分步，推荐形态仍为全量红灯再绿灯。
- **Spec 变更**：实现阶段对 02 的任何修改均在 DECISIONS 留痕。
- **DECISIONS**：注明与 ADR 格式兼容，可采用 ADR-001 等编号。