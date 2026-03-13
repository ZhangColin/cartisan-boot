# Feature: F01-06 cartisan-core 模块完整性验证 — 接口契约

> 版本：v0.1 | 日期：2026-03-13

---

## 概述

本 Feature 不包含业务接口，主要产出是测试类和构建配置。

**注意**：Phase 2 只产出文档（伪代码、配置描述），不产出源代码。Java 源代码在 Phase 4 生成。

---

## 测试类设计

### CartisanCoreModuleTest

**位置**：`cartisan-core/src/test/java/com/cartisan/core/CartisanCoreModuleTest.java`

**职责**：验证 cartisan-core 模块的包结构完整性

**方法签名**（伪代码）：

```java
/**
 * cartisan-core 模块完整性测试。
 *
 * <p>验证范围：</p>
 * <ul>
 *   <li>包结构白名单守护</li>
 *   <li>根包整洁性</li>
 * </ul>
 *
 * <p>注意：零外部依赖验证由 {@code ArchitectureTest} 负责，JavaDoc 完整性由 Gradle javadoc 任务负责。</p>
 */
class CartisanCoreModuleTest {

    /**
     * 规则 P-001：所有类必须在允许的一级包中。
     *
     * <p>允许的一级包：</p>
     * <ul>
     *   <li>com.cartisan.core.domain</li>
     *   <li>com.cartisan.core.exception</li>
     *   <li>com.cartisan.core.stereotype</li>
     *   <li>com.cartisan.core.util</li>
     * </ul>
     *
     * <p>允许这些包的任意子包（如 domain.event）。</p>
     */
    @Test
    void packageStructure_shouldOnlyUseAllowedTopLevelPackages();

    /**
     * 规则 P-002：根包下不应有类。
     *
     * <p>com.cartisan.core 根包下只允许 package-info.java，
     * 防止有人随手添加工具类到根包。</p>
     */
    @Test
    void packageStructure_shouldNotAllowUtilityClassesInRoot();
}
```

---

## 构建配置设计

### build.gradle.kts 修改

**文件**：`cartisan-core/build.gradle.kts`

**新增内容**：

```kotlin
// ========== JavaDoc 校验配置 ==========

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        // -Xdoclint:all 启用所有检查
        // -missing 允许缺少文档（只检查格式，不强制必须存在）
        // -quiet 减少输出噪音
        addStringOption("Xdoclint:all,-missing", "-quiet")
    }
}

// 确保 javadoc 在 build 时执行
tasks.build {
    dependsOn(tasks.javadoc)
}
```

**配置说明**：

| 配置项 | 值 | 作用 |
|--------|-----|------|
| `Xdoclint` | `all,-missing` | 启用所有格式检查，但允许缺少文档 |
| `-quiet` | - | 减少输出噪音，只显示错误 |
| `build dependsOn javadoc` | - | 每次 build 都执行 javadoc 校验 |

---

## 核心流程（伪代码）

### 验证流程

```
1. 开发者执行 ./gradlew :cartisan-core:build

2. Gradle 执行 javadoc 任务
   ├─ 扫描所有 public 类
   ├─ 检查类级 JavaDoc 格式
   └─ 如有格式错误，build 失败

3. Gradle 执行 test 任务
   ├─ 运行 ArchitectureTest（零外部依赖）
   ├─ 运行 CartisanCoreModuleTest（包结构）
   └─ 如有测试失败，build 失败

4. build 成功 → 模块完整性验证通过
```

---

## 数据库变更

无。

---

## 错误处理

| 场景 | 期望行为 |
|------|---------|
| JavaDoc 格式错误 | javadoc 任务报错，build 失败 |
| 类不在白名单包中 | CartisanCoreModuleTest 失败，指出违规类 |
| 根包有工具类 | CartisanCoreModuleTest 失败，指出违规类 |

---

## 技术决策

### 决策 1：为什么不用 Checkstyle？

**考虑的方案**：
1. Checkstyle 的 MissingJavadoc 规则
2. Gradle javadoc + -Xdoclint
3. ArchUnit（技术上不可行）

**选择**：方案 2

**理由**：
- Checkstyle 引入额外依赖和配置复杂度
- -Xdoclint 是 JDK 标准工具，无需额外插件
- F01-06 阶段格式检查 + 人工 review 是务实选择

### 决策 2：为什么 -missing 而不是强制文档？

**理由**：
- Record 组件访问器（eventId()）是自解释的，强制文档过度
- 枚举值（CORE、REPOSITORY）是自解释的
- public 类必须有类级 JavaDoc（由人工 review 保证）

### 决策 3：包守护为什么只守护一级？

**理由**：
- 一级包代表架构关注点（domain、exception 等）
- 二级包是内部组织（domain.event），允许灵活
- 新增一级包需要修改测试白名单 = 审批流程
