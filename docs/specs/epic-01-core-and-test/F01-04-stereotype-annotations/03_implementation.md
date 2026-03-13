# F01-04: cartisan-core — stereotype 架构注解（实施计划）

## 元数据

| 属性 | 值 |
|------|-----|
| Epic | Epic 01: 项目骨架 + Core + Test |
| Feature | F01-04: cartisan-core — stereotype 架构注解 |
| 文档版本 | v0.1.0 |
| 日期 | 2026-03-13 |
| 作者 | Claude |
| 状态 | Phase 3: 实施计划 |
| 前置文档 | [02_interface.md](./02_interface.md) |

---

## 1. 目标复述

实现 DDD 架构注解作为"可执行的架构文档"，包括：
- 5 个标记注解（@BoundedContext, @Aggregate, @DomainService, @Port, @Adapter）
- 2 个枚举（SubDomain, PortType）
- 零外部依赖，仅使用 JDK 标准库
- 单元测试守护元注解契约

---

## 2. 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 创建 | `cartisan-core/src/main/java/com/cartisan/core/stereotype/package-info.java` | 包说明文档 |
| 创建 | `cartisan-core/src/main/java/com/cartisan/core/stereotype/SubDomain.java` | 子域类型枚举 |
| 创建 | `cartisan-core/src/main/java/com/cartisan/core/stereotype/PortType.java` | 端口类型枚举 |
| 创建 | `cartisan-core/src/main/java/com/cartisan/core/stereotype/BoundedContext.java` | 限界上下文注解 |
| 创建 | `cartisan-core/src/main/java/com/cartisan/core/stereotype/Aggregate.java` | 聚合根注解 |
| 创建 | `cartisan-core/src/main/java/com/cartisan/core/stereotype/DomainService.java` | 领域服务注解 |
| 创建 | `cartisan-core/src/main/java/com/cartisan/core/stereotype/Port.java` | 端口注解 |
| 创建 | `cartisan-core/src/main/java/com/cartisan/core/stereotype/Adapter.java` | 适配器注解 |
| 创建 | `cartisan-core/src/test/java/com/cartisan/core/stereotype/StereotypeAnnotationsTest.java` | 单元测试 |

---

## 3. 核心流程（伪代码）

```
1. 创建 stereotype 包
2. 创建 SubDomain 枚举（CORE, SUPPORTING, GENERIC）
3. 创建 PortType 枚举（REPOSITORY, CLIENT, PUBLISHER）
4. 创建 @BoundedContext 注解（@Target(PACKAGE), @Retention(RUNTIME)）
5. 创建 @Aggregate 注解（@Target(TYPE), @Retention(RUNTIME)）
6. 创建 @DomainService 注解（@Target(TYPE), @Retention(RUNTIME)）
7. 创建 @Port 注解（@Target(TYPE), @Retention(RUNTIME), value: PortType）
8. 创建 @Adapter 注解（@Target(TYPE), @Retention(RUNTIME), value: PortType）
9. 创建单元测试验证元注解契约和枚举完整性
```

---

## 4. 原子任务清单

### TASK-01: 创建 stereotype 包结构

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 代码量 | ~30 行 |

**实现内容：**
1. 创建 `cartisan-core/src/main/java/com/cartisan/core/stereotype/` 目录
2. 创建 `package-info.java`，包含包级别 JavaDoc

**验收标准：**
- 目录结构正确
- package-info.java 编译通过
- JavaDoc 说明包职责

---

### TASK-02: 实现 SubDomain 枚举

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-01 |
| 代码量 | ~40 行 |

**实现内容：**
1. 创建 `SubDomain.java`
2. 定义三个枚举值：CORE, SUPPORTING, GENERIC
3. 每个值添加 JavaDoc 说明

**验收标准：**
- 枚举编译通过
- 三个值定义正确
- JavaDoc 完整

---

### TASK-03: 实现 PortType 枚举

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-01 |
| 代码量 | ~50 行 |

**实现内容：**
1. 创建 `PortType.java`
2. 定义三个枚举值：REPOSITORY, CLIENT, PUBLISHER
3. 每个值添加 JavaDoc 说明

