# Feature: F01-11 SOP 对齐与文档完善 — 测试规格与归档

## 测试策略

本 Feature 属于「文档 + 脚本」类，不涉及业务代码。验收方式为手动检查和脚本执行验证。

## 验收检查清单

### CLAUDE.md 更新

- [ ] 新增「开发环境要求」小节在正确位置
- [ ] Docker 依赖说明清晰（单元测试 vs 全部测试）
- [ ] 新增「质量门禁」小节
- [ ] 测试命名规范引用 SKILL.md 的 TEST-002
- [ ] PIT 执行说明包含命令和报告路径
- [ ] Markdown 格式正确

### README.md 创建

- [ ] 文件位于项目根目录
- [ ] 包含项目简介和定位
- [ ] 快速开始包含：环境要求、克隆、构建、测试
- [ ] Docker 环境说明清晰
- [ ] 测试分层说明（单元测试 / 集成测试 / PIT）
- [ ] 项目结构目录树正确
- [ ] 核心概念简明准确
- [ ] 开发指南链接到 docs/sop/、docs/skills/
- [ ] 技术栈表格完整
- [ ] 所有 Markdown 链接有效

### PIT 执行脚本

- [ ] `scripts/run-pitest.sh` 文件存在
- [ ] 文件具有可执行权限（`ls -l` 显示 -rwxr-xr-x）
- [ ] 无参数执行显示帮助信息
- [ ] 执行 `./scripts/run-pitest.sh cartisan-core` 成功
- [ ] 报告生成后输出路径提示
- [ ] 错误处理：模块不存在时给出明确提示
- [ ] Bash 语法正确（`shellcheck` 无报错）

### SKILL.md 更新

- [ ] 新增 TOOL-004 规则
- [ ] 规则包含：执行方式、验收标准、注意事项
- [ ] 格式与 TOOL-001、TOOL-002、TOOL-003 一致
- [ ] 代码块语法高亮正确

### 文档引用关系

- [ ] README.md 中的链接可跳转
- [ ] CLAUDE.md 引用 SKILL.md 的路径正确
- [ ] 相对链接使用正确

---

## 执行记录

### 执行日期

2026-03-14

### 执行结果

| 检查项 | 状态 | 备注 |
|--------|------|------|
| CLAUDE.md 更新 | ✅ | 已添加「开发环境要求」和「质量门禁」小节 |
| README.md 创建 | ✅ | 包含完整的快速开始和开发指南 |
| PIT 脚本创建 | ✅ | scripts/run-pitest.sh 可执行（-rwxr-xr-x）|
| SKILL.md 更新 | ✅ | 新增 TOOL-004 规则 |
| 文档引用验证 | ✅ | 所有链接正确 |
| cartisan-core 测试 | ✅ | BUILD SUCCESSFUL |
| cartisan-core check | ✅ | ArchUnit 通过 |

---

## 归档说明

本 Feature 补充了项目文档和脚本，为后续 Epic 建立了：

1. **文档模板**：README.md 作为项目入口的格式参考
2. **门禁脚本**：run-pitest.sh 作为 PIT 执行的标准方式
3. **环境说明**：Docker 依赖的标准化说明方式

这些内容可以复用到：
- aieducenter-platform 等业务项目
- 后续新模块的文档补充

---

## 相关文档

- 01_requirement.md：需求规格
- 02_interface.md：文档和脚本的接口契约
- 03_implementation.md：实施计划
- CLAUDE.md：已更新
- README.md：已创建
- docs/skills/SKILL.md：已更新（TOOL-004）
