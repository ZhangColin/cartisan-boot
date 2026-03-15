# cartisan-boot

> 业务无关的 Java 技术基础框架，为所有 Spring Boot 项目提供统一的技术能力封装。

## 快速开始

### 环境要求

- **JDK**: 21+
- **Docker**: 可选（集成测试需要）
- **Gradle**: 9.0+（项目使用 Gradle Wrapper）

### 克隆与构建

```bash
git clone <repository-url>
cd cartisan-boot
./gradlew build
```

### 运行测试

```bash
# 仅单元测试（无需 Docker）
./gradlew :cartisan-core:test

# 全部测试（需要 Docker）
./gradlew test

# 变异测试（Phase 5 门禁）
./gradlew :cartisan-core:pitest
```

> **注意**：`cartisan-test` 模块使用 Testcontainers 启动真实的 PostgreSQL/Redis 容器进行集成测试，运行前请确保 Docker 已安装并启动。

## 项目结构

```
cartisan-boot/
├── cartisan-core/       # DDD 基建（零外部依赖）
├── cartisan-test/       # 测试工具箱（ArchUnit + Testcontainers）
├── docs/
│   ├── sop/             # AI 协作开发 SOP
│   ├── specs/           # Epic/Feature 规格
│   ├── decisions/       # 架构决策记录
│   └── skills/          # 团队规则库（踩坑经验）
└── CLAUDE.md            # AI 协作上下文
```

## 核心概念

本项目基于 **DDD（领域驱动设计）** 和 **六边形架构**，提供以下能力：

- **领域基础类型**：AggregateRoot、Entity、ValueObject、Identity、DomainEvent
- **架构守护**：ArchUnit 规则自动验证分层约束
- **测试工具**：Testcontainers 基类、API 测试断言、Fixture 工具
- **统一异常**：带错误码的领域异常体系

## 开发指南

- **使用手册**：参见 [docs/guides/cartisan-boot-使用手册.md](docs/guides/cartisan-boot-使用手册.md)
- **架构设计**：参见 [docs/cartisan-boot-设计文档.md](docs/cartisan-boot-设计文档.md)
- **协作规范**：参见 [docs/sop/AI协作开发SOP.md](docs/sop/AI协作开发SOP.md)
- **踩坑经验**：参见 [docs/skills/SKILL.md](docs/skills/SKILL.md)

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.4.x |
| 构建 | Gradle (Kotlin DSL) |
| 持久化 | Spring Data JPA (写) + jOOQ (读) |
| 测试 | JUnit 5 + AssertJ + ArchUnit + Testcontainers |
