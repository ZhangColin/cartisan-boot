# Feature: F01-11 SOP 对齐与文档完善 — 接口契约

## 文档接口定义

### 1. CLAUDE.md 扩展

**位置**：项目根目录 `CLAUDE.md`

**新增内容结构**：

```markdown
## 开发环境要求

### Docker 环境（集成测试必需）

本项目的 cartisan-test 模块使用 Testcontainers 进行集成测试，需要本地 Docker 环境。

- **运行单元测试**（无需 Docker）：`./gradlew :cartisan-core:test`
- **运行全部测试**（需要 Docker）：`./gradlew test`
- **Docker 验证**：运行 `docker ps` 确认 Docker 可用

如 Docker 未安装或未启动，cartisan-test 模块的测试会失败（报错：`Could not find a valid Docker environment`）。

## 质量门禁

### 测试命名规范

遵循 `docs/skills/SKILL.md` 中的 **TEST-002** 规则：`given_{条件}_when_{操作}_then_{预期结果}`。

### PIT 变异测试

cartisan-core 模块已配置 PIT（Mutation Testing），Phase 5 审查时必须执行：

```bash
./gradlew :cartisan-core:pitest
```

**验收标准**：变异杀死率 ≥ 70%，报告位于 `build/reports/pitest/index.html`。
```

**插入位置**：在现有 `## AI 协作规范` 小节之后

---

### 2. README.md 文档

**位置**：项目根目录 `README.md`

**内容结构**：

```markdown
# cartisan-boot

> 业务无关的 Java 技术基础框架...

## 快速开始

### 环境要求
- JDK 21+
- Docker（可选，集成测试需要）
- Gradle 9.0+（项目使用 Gradle Wrapper）

### 克隆与构建
...

### 运行测试
...

## 项目结构
...

## 核心概念
...

## 开发指南
...

## 技术栈
...
```

**格式要求**：
- 使用一级标题作为项目名称
- 使用引用块 `>` 作为项目描述
- 使用表格呈现技术栈
- 使用代码块展示命令
- 使用链接引用详细文档

---

### 3. PIT 执行脚本

**位置**：`scripts/run-pitest.sh`

**接口签名**：
```bash
./scripts/run-pitest.sh <module-name>
```

**参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| module-name | String | 是 | 模块名称，如 `cartisan-core` |

**行为**：
1. 检查参数是否存在
2. 执行 `./gradlew :<module>:pitest`
3. 验证报告目录是否生成
4. 输出报告路径和检查建议

**退出码**：
- `0`：成功
- `1`：参数缺失或报告未生成

**输出示例**：
```
🧪 Running PIT mutation testing...
Running: ./gradlew :cartisan-core:pitest
...
✅ PIT report generated: cartisan-core/build/reports/pitest/index.html

Please check:
  - Mutation Threshold: should be ≥ 70%
  - Surviving mutations: review and add tests if needed
```

---

### 4. SKILL.md 扩展

**位置**：`docs/skills/SKILL.md`

**新增规则**：在 `## 工具配置` 小节下新增

```markdown
### 规则 TOOL-004：PIT 变异测试是 Phase 5 必跑门禁

**执行方式**：
```bash
# 方式一：使用脚本（推荐）
./scripts/run-pitest.sh cartisan-core

# 方式二：直接调用 Gradle
./gradlew :cartisan-core:pitest
```

**验收标准**：
- 变异杀死率 ≥ 70%
- 报告位置：`cartisan-core/build/reports/pitest/index.html`
- 存活变异需审查，补充边界测试

**注意**：PIT 较耗时（分钟级），仅在 Phase 5 审查时必跑，编码阶段不需要每次运行。
```

---

## 文档引用关系

```
README.md
    ├──→ docs/cartisan-boot-设计文档.md
    ├──→ docs/sop/AI协作开发SOP.md
    └──→ docs/skills/SKILL.md

CLAUDE.md
    ├──→ docs/sop/AI协作开发SOP.md
    └──→ docs/skills/SKILL.md (TEST-002)
```

---

## 验收方式

### 文档验证

- [ ] Markdown 语法正确
- [ ] 链接可正常跳转
- [ ] 代码块语法高亮正确

### 脚本验证

- [ ] `chmod +x scripts/run-pitest.sh` 可执行
- [ ] 无参数时显示帮助信息
- [ ] 正确模块执行成功
- [ ] 错误模块给出明确错误提示