**验收标准：**
- 枚举编译通过
- 三个值定义正确
- JavaDoc 完整

---

### TASK-04: 实现 @BoundedContext 注解

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-02 |
| 代码量 | ~60 行 |

**实现内容：**
1. 创建 `BoundedContext.java`
2. 配置 `@Target(ElementType.PACKAGE)`
3. 配置 `@Retention(RetentionPolicy.RUNTIME)`
4. 定义 `name()` 属性
5. 定义 `subDomain()` 属性（返回 SubDomain）
6. 添加完整 JavaDoc

**验收标准：**
- 注解编译通过
- @Target 和 @Retention 配置正确
- 属性定义正确

---

### TASK-05: 实现 @Aggregate 注解

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-01 |
| 代码量 | ~40 行 |

**实现内容：**
1. 创建 `Aggregate.java`
2. 配置 `@Target(ElementType.TYPE)`
3. 配置 `@Retention(RetentionPolicy.RUNTIME)`
4. 无属性，标记注解
5. 添加完整 JavaDoc

**验收标准：**
- 注解编译通过
- @Target 和 @Retention 配置正确

---

### TASK-06: 实现 @DomainService 注解

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-01 |
| 代码量 | ~40 行 |

**实现内容：**
1. 创建 `DomainService.java`
2. 配置 `@Target(ElementType.TYPE)`
3. 配置 `@Retention(RetentionPolicy.RUNTIME)`
4. 无属性，标记注解
5. 添加完整 JavaDoc

**验收标准：**
- 注解编译通过
- @Target 和 @Retention 配置正确

---

### TASK-07: 实现 @Port 注解

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-03 |
| 代码量 | ~50 行 |

**实现内容：**
1. 创建 `Port.java`
2. 配置 `@Target(ElementType.TYPE)`
3. 配置 `@Retention(RetentionPolicy.RUNTIME)`
4. 定义 `value()` 属性（返回 PortType）
5. 添加完整 JavaDoc

**验收标准：**
- 注解编译通过
- @Target 和 @Retention 配置正确
- value 属性定义正确

---

### TASK-08: 实现 @Adapter 注解

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-03 |
| 代码量 | ~50 行 |

**实现内容：**
1. 创建 `Adapter.java`
2. 配置 `@Target(ElementType.TYPE)`
3. 配置 `@Retention(RetentionPolicy.RUNTIME)`
4. 定义 `value()` 属性（返回 PortType）
5. 添加完整 JavaDoc

**验收标准：**
- 注解编译通过
- @Target 和 @Retention 配置正确
- value 属性定义正确

---

### TASK-09: 编写 StereotypeAnnotationsTest（元注解验证）

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | TASK-04 ~ TASK-08 |
| 代码量 | ~150 行 |

**实现内容：**
1. 创建 `StereotypeAnnotationsTest.java`
2. 使用 JUnit 5 + AssertJ
3. 使用 @ParameterizedTest 验证所有注解的 @Retention(RUNTIME)

**测试场景：**
- 所有注解的 @Retention(RUNTIME) 被验证
- @BoundedContext 的 @Target(PACKAGE) 被验证
- @Aggregate 的 @Target(TYPE) 被验证
- @DomainService 的 @Target(TYPE) 被验证
- @Port 的 @Target(TYPE) 被验证
- @Adapter 的 @Target(TYPE) 被验证

**验收标准：**
- 测试编译通过
- 所有测试通过
- 覆盖所有元注解配置

---

### TASK-10: 编写枚举完整性测试

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | TASK-02, TASK-03 |
| 代码量 | ~40 行 |

**实现内容：**
1. 在 `StereotypeAnnotationsTest` 中添加枚举测试
2. 验证 SubDomain 有且仅有三个值
3. 验证 PortType 有且仅有三个值

**测试场景：**
- SubDomain.values() 精确匹配 [CORE, SUPPORTING, GENERIC]
- PortType.values() 精确匹配 [REPOSITORY, CLIENT, PUBLISHER]

**验收标准：**
- 测试编译通过
- 所有测试通过

---

### TASK-11: ArchUnit 架构验证

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.5h |
| 依赖 | 所有 TASK |
| 代码量 | ~80 行 |

