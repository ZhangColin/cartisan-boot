# @Condition 注解实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 cartisan-data-jpa 模块添加 @Condition 注解，简化 JPA 条件查询，通过注解自动生成 Specification

**Architecture:**
- 注解驱动的查询条件生成器
- ConditionType 枚举定义 11 种查询类型
- ConditionSpecifications 通过反射读取注解，生成 JPA Specification
- 支持嵌套属性路径（如 `user.profile.name`）和多字段模糊搜索

**Tech Stack:**
- Spring Data JPA (Specification API)
- JPA Criteria API
- Java 反射
- JUnit 5 + AssertJ

---

## 文件结构

```
cartisan-data-jpa/
├── src/main/java/com/cartisan/data/jpa/specification/
│   ├── Condition.java                    # 注解定义
│   ├── ConditionType.java                # 查询类型枚举 (11 种)
│   ├── ConditionSpecifications.java      # Specification 生成器
│   └── package-info.java                 # 包文档
│
└── src/test/java/com/cartisan/data/jpa/specification/
    ├── ConditionTypeTest.java            # 枚举单元测试
    ├── ConditionSpecificationsTest.java  # 集成测试（包含测试实体）
    ├── TestProduct.java                  # 测试实体
    ├── TestProductRepository.java        # 测试 Repository
    └── ProductQuery.java                 # 测试查询 DTO
```

---

## Task 1: 创建 specification 包结构

**Files:**
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/package-info.java`

- [ ] **Step 1: 创建 package-info.java**

添加包文档，说明 specification 包提供基于注解的 JPA 查询条件生成功能。

- [ ] **Step 2: 验证编译**

```bash
./gradlew :cartisan-data-jpa:compileJava
```

Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/
git commit -m "feat(data-jpa): add specification package structure"
```

---

## Task 2: 实现 ConditionType 枚举

**Files:**
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionType.java`
- Test: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/ConditionTypeTest.java`

- [ ] **Step 1: 编写枚举测试**

创建测试文件，验证 11 种查询类型枚举值存在：
- EQUAL, NOT_EQUAL
- GREATER_EQUAL, GREATER, LESS_EQUAL, LESS
- INNER_LIKE, LEFT_LIKE, RIGHT_LIKE
- IN, BETWEEN

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionTypeTest
```

Expected: FAIL with "Class not found"

- [ ] **Step 3: 实现 ConditionType 枚举**

创建枚举类，定义 11 种 JPA Criteria 查询类型。

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionTypeTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionType.java
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/ConditionTypeTest.java
git commit -m "feat(data-jpa): add ConditionType enum with 11 query types"
```

---

## Task 3: 实现 @Condition 注解

**Files:**
- Create: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/Condition.java`

- [ ] **Step 1: 编写注解定义**

创建 `@Condition` 注解：
- @Target(ElementType.FIELD)
- @Retention(RetentionPolicy.RUNTIME)
- 属性：propName (String, default ""), type (ConditionType, default EQUAL), blurry (String, default "")

- [ ] **Step 2: 验证编译**

```bash
./gradlew :cartisan-data-jpa:compileJava
```

Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/Condition.java
git commit -m "feat(data-jpa): add @Condition annotation for query criteria"
```

---

## Task 4: 实现查询类型处理器（Predicate 生成）

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionSpecifications.java`

- [ ] **Step 1: 编写处理器测试**

针对每种 ConditionType 编写测试，验证能生成正确的 Predicate：
- shouldGenerateEqualPredicate
- shouldGenerateNotEqualPredicate
- shouldGenerateGreaterEqualPredicate
- shouldGenerateInnerLikePredicate
- shouldGenerateInPredicate
- shouldGenerateBetweenPredicate

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionSpecificationsTest
```

Expected: FAIL with "Class not found"

- [ ] **Step 3: 实现 ConditionSpecifications 基础结构**

创建类，定义：
- `of(Object queryCondition)` 静态工厂方法
- 内部 Handler 接口或 Map 存储 ConditionType → Predicate 生成逻辑
- 支持 11 种查询类型的 Predicate 生成

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionSpecificationsTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionSpecifications.java
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/ConditionSpecificationsTest.java
git commit -m "feat(data-jpa): add ConditionSpecifications with predicate generators"
```

---

## Task 5: 实现反射读取注解逻辑

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionSpecifications.java`

