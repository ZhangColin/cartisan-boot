# Design: DomainService & Adapter 加 @Component 元注解

> **日期**: 2026-03-19
> **状态**: 已批准

---

## 背景与问题

`cartisan-core` 的 `stereotype` 包提供了 `@DomainService`、`@Adapter` 等 DDD 语义注解。业务项目使用这些注解标注领域服务和适配器类。

**现有问题：**

`@DomainService` 和 `@Adapter` 没有 `@Component` 元注解，Spring 不会将标注了这两个注解的类自动扫描为 Bean。业务项目被迫在领域类上同时写两个注解：

```java
@DomainService
@Component   // 触发 ArchUnit domainShouldNotDependOnSpring 规则报错
public class OrderPricingService { ... }
```

直接写 `@Component` 违反业务项目的 ArchUnit 规则（`domain 层不能直接依赖 org.springframework..`），导致编译失败。

---

## 目标

- 业务代码只写 `@DomainService` 或 `@Adapter`，Spring 自动扫描为 Bean
- 业务项目的 ArchUnit `domainShouldNotDependOnSpring` 规则不受影响
- `cartisan-core` 的架构约束保持精确、可维护

---

## 决策：方案 A — 在 stereotype 注解上添加 @Component 元注解

### 核心原理

`@Service`、`@Repository`、`@Controller` 都是 `@Component` 的元注解形式。`@DomainService` 和 `@Adapter` 采用完全相同的模式：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component   // 元注解：Spring 通过元注解链识别 Bean 候选
public @interface DomainService { }
```

业务领域类的字节码中只有 `import com.cartisan.core.stereotype.DomainService`，没有任何 `org.springframework..` 引用，ArchUnit 的 `domainShouldNotDependOnSpring` 规则不会报错。

### 受影响的 stereotype 注解

| 注解 | 加 @Component | 理由 |
|------|:---:|------|
| `DomainService` | ✅ | 领域服务需要被注入到应用服务 |
| `Adapter` | ✅ | 适配器实现端口，需要被注入到调用方 |
| `Aggregate` | ❌ | 聚合根由 Repository 实例化，不是 Bean |
| `BoundedContext` | ❌ | 纯文档标记 |
| `SubDomain` | ❌ | 纯文档标记 |
| `Port` | ❌ | 接口标记，不是具体类 |
| `PortType` | ❌ | 枚举值标记 |

---

## 当前状态（实施前快照）

| 文件 | 状态 |
|------|------|
| `cartisan-core/build.gradle.kts` | ✅ 已完成：`compileOnly spring-context` 已添加 |
| `stereotype/DomainService.java` | ✅ 已完成：`@Component` 元注解已添加，但 ArchUnit 规则尚未更新，**构建当前处于 BROKEN 状态** |
| `stereotype/Adapter.java` | ❌ 待完成：尚无 `@Component` 元注解 |
| `arch/ArchitectureTest.java` S-001 | ❌ 待完成：仍禁止 `org.springframework..`（与 DomainService 的 @Component 冲突） |
| `arch/ArchitectureTest.java` S-002 | ❌ 待完成：尚未允许 `org.springframework.stereotype..` |

> **注意：** `DomainService.java` 的 `@Component` 已提前加入，但 ArchUnit 规则尚未同步更新，导致 `./gradlew :cartisan-core:test` 当前失败。实施时需原子性地完成 `Adapter.java` + S-001 + S-002 的更新，使构建恢复绿色。

---

## 变更范围

### 1. `cartisan-core/build.gradle.kts` ✅ 已完成

`spring-context` 的 `compileOnly` 依赖已添加，无需操作。

### 2. `stereotype/DomainService.java` ✅ 已完成

`@Component` 元注解已添加，无需操作。

### 3. `stereotype/Adapter.java` ❌ 待完成

```java
import org.springframework.stereotype.Component;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Adapter { }
```

### 4. `arch/ArchitectureTest.java` — 更新 S-001 和 S-002 ❌ 待完成

**S-001（黑名单规则）**：移除 `"org.springframework.."` 的禁止项。该项由 S-002 的白名单更精确地约束。

**S-002（白名单规则）**：新增 `"org.springframework.stereotype.."` 到允许列表。

```java
// S-001: 禁止 stereotype 包依赖以下第三方库（org.springframework.stereotype 由 S-002 白名单覆盖）
noClasses()
    .that().resideInAPackage(STEREOTYPE_PACKAGE)
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "org.apache..",
        "com.google..",
        "com.fasterxml..",
        "io..",
        "jakarta..",
        "reactor..",
        "com.tngtech.."
    )

// S-002: stereotype 包只能依赖 JDK、自身，以及 Spring stereotype 元注解
classes()
    .that().resideInAPackage(STEREOTYPE_PACKAGE)
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage(
        "java..",
        "javax..",
        "com.cartisan.core.stereotype..",
        "org.springframework.stereotype.."  // 新增：@Component 元注解
    )
```

---

## 不变更的内容

- 其余 stereotype 注解（`Aggregate`、`BoundedContext` 等）：无变化
- 业务项目的 ArchUnit 规则：无需修改
- `cartisan-core` 的 `domain` 包和 `exception` 包：无变化
- 所有其他模块：无变化

---

## 验收标准

1. `@DomainService`/`@Adapter` 标注的类无需再加 `@Component`，Spring 自动扫描为 Bean
2. 业务领域类的源文件中无 `org.springframework..` import
3. `./gradlew :cartisan-core:test` BUILD SUCCESSFUL（S-001、S-002 通过）
4. `./gradlew test` 全量测试 BUILD SUCCESSFUL
