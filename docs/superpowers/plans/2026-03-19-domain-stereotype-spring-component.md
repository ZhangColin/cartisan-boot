# DomainService & Adapter @Component 元注解 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `@DomainService` 和 `@Adapter` 携带 `@Component` 元注解，使 Spring 自动扫描为 Bean，同时更新 ArchUnit 规则精确放行 `org.springframework.stereotype..`，恢复当前 BROKEN 状态的构建。

**Architecture:** `DomainService` 和 `Adapter` 加 `@Component` 元注解（与 `@Service`/`@Repository` 完全相同模式）。ArchUnit S-001 移除对 `org.springframework..` 的整体禁止；S-002 白名单增加 `org.springframework.stereotype..`。`build.gradle.kts` 的 `compileOnly spring-context` 已就绪，无需变更。

**Tech Stack:** Java 21, Spring Boot 3.4, ArchUnit 1.3

---

## 当前状态说明

构建**当前处于 BROKEN 状态**：`DomainService.java` 已有 `@Component`，但 ArchUnit S-001 仍禁止 `org.springframework..`，S-002 仍不允许 `org.springframework.stereotype..`。本计划的单一任务需**原子性地**完成所有剩余变更，恢复绿色构建。

---

## 文件变更总览

| 文件 | 操作 | 说明 |
|------|------|------|
| `cartisan-core/src/main/java/com/cartisan/core/stereotype/Adapter.java` | 修改 | 加 `@Component` 元注解 |
| `cartisan-core/src/test/java/com/cartisan/core/stereotype/StereotypeAnnotationsTest.java` | 修改 | 新增断言验证 `@DomainService`、`@Adapter` 携带 `@Component` 元注解 |
| `cartisan-core/src/test/java/com/cartisan/core/arch/ArchitectureTest.java` | 修改 | S-001 移除 `org.springframework..`；S-002 加 `org.springframework.stereotype..` |

---

## Task 1: 原子性完成 Adapter + ArchUnit 规则，恢复绿色构建

**Files:**
- Modify: `cartisan-core/src/main/java/com/cartisan/core/stereotype/Adapter.java`
- Modify: `cartisan-core/src/test/java/com/cartisan/core/stereotype/StereotypeAnnotationsTest.java`
- Modify: `cartisan-core/src/test/java/com/cartisan/core/arch/ArchitectureTest.java`

---

- [ ] **Step 1: 确认当前构建状态**

```bash
./gradlew :cartisan-core:test 2>&1 | grep -E "FAILED|PASSED|ERROR" | head -20
```

期望：看到 S-001 / S-002 失败，确认构建确实 BROKEN。

---

- [ ] **Step 2: 在 StereotypeAnnotationsTest 新增 @Component 元注解守护测试**

在 `cartisan-core/src/test/java/com/cartisan/core/stereotype/StereotypeAnnotationsTest.java` 中，在现有 `@Retention` 和 `@Target` 测试段落**之后**、枚举完整性测试**之前**，插入以下测试（需新增 `import org.springframework.stereotype.Component;`）：

```java
import org.springframework.stereotype.Component;

// ========== @Component 元注解验证 ==========

@ParameterizedTest(name = "{0} 应携带 @Component 元注解，以便 Spring 自动扫描为 Bean")
@MethodSource("springBeanStereotypeAnnotations")
@DisplayName("DomainService 和 Adapter 应携带 @Component 元注解")
void springBeanStereotypes_shouldHaveComponentMetaAnnotation(Class<?> annotation) {
    Component component = annotation.getAnnotation(Component.class);
    assertThat(component)
            .as("注解 %s 应有 @Component 元注解，使 Spring 扫描为 Bean", annotation.getSimpleName())
            .isNotNull();
}

static Stream<Arguments> springBeanStereotypeAnnotations() {
    return Stream.of(
            Arguments.of(Named.of("@DomainService", DomainService.class)),
            Arguments.of(Named.of("@Adapter", Adapter.class))
    );
}
```

