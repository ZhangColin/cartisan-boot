# F01-04: cartisan-core — stereotype 架构注解

## 元数据

| 属性 | 值 |
|------|-----|
| Epic | Epic 01: 项目骨架 + Core + Test |
| Feature | F01-04: cartisan-core — stereotype 架构注解 |
| 文档版本 | v0.1.0 |
| 日期 | 2026-03-13 |
| 作者 | Claude |
| 状态 | Phase 1: 需求定义 |
| 依赖 | F01-02: cartisan-core — domain 基础类型 |
| 复杂度 | S |
| 预估工时 | 0.5d |

---

## 1. 问题陈述

### 1.1 背景

DDD（领域驱动设计）项目中有多个架构概念需要表达：

- **限界上下文**：系统的业务边界，不同上下文内的模型独立演化
- **聚合根**：一致性维护的边界，数据持久化的基本单位
- **领域服务**：不属于任何聚合根或值对象的领域逻辑
- **端口与适配器**：六边形架构的边界，系统与外部交互的契约

这些概念目前只存在于设计文档或团队约定中，代码层面没有显式表达。这导致：

1. **架构意图不可见**：新成员读代码时无法快速识别各类的架构角色
2. **无法自动验证**：没有"可执行的架构文档"，只能靠人工 Code Review 守护规则
3. **AI 生成代码无约束**：AI 不理解项目的架构约定，生成的代码可能违反分层规则

### 1.2 影响

这些问题导致：

1. **架构侵蚀**：随着代码演进，分层约定被逐渐破坏
2. **沟通成本高**：需要口头传达"这个类是聚合根"、"那个包属于 Billing 上下文"
3. **质量门禁缺失**：无法在编译期/测试期自动检查架构约束

### 1.3 目标

提供一套**零外部依赖**的架构注解，作为"可执行的架构文档"。

---

## 2. 解决方案概述

### 2.1 设计原则

| 原则 | 说明 |
|------|------|
| **纯粹** | 只依赖 JDK，不引入任何外部库 |
| **标记而非行为** | 注解本身不产生运行时行为，只标记架构角色 |
| **可执行** | 配合 ArchUnit 规则，自动验证架构约束 |
| **语义清晰** | 注解的语义粒度与架构概念的粒度一致 |

### 2.2 核心注解

| 注解 | 目标 | 职责 |
|------|------|------|
| `@BoundedContext` | PACKAGE | 标注限界上下文，写在 package-info.java 上 |
| `@Aggregate` | TYPE（类） | 标注聚合根 |
| `@DomainService` | TYPE（类） | 标注领域服务 |
| `@Port` | TYPE（接口） | 标注端口接口 |
| `@Adapter` | TYPE（类） | 标注适配器实现 |

### 2.3 支持枚举

| 枚举 | 值 | 说明 |
|------|-----|------|
| `SubDomain` | CORE, SUPPORTING, GENERIC | DDD 子域分类，完备分类 |
| `PortType` | REPOSITORY, CLIENT, PUBLISHER | 六边形架构端口类型 |

---

## 3. 功能范围

### 3.1 包含（In Scope）

| 类型 | 包路径 | 职责 |
|------|--------|------|
| `@BoundedContext` | `com.cartisan.core.stereotype` | 标注限界上下文顶层包 |
| `@Aggregate` | `com.cartisan.core.stereotype` | 标注聚合根类 |
| `@DomainService` | `com.cartisan.core.stereotype` | 标注领域服务类 |
| `@Port` | `com.cartisan.core.stereotype` | 标注端口接口 |
| `@Adapter` | `com.cartisan.core.stereotype` | 标注适配器类 |
| `SubDomain` | `com.cartisan.core.stereotype` | 子域类型枚举 |
| `PortType` | `com.cartisan.core.stereotype` | 端口类型枚举 |
| 单元测试 | `com.cartisan.core.stereotype` | 验证元注解契约、枚举完整性 |

### 3.2 不包含（Out of Scope）

| 功能 | 理由 | 归属 |
|------|------|------|
| ArchUnit 规则 | 需要 ArchUnit 依赖，违反零依赖原则 | F01-07: cartisan-test |
| 注解处理器 | 增加复杂度，当前阶段不需要 | 未来可选 |
| 架构规则验证 | 属于测试工具箱职责 | F01-07: cartisan-test |

---

## 4. 用户故事

### US-01: 标注限界上下文

**作为** 领域建模者
**我想要** 在包上标注限界上下文
**以便** ArchUnit 能自动验证上下文边界

**验收标准：**
- `@BoundedContext` 可标注在 `package-info.java` 上
- `name()` 属性返回上下文名称
- `subDomain()` 属性返回子域类型
- 不支持 `@Repeatable`（一个包只能属于一个上下文）

### US-02: 标注聚合根

**作为** 领域建模者
**我想要** 在类上标注聚合根
**以便** ArchUnit 能验证只有聚合根才能拥有 Repository

**验收标准：**
- `@Aggregate` 可标注在类上
- ArchUnit 规则能识别被标注的类

### US-03: 标注端口与适配器

**作为** 架构设计师
**我想要** 在接口上标注端口类型，在实现类上标注适配器类型
**以便** ArchUnit 能验证适配器与端口的类型一致性

