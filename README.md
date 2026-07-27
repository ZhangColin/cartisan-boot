# cartisan-boot

> 业务无关的 Java 技术基础框架，为所有 Spring Boot 项目提供统一的技术能力封装。基于 DDD 和六边形架构设计，提供 CQRS 读写分离基础设施。

## 快速开始

### 环境要求

- **JDK**: 21+
- **Docker**: 可选（集成测试需要）
- **Maven**: 3.9+（项目使用 Maven）

### 克隆与构建

```bash
git clone <repository-url>
cd cartisan-boot
mvn package
```

### 运行测试

```bash
# 仅单元测试（无需 Docker）
mvn test -pl cartisan-core

# 全部测试（需要 Docker）
mvn test

# 变异测试（Phase 5 门禁）
mvn org.pitest:pitest-maven:mutationCoverage -pl cartisan-core
```

### 安装到本地仓库

```bash
mvn install
```

> **注意**：`cartisan-test` 模块使用 Testcontainers 启动真实的 PostgreSQL/Redis 容器进行集成测试，运行前请确保 Docker 已安装并启动。

## 项目结构

```
cartisan-boot/
├── cartisan-dependencies/   # 依赖 BOM 管理
├── cartisan-core/           # DDD 基建（stereotype 注解 + 领域抽象）
├── cartisan-test/           # 测试工具箱（ArchUnit + Testcontainers）
├── cartisan-web/            # Web 基础设施（统一响应、异常处理）
├── cartisan-event/          # 应用事件基础设施
├── cartisan-data-jpa/       # JPA 写侧封装（CQRS）
├── cartisan-data-query/     # jOOQ 读侧封装（CQRS）
├── cartisan-security/       # 安全认证（Sa-Token 抽象 + 多租户）
├── docs/
│   ├── guides/              # 使用手册
│   ├── sop/                 # AI 协作开发 SOP
│   ├── specs/               # Epic/Feature 规格
│   ├── decisions/           # 架构决策记录
│   └── skills/              # 团队规则库（踩坑经验）
└── CLAUDE.md                # AI 协作上下文
```

## 核心能力

### DDD 基础设施
- **领域基础类型**：AggregateRoot、Entity、ValueObject、Identity、DomainEvent
- **架构守护**：ArchUnit 规则自动验证分层约束
- **统一异常**：带错误码的领域异常体系
- **断言工具**：Design by Contract 风格的前置/后置条件断言

### CQRS 读写分离
- **写侧**：Spring Data JPA + 事件自动发布 + 审计 + 软删除 + TSID 分布式 ID
- **读侧**：jOOQ DSL 查询 + 分页支持 + 多租户过滤

### Web & 安全
- **统一响应**：`ApiResponse<T>`、`PageResponse<T>` + 全局异常处理
- **认证授权**：Sa-Token 抽象封装 + `@RequireAuth/@RequireRole/@RequirePermission` 注解
- **多租户**：`TenantContext`（Virtual Threads 兼容）+ jOOQ 租户过滤

### 测试工具
- **Testcontainers**：PostgreSQL/Redis 集成测试基类
- **API 测试**：MockMvc 测试基类 + 断言辅助
- **Fixture 工具**：随机数据生成器 + 对象构建器

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
| 构建 | Maven |
| 持久化 | Spring Data JPA (写) + jOOQ (读) |
| 安全 | Sa-Token 1.45.0（可替换抽象层） |
| 测试 | JUnit 5 + AssertJ + ArchUnit + Testcontainers |

## 模块依赖

```
cartisan-web ─────────────────────────────────────────────┐
cartisan-data-jpa ────────────────────────────────────────┤
cartisan-data-query ──────────────────────────────────────┤
cartisan-event ───────────────────────────────────────────┤
cartisan-security ────────────────────────────────────────┤──> 业务项目
cartisan-test ────────────────────────────────────────────┤
                                                              │
                    cartisan-core ◄──────────────────────────┘
```

## 版本

当前版本：v0.4（基于 Epic 01-04）

**已完成模块**：
- ✅ cartisan-core (Epic 01)
- ✅ cartisan-test (Epic 01)
- ✅ cartisan-web (Epic 02)
- ✅ cartisan-event (Epic 02)
- ✅ cartisan-data-jpa (Epic 02)
- ✅ cartisan-security (Epic 03)
- ✅ cartisan-data-query (Epic 04)

**计划中模块**：
- ⏳ cartisan-storage
- ⏳ cartisan-payment
