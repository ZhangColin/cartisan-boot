# Feature: F01-06 cartisan-core 模块完整性验证

> 版本：v0.1 | 日期：2026-03-13
> 依赖：F01-02, F01-03, F01-04, F01-05
> 复杂度：S | 预估工时：0.5d

---

## 背景

cartisan-core 作为框架基础设施，必须保证其对外承诺的约束（零外部依赖、文档完整、结构清晰）是可信的。

核心原则：**谁的约束，谁自己守护**。

- cartisan-core 的"零外部依赖"测试是**自检**——验证 cartisan-core 自己的承诺
- cartisan-test 的 ArchUnit 规则是**规则输出**——供业务项目继承使用

如果把 cartisan-core 的自检移到 cartisan-test，就变成了"我需要别人来告诉我自己是否正确"，依赖关系反了。

| 问的问题 | cartisan-core ArchitectureTest | cartisan-test ArchUnit 规则 |
|---------|-------------------------------|----------------------------|
| 验证对象 | cartisan-core 自身 | 业务项目的代码 |
| 运行时机 | cartisan-core 构建时 | 业务项目构建时 |
| 归属 | F01-06 | F01-07 |

---

## 目标

1. 确保模块始终保持零外部依赖
2. 确保 public API 有完整的 JavaDoc 文档
3. 确保包结构受控，防止随意添加混乱包
4. 确保模块可独立构建和打包

---

## 范围

### 包含 (In Scope)

- JavaDoc 格式校验（通过 Gradle javadoc 任务 + -Xdoclint）
- 一级包白名单守护（domain/exception/stereotype/util）
- 允许一级包内的任意子包（如 domain.event）
- 独立打包验证

### 不包含 (Out of Scope)

- Checkstyle 集成（过重，F01-06 阶段不引入）
- 二级子包的命名约束（允许内部自由组织）
- 跨模块依赖验证（属于 F01-07）

---

## 验收标准 (Acceptance Criteria)

### AC1: JavaDoc 完整性校验
- [ ] `./gradlew :cartisan-core:javadoc` 对缺少 public 类 JavaDoc 的情况报错
- [] javadoc 任务配置 `-Xdoclint:all,-missing`（格式校验但允许缺少方法文档）
- [] 所有 public 类必须有类级 JavaDoc

### AC2: 包结构白名单守护
- [ ] `CartisanCoreModuleTest.packageStructure_shouldOnlyUseAllowedTopLevelPackages()` 通过
- [ ] 只允许 4 个一级包：domain、exception、stereotype、util
- [ ] 允许一级包内任意子包（如 domain.event、exception.spi）

### AC3: 防止根包杂乱
- [ ] `CartisanCoreModuleTest.packageStructure_shouldNotAllowUtilityClassesInRoot()` 通过
- [ ] `com.cartisan.core` 根包下不应有类（只有 package-info.java）

### AC4: 独立打包验证
- [ ] `./gradlew :cartisan-core:build` 成功
- [ ] 生成的 jar 包不包含任何第三方依赖
- [ ] `./gradlew :cartisan-core:dependencies` 配置项显示为零依赖

---

## 约束

### 性能
- javadoc 任务应在 10 秒内完成

### 兼容性
- 使用 JDK 标准的 javadoc 工具，无需额外 Gradle 插件

### 架构
- 测试类留在 `cartisan-core/src/test/`，不移到 cartisan-test
- 保留现有 `ArchitectureTest.java`，新增 `CartisanCoreModuleTest.java`

---

## 技术要点

### JavaDoc 检查工具选型

| 工具 | 能力 | F01-06 采用 |
|------|------|------------|
| ArchUnit | 无法检查 JavaDoc（字节码层面） | ❌ |
| Gradle javadoc + -Xdoclint | 格式校验 | ✅ |
| Checkstyle | 强制文档存在 | ❌（过重） |

**F01-06 策略**：-Xdoclint 做格式检查 + 人工 review 确保覆盖

### 包守护策略

**选 A：固定白名单，允许子包**

```
com.cartisan.core.domain         ✓ 精确匹配
com.cartisan.core.domain.event   ✓ 白名单包的子包（放行）
com.cartisan.core.misc           ✗ 不在白名单中（拦截）
```

**为什么不选其他方案**：
- B（任意子包）：misc、helper、common 都能混进来，等于没守护
- C（命名规范）：谁来定义"DDD 术语列表"？判定模糊

**新增一级包的审批流程**：修改测试白名单 = 修改测试本身就是审批

---

## 交付物

1. `cartisan-core/src/test/java/com/cartisan/core/CartisanCoreModuleTest.java`
2. `cartisan-core/build.gradle.kts`（修改，添加 javadoc 配置）
