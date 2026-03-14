# Feature: F01-11 SOP 对齐与文档完善 — 实施计划

## 目标复述

基于 Epic 1 审计反馈，补充 CLAUDE.md、README.md、PIT 脚本和 SKILL.md，确保：
1. Docker 环境依赖说明清晰
2. PIT 变异测试成为可执行的门禁
3. 项目有完整的快速入口文档

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `CLAUDE.md` | 新增「开发环境要求」和「质量门禁」小节 |
| 新建 | `README.md` | 项目快速入口文档 |
| 新建 | `scripts/run-pitest.sh` | PIT 执行脚本 |
| 修改 | `docs/skills/SKILL.md` | 新增 TOOL-004 规则 |
| 新建 | `docs/specs/epic-01-core-and-test/F01-11-sop-alignment/04_test_spec.md` | 归档文档 |

## 原子任务清单

### Step 1: 更新 CLAUDE.md

- **文件**：`CLAUDE.md`
- **内容**：在 `## AI 协作规范` 之后新增两个小节
  - `## 开发环境要求`：Docker 依赖说明、测试分层命令
  - `## 质量门禁`：测试命名引用、PIT 执行说明
- **验证**：文档格式正确、链接可跳转

### Step 2: 创建 README.md

- **文件**：项目根目录 `README.md`
- **内容**：
  1. 项目简介（引用设计文档的描述）
  2. 快速开始（环境要求、克隆、构建、测试）
  3. 项目结构（目录树）
  4. 核心概念（DDD、六边形架构）
  5. 开发指南（链接到 docs/）
  6. 技术栈（表格）
- **验证**：Markdown 语法正确、链接有效

### Step 3: 创建 PIT 执行脚本

- **文件**：`scripts/run-pitest.sh`
- **内容**：
  1. 参数校验（模块名必填）
  2. 执行 `./gradlew :<module>:pitest`
  3. 验证报告目录生成
  4. 输出报告路径和检查建议
- **验证**：
  - `chmod +x scripts/run-pitest.sh`
  - 无参数显示帮助
  - 正确模块执行成功
  - 错误处理正确

### Step 4: 更新 SKILL.md

- **文件**：`docs/skills/SKILL.md`
- **内容**：在 `## 工具配置` 小节下新增 TOOL-004
  - 执行方式（脚本和 Gradle）
  - 验收标准（杀死率、报告位置）
  - 注意事项（耗时、仅 Phase 5 必跑）
- **验证**：格式与现有规则一致

### Step 5: 手动验证

- **检查项**：
  - [ ] CLAUDE.md 新增内容可读
  - [ ] README.md 渲染正确
  - [ ] `./scripts/run-pitest.sh cartisan-core` 执行成功
  - [ ] SKILL.md 新增规则格式一致
  - [ ] 所有文档链接可跳转
- **验证方式**：人工阅读 + 脚本执行

---

## 无代码 Feature 说明

本 Feature 属于「文档 + 脚本」类，不涉及业务代码。按照 SOP 无代码 Feature 的简化处理：
- 02_interface.md：描述文档和脚本的变更范围与验收方式
- 03_implementation.md：步骤清单
- 04_test_spec.md：手动检查清单（不写测试用例）

---

## 完成检查

- [ ] CLAUDE.md 新增内容正确无误
- [ ] README.md 内容完整，格式清晰
- [ ] `scripts/run-pitest.sh` 可执行且输出正确
- [ ] SKILL.md 新增规则格式符合现有规范
- [ ] 文档间引用关系正确
- [ ] 脚本手动执行验证通过
