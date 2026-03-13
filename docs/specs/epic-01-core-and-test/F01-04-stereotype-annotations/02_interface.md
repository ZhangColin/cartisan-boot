# F01-04: cartisan-core — stereotype 架构注解 — 接口契约

## 元数据

| 属性 | 值 |
|------|-----|
| Feature | F01-04: cartisan-core — stereotype 架构注解 |
| 文档版本 | v0.1.0 |
| 日期 | 2026-03-13 |
| 状态 | Phase 2: 接口设计 |
| 前置文档 | [01_requirement.md](./01_requirement.md) |

---

## 1. 包结构

```
com.cartisan.core.stereotype/
├── package-info.java           # 包说明文档
├── SubDomain.java               # 子域类型枚举
├── PortType.java                # 端口类型枚举
├── BoundedContext.java          # 限界上下文注解
├── Aggregate.java               # 聚合根注解
├── DomainService.java           # 领域服务注解
├── Port.java                    # 端口注解
└── Adapter.java                 # 适配器注解
```

**包职责说明**：
- 提供 DDD 架构的标记注解
- 零外部依赖，仅使用 JDK 标准库
- 注解本身不产生运行时行为

---

## 2. 枚举定义

### 2.1 SubDomain（子域类型）

| 值 | 说明 | DDD 定义 |
|----|------|---------|
| `CORE` | 核心域 | 业务核心竞争力的来源 |
| `SUPPORTING` | 支撑域 | 业务流程必须但非核心竞争力 |
| `GENERIC` | 通用域 | 所有行业都一样的通用能力 |

**伪代码签名**：
```java
public enum SubDomain {
    CORE,
    SUPPORTING,
    GENERIC
}
```

### 2.2 PortType（端口类型）

| 值 | 说明 | 方向 |
|----|------|------|
| `REPOSITORY` | 仓储端口 | 出端口（持久化） |
| `CLIENT` | 客户端端口 | 出端口（调用外部服务） |
| `PUBLISHER` | 发布者端口 | 出端口（发布领域事件） |

**伪代码签名**：
```java
public enum PortType {
    REPOSITORY,
    CLIENT,
    PUBLISHER
}
```

---

## 3. 注解定义

### 3.1 BoundedContext（限界上下文）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | 上下文名称 |
| `subDomain` | SubDomain | 是 | 子域类型 |

**元注解配置**：
| 元注解 | 值 | 说明 |
|--------|-----|------|
| `@Target` | `PACKAGE` | 只能标注在包上 |
| `@Retention` | `RUNTIME` | 运行时可反射读取 |
| `@Repeatable` | — | **不支持** |

**伪代码签名**：
```java
@Target(PACKAGE)
@Retention(RUNTIME)
public @interface BoundedContext {
    String name();
    SubDomain subDomain();
}
```

**使用示例**：
```java
// 文件：com/cartisan/billing/package-info.java
/**
 * Billing 上下文 - 计费核心域。
 */
@BoundedContext(name = "Billing", subDomain = CORE)
package com.cartisan.billing;
```

---

### 3.2 Aggregate（聚合根）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| — | — | — | 无属性，标记注解 |

**元注解配置**：
| 元注解 | 值 | 说明 |
|--------|-----|------|
| `@Target` | `TYPE` | 标注在类、接口、枚举上（实际只用于类） |
| `@Retention` | `RUNTIME` | 运行时可反射读取 |

**伪代码签名**：
```java
@Target(TYPE)
@Retention(RUNTIME)
public @interface Aggregate {
}
```

**使用示例**：
```java
@Aggregate
public class Order extends AbstractAggregateRoot<OrderId> {
    // ...
}
```

---

### 3.3 DomainService（领域服务）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| — | — | — | 无属性，标记注解 |

**元注解配置**：
| 元注解 | 值 | 说明 |
|--------|-----|------|
| `@Target` | `TYPE` | 标注在类上 |
| `@Retention` | `RUNTIME` | 运行时可反射读取 |

**伪代码签名**：
```java
@Target(TYPE)
@Retention(RUNTIME)
public @interface DomainService {
}
```

**使用示例**：
```java
@DomainService
public class OrderPricingService {
    // 不属于任何聚合根的定价逻辑
}
```

---

### 3.4 Port（端口）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `value` | PortType | 是 | 端口类型 |

**元注解配置**：
| 元注解 | 值 | 说明 |
|--------|-----|------|
| `@Target` | `TYPE` | 标注在接口上（实际只用于接口） |
| `@Retention` | `RUNTIME` | 运行时可反射读取 |

**伪代码签名**：
```java
@Target(TYPE)
@Retention(RUNTIME)
public @interface Port {
    PortType value();
}
```

**使用示例**：
```java
@Port(PortType.REPOSITORY)
public interface OrderRepository extends BaseRepository<Order, OrderId> {
    // ...
}
```

---

### 3.5 Adapter（适配器）

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `value` | PortType | 是 | 适配的端口类型 |

**元注解配置**：
| 元注解 | 值 | 说明 |
|--------|-----|------|
| `@Target` | `TYPE` | 标注在类上 |
| `@Retention` | `RUNTIME` | 运行时可反射读取 |

**伪代码签名**：
```java
@Target(TYPE)
@Retention(RUNTIME)
public @interface Adapter {
    PortType value();
}
```

**使用示例**：
```java
@Adapter(PortType.REPOSITORY)
public class JpaOrderRepository implements OrderRepository {
    // ...
}
```

---

## 4. 注解属性汇总表

| 注解 | @Target | @Retention | 属性 | 用途 |
|------|---------|------------|------|------|
| `@BoundedContext` | PACKAGE | RUNTIME | name, subDomain | 标注限界上下文 |
| `@Aggregate` | TYPE | RUNTIME | 无 | 标注聚合根 |
| `@DomainService` | TYPE | RUNTIME | 无 | 标注领域服务 |
| `@Port` | TYPE | RUNTIME | value: PortType | 标注端口接口 |
| `@Adapter` | TYPE | RUNTIME | value: PortType | 标注适配器实现 |

---

## 5. 设计约束

### 5.1 零外部依赖

所有类型仅使用 JDK 标准库：
- `java.lang.annotation.*` — 注解相关
- `java.lang.*` — 基础类型（String, Enum）

禁止引入：
- Spring 注解（`org.springframework.*`）
- Jakarta 注解（`jakarta.*`）
- 任何第三方库

### 5.2 运行时行为

**注解本身不产生任何运行时行为**。
- 无 `@Component` 或类似 Spring 注解
- 无注解处理器（Annotation Processor）
- 无反射扫描逻辑

运行时行为由 **F01-07 的 ArchUnit 规则** 提供。

### 5.3 契约验证

| 验证项 | 方式 | 归属 |
|--------|------|------|
| `@Retention(RUNTIME)` 配置正确 | 单元测试验证 | F01-04 |
| `@Target` 配置正确 | 单元测试验证 | F01-04 |
| 枚举值完整性 | 单元测试验证 | F01-04 |
| @Port 只标注在接口上 | ArchUnit 规则 | F01-07 |
| @Adapter 只标注在类上 | ArchUnit 规则 | F01-07 |
| @Adapter 类型与实现端口一致 | ArchUnit 规则 | F01-07 |

---

## 6. 相关文档

- [01_requirement.md](./01_requirement.md) — 需求文档
- [cartisan-boot 设计文档](../../../cartisan-boot-设计文档.md) — 4.2 节 stereotype
- [00_epic_backlog.md](../00_epic_backlog.md) — F01-04 定义

---

## 7. 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v0.1.0 | 2026-03-13 | 初始版本 | Claude |