---

- [ ] **Step 3: 运行新增测试，确认 Adapter 那条失败**

```bash
./gradlew :cartisan-core:test --tests "com.cartisan.core.stereotype.StereotypeAnnotationsTest.springBeanStereotypes_shouldHaveComponentMetaAnnotation*" 2>&1 | tail -20
```

期望：`@DomainService` 那条 PASS，`@Adapter` 那条 **FAIL**（因为 `Adapter.java` 尚无 `@Component`）。

---

- [ ] **Step 4: 给 Adapter.java 加 @Component 元注解**

在 `cartisan-core/src/main/java/com/cartisan/core/stereotype/Adapter.java` 中：

找到：
```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
```
替换为：
```java
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
```

找到：
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Adapter {
```
替换为：
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Adapter {
```

---

- [ ] **Step 5: 更新 ArchitectureTest — S-001 移除 org.springframework..**

在 `cartisan-core/src/test/java/com/cartisan/core/arch/ArchitectureTest.java` 中，找到 S-001 规则的禁止列表：

找到：
```java
        ArchRule rule = noClasses()
                .that().resideInAPackage(STEREOTYPE_PACKAGE)
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "org.apache..",
                        "com.google..",
                        "com.fasterxml..",
                        "io..",
                        "jakarta..",
                        "reactor..",
                        "com.tngtech.."
                )
                .because("stereotype package should have zero external dependencies (JDK only)");
```
替换为：
```java
        ArchRule rule = noClasses()
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
                .because("stereotype package should not depend on third-party libraries except org.springframework.stereotype for @Component meta-annotation");
```

---

- [ ] **Step 6: 更新 ArchitectureTest — S-002 加 org.springframework.stereotype..**

在同文件中，找到 S-002 规则的允许列表：

找到：
```java
        ArchRule rule = classes()
                .that().resideInAPackage(STEREOTYPE_PACKAGE)
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "javax..",
                        "com.cartisan.core.stereotype.."
                )
                .because("stereotype package should only depend on JDK standard library");
```
替换为：
```java
        ArchRule rule = classes()
                .that().resideInAPackage(STEREOTYPE_PACKAGE)
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "java..",
                        "javax..",
                        "com.cartisan.core.stereotype..",
                        "org.springframework.stereotype.."
                )
                .because("stereotype package may depend on Spring stereotype annotations (@Component) for Spring Bean registration");
```

---

- [ ] **Step 7: 运行 cartisan-core 全量测试**

```bash
./gradlew :cartisan-core:test 2>&1 | tail -15
```

期望：`BUILD SUCCESSFUL`，所有测试通过（包括 S-001、S-002、新增 `@Component` 守护测试）。

若仍有失败：
- S-001 报 `org.springframework.stereotype` 被禁止 → 检查 Step 5 的替换是否生效
- S-002 报 `org.springframework.stereotype` 不在允许列表 → 检查 Step 6 的替换是否生效
- `@Component` 守护测试失败 → 检查 Step 4 的 `@Component` 是否正确加到 `Adapter.java`

---

- [ ] **Step 8: 运行全量测试**

```bash
./gradlew test 2>&1 | tail -10
```

期望：`BUILD SUCCESSFUL`

---

- [ ] **Step 9: Commit**

```bash
git add \
  cartisan-core/src/main/java/com/cartisan/core/stereotype/Adapter.java \
  cartisan-core/src/test/java/com/cartisan/core/stereotype/StereotypeAnnotationsTest.java \
  cartisan-core/src/test/java/com/cartisan/core/arch/ArchitectureTest.java
git commit -m "feat(core): Adapter 加 @Component 元注解，更新 ArchUnit stereotype 包规则"
```

---

## Task 2: 发布到本地 Maven 仓库

- [ ] **Step 1: 发布**

```bash
./gradlew publishToMavenLocal 2>&1 | tail -10
```

期望：`BUILD SUCCESSFUL`