- [ ] **Step 1: 编写注解解析测试**

验证能正确读取查询 DTO 上的 @Condition 注解并生成 Specification：
- shouldReadConditionAnnotation
- shouldHandleCustomPropName
- shouldSkipNullAndEmptyValues

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionSpecificationsTest
```

Expected: FAIL

- [ ] **Step 3: 实现注解解析逻辑**

在 ConditionSpecifications 中实现：
- 反射获取查询对象所有字段（包括父类）
- 过滤带 @Condition 注解的字段
- 跳过 null 和空字符串值
- 构建 Predicate 列表

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionSpecificationsTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionSpecifications.java
git commit -m "feat(data-jpa): add annotation reflection parsing logic"
```

---

## Task 6: 支持嵌套属性路径

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionSpecifications.java`

- [ ] **Step 1: 编写嵌套属性测试**

验证支持 `user.profile.name` 格式的属性路径：
- shouldHandleNestedPropertyPath
- shouldHandleMultipleLevelNesting

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionSpecificationsTest
```

Expected: FAIL

- [ ] **Step 3: 实现嵌套路径支持**

实现 Path 解析逻辑：
- 按 `.` 分割属性名
- 使用 `root.get()` 链式调用构建嵌套路径

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionSpecificationsTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionSpecifications.java
git commit -m "feat(data-jpa): support nested property paths in @Condition"
```

---

## Task 7: 支持多字段模糊搜索（blurry）

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionSpecifications.java`

- [ ] **Step 1: 编写 blurry 测试**

验证 `blurry = "title,content"` 多字段模糊搜索：
- shouldHandleBlurrySearch
- shouldCombineBlurryFieldsWithOrPredicate

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionSpecificationsTest
```

Expected: FAIL

- [ ] **Step 3: 实现 blurry 支持**

解析 blurry 属性，按逗号分割，生成 OR 连接的 LIKE Predicate。

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionSpecificationsTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/specification/ConditionSpecifications.java
git commit -m "feat(data-jpa): add blurry multi-field search support"
```

---

## Task 8: 集成测试（完整场景）

**Files:**
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/ConditionIntegrationTest.java`
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/TestProduct.java`
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/TestProductRepository.java`
- Create: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/ProductQuery.java`

- [ ] **Step 1: 创建测试实体和 Repository**

创建 TestProduct 实体（JPA @Entity）和对应的 Repository。

- [ ] **Step 2: 创建查询 DTO**

创建 ProductQuery Record，标注各种 @Condition 注解测试不同查询类型。

- [ ] **Step 3: 编写集成测试**

验证完整使用场景：
- shouldFilterByEqualCondition
- shouldFilterByLikeCondition
- shouldFilterByGreaterCondition
- shouldFilterByInCondition
- shouldFilterByBetweenCondition
- shouldCombineMultipleConditions

- [ ] **Step 4: 运行集成测试**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionIntegrationTest
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/
git commit -m "test(data-jpa): add Condition annotation integration tests"
```

---

## Task 9: 更新模块文档

**Files:**
- Create: `docs/guide/condition-annotation.md`

- [ ] **Step 1: 编写使用文档**

文档内容：
- @Condition 注解介绍
- ConditionType 类型说明
- 使用示例（查询 DTO + Repository）
- 注意事项

- [ ] **Step 2: 验证文档格式**

确保 Markdown 格式正确，代码块可读。

- [ ] **Step 3: Commit**

```bash
git add docs/guide/condition-annotation.md
git commit -m "docs: add @Condition annotation usage guide"
```

---

## Task 10: 最终验证

- [ ] **Step 1: 运行模块全量测试**

```bash
./gradlew :cartisan-data-jpa:test
```

Expected: 全部 PASS

- [ ] **Step 2: 检查代码质量**

```bash
./gradlew :cartisan-data-jpa:checkstyleMain :cartisan-data-jpa:checkstyleTest
```

（如有 checkstyle 配置）

- [ ] **Step 3: 最终 Commit**

如有调整，提交最终修改。

---

## 参考资料

- 旧框架实现：`~/workspace/cartisan-by-spring-boot-2/cartisan-persistence/src/main/java/com/cartisan/repository/`
- Spring Data JPA Specification 文档
- JPA Criteria API 文档