**实现内容：**
1. 更新 `cartisan-core/src/test/java/com/cartisan/core/arch/ArchitectureTest.java`
2. 添加 stereotype 包的零外部依赖验证

**验证规则：**
- stereotype 包不依赖任何第三方库
- stereotype 包不依赖 Spring
- stereotype 包仅依赖 JDK 标准库

**验收标准：**
- ArchUnit 测试通过
- 规则失败时有清晰的错误消息

---

### TASK-12: 模块完整性验证

| 属性 | 值 |
|------|-----|
| 优先级 | P0 |
| 预估工时 | 0.25h |
| 依赖 | 所有 TASK |
| 代码量 | ~50 行 |

**实现内容：**
1. 运行 `./gradlew :cartisan-core:build`
2. 验证构建成功
3. 验证测试覆盖率 > 90%
4. 验证 JavaDoc 生成

**验收标准：**
- Gradle 构建成功
- 所有测试通过
- 测试覆盖率 > 90%
- JavaDoc 生成无警告

---

## 5. 任务依赖关系

```
TASK-01 (包结构)
    │
    ├──→ TASK-02 (SubDomain)
    │       │
    │       └──→ TASK-04 (@BoundedContext)
    │
    ├──→ TASK-03 (PortType)
    │       │
    │       ├──→ TASK-07 (@Port)
    │       └──→ TASK-08 (@Adapter)
    │
    ├──→ TASK-05 (@Aggregate)
    │
    └──→ TASK-06 (@DomainService)
            │
            └──→ TASK-09 (元注解验证测试)
                    │
                    ├──→ TASK-10 (枚举完整性测试)
                    │       │
                    │       └──→ TASK-11 (ArchUnit 验证)
                    │               │
                    │               └──→ TASK-12 (模块完整性验证)
```

---

## 6. 推荐执行顺序

### 第一批：枚举（0.5h）
1. TASK-01: 创建 stereotype 包结构
2. TASK-02: 实现 SubDomain 枚举
3. TASK-03: 实现 PortType 枚举

### 第二批：注解实现（1h）
4. TASK-04: 实现 @BoundedContext 注解
5. TASK-05: 实现 @Aggregate 注解
6. TASK-06: 实现 @DomainService 注解
7. TASK-07: 实现 @Port 注解
8. TASK-08: 实现 @Adapter 注解

### 第三批：测试验证（1h）
9. TASK-09: 编写 StereotypeAnnotationsTest（元注解验证）
10. TASK-10: 编写枚举完整性测试
11. TASK-11: ArchUnit 架构验证
12. TASK-12: 模块完整性验证

**总计预估：2.5 小时**

---

## 7. 测试策略

### 7.1 单元测试覆盖

| 测试类 | 测试内容 | 场景数 |
|--------|----------|--------|
| StereotypeAnnotationsTest | 元注解契约验证 | 6 |
| StereotypeAnnotationsTest | 枚举完整性验证 | 2 |

**关键测试方法：**
- `allStereotypeAnnotations_shouldBeRetainedAtRuntime()` — @ParameterizedTest
- `boundedContext_shouldTargetPackageOnly()` — 精确匹配
- `aggregate_shouldTargetTypeOnly()` — 精确匹配
- `domainService_shouldTargetTypeOnly()` — 精确匹配
- `port_shouldTargetTypeOnly()` — 精确匹配
- `adapter_shouldTargetTypeOnly()` — 精确匹配
- `portType_shouldHaveExactlyThreeValues()` — 精确匹配
- `subDomain_shouldHaveExactlyThreeValues()` — 精确匹配

### 7.2 架构测试

- ArchUnit 验证 stereotype 包零外部依赖
- ArchUnit 验证 stereotype 包不依赖 Spring

### 7.3 覆盖率目标

- 行覆盖率：> 90%
- 分支覆盖率：> 80%

---

## 8. 相关文档

- [01_requirement.md](./01_requirement.md) — 需求文档
- [02_interface.md](./02_interface.md) — 接口设计
- [00_epic_backlog.md](../00_epic_backlog.md) — Epic Backlog

---

## 9. 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v0.1.0 | 2026-03-13 | 初始版本 | Claude |
