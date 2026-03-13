# Feature: F01-06 cartisan-core 模块完整性验证 — 实施计划

> 版本：v0.1 | 日期：2026-03-13

---

## 目标复述

为 cartisan-core 模块添加完整性验证，确保：
1. JavaDoc 格式正确（通过 Gradle javadoc 任务）
2. 包结构受控（只允许 domain/exception/stereotype/util 四个一级包）
3. 根包整洁（不允许随意添加工具类）

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `cartisan-core/src/test/java/com/cartisan/core/CartisanCoreModuleTest.java` | 包结构完整性测试 |
| 修改 | `cartisan-core/build.gradle.kts` | 添加 javadoc 校验配置 |

---

## 核心流程（伪代码）

### JavaDoc 校验流程

```gradle
// 1. 配置 javadoc 任务
tasks.javadoc {
    options.addStringOption("Xdoclint:all,-missing", "-quiet")
}

// 2. 每次 build 都执行
tasks.build {
    dependsOn(tasks.javadoc)
}

// 3. 执行时行为
// ./gradlew :cartisan-core:javadoc
// → 扫描所有 public 类
// → 检查 JavaDoc 格式
// → 格式错误 → build 失败
// → 格式正确 → 生成文档
```

### 包结构检查流程

```java
// 1. 定义白名单包
List<String> allowedPackages = List.of(
    "com.cartisan.core.domain",
    "com.cartisan.core.exception",
    "com.cartisan.core.stereotype",
    "com.cartisan.core.util"
);

// 2. 使用 ArchUnit 检查
classes()
    .that().resideInAPackage("com.cartisan.core..")
    .should().resideInAnyPackage(allowedPackages.stream()
        .map(p -> p + "..")  // 允许子包
        .toArray(String[]::new))
    .because("只允许四个一级包，但允许其子包");

// 3. 检查根包整洁性
noClasses()
    .that().resideInAPackage("com.cartisan.core")
    .and().areNotAssignableTo(PackageInfo.class)
    .should().exist()
    .because("根包下不应有类，只允许 package-info.java");
```

---

## 原子任务清单

### Step 1: 修改 build.gradle.kts

**文件**：`cartisan-core/build.gradle.kts`

**内容**：
```kotlin
// ========== JavaDoc 校验配置 ==========

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all,-missing", "-quiet")
    }
}

tasks.build {
    dependsOn(tasks.javadoc)
}
```

**验证**：
```bash
./gradlew :cartisan-core:javadoc
# 预期：成功执行，生成文档到 build/docs/javadoc/
```

---

### Step 2: 编写 CartisanCoreModuleTest 测试骨架（红灯）

**文件**：`cartisan-core/src/test/java/com/cartisan/core/CartisanCoreModuleTest.java`

**内容**：
```java
package com.cartisan.core;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * cartisan-core 模块完整性测试。
 */
class CartisanCoreModuleTest {

    private static final String ROOT_PACKAGE = "com.cartisan.core";

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages(ROOT_PACKAGE);

    @Test
    void packageStructure_shouldOnlyUseAllowedTopLevelPackages() {
        // Step 3 实现
    }

    @Test
    void packageStructure_shouldNotAllowUtilityClassesInRoot() {
        // Step 3 实现
    }
}
```

**验证**：
```bash
./gradlew :cartisan-core:compileTestJava
# 预期：编译通过
```

---

### Step 3: 实现 ArchUnit 规则（绿灯）

**文件**：`cartisan-core/src/test/java/com/cartisan/core/CartisanCoreModuleTest.java`

**内容**：
```java
@Test
void packageStructure_shouldOnlyUseAllowedTopLevelPackages() {
    ArchRule rule = classes()
            .that().resideInAPackage(ROOT_PACKAGE + "..")
            .should().resideInAnyPackage(
                    "com.cartisan.core.domain..",
                    "com.cartisan.core.exception..",
                    "com.cartisan.core.stereotype..",
                    "com.cartisan.core.util.."
            )
            .because("只允许四个一级包，但允许其子包");

    rule.check(classes);
}

@Test
void packageStructure_shouldNotAllowUtilityClassesInRoot() {
    // 根包下只允许 package-info.java
    // 由于 ArchUnit 不方便直接排除特定类，使用包层级检查
    ArchRule rule = noClasses()
            .that().resideInAPackage(ROOT_PACKAGE)
            .should().exist()
            .because("根包下不应有类，只允许 package-info.java");

    // 注意：package-info.java 会被导入，但 ArchRule 检查类时会忽略它
    // 如果测试失败，说明有人直接在根包创建了类
    rule.check(classes);
}
```

**验证**：
```bash
./gradlew :cartisan-core:test --tests CartisanCoreModuleTest
# 预期：两个测试都通过
```

---

### Step 4: 全量验证

**命令**：
```bash
./gradlew :cartisan-core:build
```

**验证清单**：
- [ ] 编译通过
- [ ] 所有单元测试通过（包括 ArchitectureTest 和 CartisanCoreModuleTest）
- [ ] javadoc 任务执行成功
- [ ] jar 包成功生成

---

## 实施注意事项

1. **保留现有测试**：`ArchitectureTest.java` 保留不动，新增 `CartisanCoreModuleTest.java`
2. **测试命名**：测试方法名使用 `should_` 前缀，描述期望行为
3. **规则描述**：`because()` 子句清晰说明规则理由
4. **包匹配**：使用 `..` 后缀匹配包及其所有子包

---

## 预期产出

| 文件 | 行数估算 | 说明 |
|------|---------|------|
| `CartisanCoreModuleTest.java` | ~60 行 | 2 个测试方法 + JavaDoc |
| `build.gradle.kts` 修改 | +8 行 | javadoc 配置 |

**总计**：~70 行代码