**验收标准：**
- `@Port(PortType.REPOSITORY)` 标注在仓储接口上
- `@Adapter(PortType.REPOSITORY)` 标注在仓储实现上
- ArchUnit 规则能验证类型一致性

### US-04: 验证注解元数据

**作为** 框架维护者
**我想要** 单元测试验证注解的元注解配置正确
**以便** 防止误改导致 ArchUnit 规则失效

**验收标准：**
- 所有注解的 `@Retention(RUNTIME)` 被测试验证
- 所有注解的 `@Target` 正确性被测试验证
- 枚举值的完整性被测试验证

---

## 5. 边界场景与异常处理

| 场景 | 处理方式 | 异常类型 |
|------|----------|----------|
| 在 class 上标注 `@BoundedContext` | 编译器拒绝（@Target(PACKAGE)） | 编译错误 |
| 在接口上标注 `@Adapter` | 编译器允许，由 ArchUnit 规则检查 | — |
| `@Port` 和 `@Adapter` 类型不一致 | 编译器允许，由 ArchUnit 规则检查 | — |
| 同一个包标注多个 `@BoundedContext` | 编译器拒绝（不支持 @Repeatable） | 编译错误 |
| `@Retention` 被误改为 SOURCE | 单元测试失败 | 断言失败 |

---

## 6. 非功能性需求

### 6.1 技术约束

- **JDK 版本**：Java 21
- **依赖**：仅 JDK 标准库（java.lang.annotation 包）
- **测试框架**：JUnit 5 + AssertJ
- **保留策略**：所有注解 `@Retention(RUNTIME)`

### 6.2 质量标准

- 所有注解有完整的 JavaDoc
- package-info.java 说明 stereotype 包的职责
- 单元测试覆盖所有元注解验证

### 6.3 架构约束

- **零外部依赖**：仅依赖 JDK 标准库
- **不产生运行时行为**：注解只是标记，不包含任何处理逻辑

---

## 7. 验收标准

### 7.1 功能验收

| 注解 | 验收标准 |
|------|----------|
| `@BoundedContext` | @Target(PACKAGE); @Retention(RUNTIME); name() 和 subDomain() 属性可读 |
| `@Aggregate` | @Target(TYPE); @Retention(RUNTIME); 可标注在类上 |
| `@DomainService` | @Target(TYPE); @Retention(RUNTIME); 可标注在类上 |
| `@Port` | @Target(TYPE); @Retention(RUNTIME); value() 返回 PortType |
| `@Adapter` | @Target(TYPE); @Retention(RUNTIME); value() 返回 PortType |
| `SubDomain` | 枚举值：CORE, SUPPORTING, GENERIC |
| `PortType` | 枚举值：REPOSITORY, CLIENT, PUBLISHER |

### 7.2 质量验收

- **元注解验证测试**：所有注解的 @Retention(RUNTIME) 被测试验证
- **@Target 正确性测试**：每个注解的 @Target 被测试验证
- **枚举完整性测试**：PortType 和 SubDomain 的枚举值被精确验证
- **零外部依赖**：ArchUnit 验证无第三方依赖

---

## 8. 依赖关系

```
F01-01 (Gradle 多模块项目骨架)
    │
    └──→ F01-02 (domain 基础类型)
            │
            └──→ F01-04 (stereotype 架构注解) ◄── 当前文档
```

---

## 9. 相关文档

- [F01-02 需求文档](../F01-02-domain-base-types/01_requirement.md)
- [cartisan-boot 设计文档](../../../cartisan-boot-设计文档.md)
- [00_epic_backlog.md](../00_epic_backlog.md)
- [AI 协作开发 SOP](../../../sop/AI协作开发SOP.md)

---

## 10. 设计决策

### 决策 1：PortType 作为独立枚举

**选项**：
- A. 独立枚举类 `PortType`
- B. 内嵌在 `@Port` 注解中

**选择**：A

**理由**：
- `PortType` 被 `@Port` 和 `@Adapter` 两个注解共享
- 独立枚举语义更清晰，API 更简洁
- 未来扩展无压力

### 决策 2：@BoundedContext 不支持 @Repeatable

**选择**：不支持

**理由**：
- DDD 的限界上下文边界应该是清晰的
- 框架应该让架构错误不可表达
- 需要多重标注的场景应通过 shared kernel 表达

### 决策 3：所有注解 @Target(TYPE)

**选择**：所有注解使用 @Target(TYPE)，通过 Javadoc + ArchUnit 规则强制语义约束

**理由**：
- Java 的 ElementType.TYPE 无法区分类和接口
- 注解层做粗粒度限制，ArchUnit 层做精确语义检查
- 职责分层清晰

### 决策 4：SubDomain 只有三个标准值

**选择**：CORE, SUPPORTING, GENERIC，不加 OTHER

**理由**：
- Core/Supporting/Generic 是完备分类
- 框架应强制思考，不提供逃避路径
- 判断子域归属是 DDD 战略设计的重要决策

---

## 11. 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v0.1.0 | 2026-03-13 | 初始版本，通过 brainstorming 产出 | Claude |
