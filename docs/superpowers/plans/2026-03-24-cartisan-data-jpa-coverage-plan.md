# cartisan-data-jpa 测试覆盖率改进计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 提升 cartisan-data-jpa 模块测试覆盖率至 75% 以上

**架构:** 单元测试 + 集成测试，使用 @DataJpaTest 和 TestEntityManager

**技术栈:** JUnit 5, AssertJ, Mockito, @DataJpaTest, TestEntityManager

---

## 当前覆盖率状态

| 模块 | 指令覆盖率 | 分支覆盖率 |
|------|-----------|-----------|
| cartisan-data-jpa 总体 | 67% | 60% |
| ConditionSpecifications | 60% | 60% |
| BaseRepositoryImpl | 67% | 59% |
| DomainEventPublisherHolder | 未测试 | 未测试 |

---

### Task 1: ConditionSpecifications - 边界条件和异常处理测试

**文件:**
- 修改: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/ConditionSpecificationsTest.java`

**目标:** ConditionSpecifications 覆盖率 ≥ 70%（边界测试完成后，通过其他任务进一步提升）

**步骤:**

- [ ] **Step 1: 添加 IN 条件数组值测试**
  - 测试 `IN` 类型使用 Object[] 数组（非 Collection）的场景
  - 验证生成的 Specification 不为 null

- [ ] **Step 2: 添加 BETWEEN 条件数组值测试**
  - 测试 `BETWEEN` 类型使用 Object[] 数组（非 List）的场景
  - 验证生成的 Specification 不为 null

- [ ] **Step 3: 添加无效 IN 条件测试**
  - 测试 `IN` 类型使用非 Collection/非数组的值（如 String）
  - 验证返回 disjunction（不匹配任何记录的 predicate）

- [ ] **Step 4: 添加无效 BETWEEN 条件测试**
  - 测试 `BETWEEN` 类型使用非 2 元素 List/数组的值
  - 验证返回 disjunction

- [ ] **Step 5: 添加 getFieldValue 异常测试**
  - 测试 `of()` 方法使用缺少必需字段的 query condition 对象
  - 验证抛出 IllegalArgumentException，消息包含 "Field.*not found"

- [ ] **Step 6: 添加 buildPath 单层路径测试**
  - 测试 `buildPath` 方法处理不含 "." 的单层属性名
  - 使用 mock 验证 root.get() 被正确调用

- [ ] **Step 7: 添加 buildPath 多层嵌套测试**
  - 测试 `buildPath` 方法处理 "a.b.c" 三层嵌套路径
  - 使用 mock 验证路径链式调用

- [ ] **Step 8: 添加 buildBlurryPredicate 空字段测试**
  - 测试 `buildBlurryPredicate` 处理空字段名（如 " , ,"）的场景
  - 验证返回 conjunction（全匹配的 predicate）

- [ ] **Step 9: 运行测试并验证覆盖率**

```bash
./gradlew :cartisan-data-jpa:test --tests ConditionSpecificationsTest
./gradlew :cartisan-data-jpa:jacocoTestReport
```

预期：ConditionSpecifications ≥ 85%

- [ ] **Step 10: 提交**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/specification/ConditionSpecificationsTest.java
git commit -m "test(jpa): add edge case and exception tests for ConditionSpecifications"
```

---

### Task 2: BaseRepositoryImpl - 软删除集成测试

**文件:**
- 创建: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImplIntegrationTest.java`

**目标:** BaseRepositoryImpl 覆盖率 ≥ 80%

**步骤:**

- [ ] **Step 1: 创建测试实体和基础设施**
  - 创建测试实体类 `TestSoftDeletableEntity` 实现 `SoftDeletable`
  - 创建测试实体类 `TestRegularEntity` 不实现 `SoftDeletable`
  - 设置 @DataJpaTest 测试类，注入 TestEntityManager 和 Repository

- [ ] **Step 2: 测试 deleteById 软删除**
  - 创建软删除实体并保存
  - 调用 deleteById
  - 验证 deleted 标记被设置为 true（非物理删除）

- [ ] **Step 3: 测试 deleteById 物理删除**
  - 创建常规实体并保存
  - 调用 deleteById
  - 验证实体被物理删除（不存在于数据库）

- [ ] **Step 4: 测试 delete 软删除分支（SoftDeletable 接口）**
  - 创建实现 SoftDeletable 接口的实体
  - 调用 delete(entity)
  - 验证 markAsDeleted() 被调用，deleted = true

- [ ] **Step 5: 测试 deleteAllById 混合场景**
  - 创建软删除和常规实体
  - 调用 deleteAllById
  - 验证软删除实体被标记，常规实体被物理删除

- [ ] **Step 6: 测试 deleteAll 软删除分支**
  - 创建多个软删除实体
  - 调用 deleteAll()
  - 验证批量更新 deleted 标记（非逐个删除）

- [ ] **Step 7: 测试 deleteAll 混合场景**
  - 创建软删除和常规实体混合列表
  - 调用 deleteAll(entities)
  - 验证软删除实体被标记保存，常规实体被物理删除

- [ ] **Step 8: 运行测试并验证覆盖率**

```bash
./gradlew :cartisan-data-jpa:test --tests BaseRepositoryImplIntegrationTest
./gradlew :cartisan-data-jpa:jacocoTestReport
```

预期：BaseRepositoryImpl ≥ 80%

- [ ] **Step 9: 提交**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImplIntegrationTest.java
git commit -m "test(jpa): add soft delete integration tests for BaseRepositoryImpl"
```

---

### Task 3: DomainEventPublisherHolder 单元测试

**文件:**
- 创建: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/DomainEventPublisherHolderTest.java`

**目标:** DomainEventPublisherHolder 覆盖率 100%

**步骤:**

- [ ] **Step 1: 测试 setPublisher 和 getPublisher**
  - 调用 setPublisher(mockPublisher)
  - 验证 getPublisher() 返回设置的 publisher

- [ ] **Step 2: 测试 setPublisher 拒绝 null**
  - 调用 setPublisher(null)
  - 验证抛出 NullPointerException，消息包含 "publisher cannot be null"

- [ ] **Step 3: 测试 getPublisher 初始返回 null**
  - 在未调用 setPublisher 的情况下
  - 验证 getPublisher() 返回 null

- [ ] **Step 4: 测试私有构造函数抛出异常**
  - 使用反射调用私有构造函数
  - 验证抛出 UnsupportedOperationException，消息包含 "Utility class"

- [ ] **Step 5: 运行测试并验证覆盖率**

```bash
./gradlew :cartisan-data-jpa:test --tests DomainEventPublisherHolderTest
./gradlew :cartisan-data-jpa:jacocoTestReport
```

预期：DomainEventPublisherHolder = 100%

- [ ] **Step 6: 提交**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/DomainEventPublisherHolderTest.java
git commit -m "test(jpa): add unit tests for DomainEventPublisherHolder"
```

---

### Task 4: 覆盖率验证

**步骤:**

- [ ] **Step 1: 运行完整测试套件**

```bash
./gradlew :cartisan-data-jpa:test
```

预期：所有测试通过

- [ ] **Step 2: 生成覆盖率报告**

```bash
./gradlew :cartisan-data-jpa:jacocoTestReport
```

- [ ] **Step 3: 验证覆盖率目标**

打开 `cartisan-data-jpa/build/reports/jacoco/test/html/index.html`

验证：
- cartisan-data-jpa 总体 ≥ 75%
- ConditionSpecifications ≥ 85%
- BaseRepositoryImpl ≥ 80%
- DomainEventPublisherHolder = 100%

- [ ] **Step 4: 最终提交**

```bash
git add docs/superpowers/plans/2026-03-24-cartisan-data-jpa-coverage-plan.md
git commit -m "docs: complete cartisan-data-jpa coverage improvement plan"
```

---

## 验收标准

- [ ] cartisan-data-jpa 总体覆盖率 ≥ 75%
- [ ] ConditionSpecifications ≥ 85%
- [ ] BaseRepositoryImpl ≥ 80%
- [ ] DomainEventPublisherHolder = 100%
- [ ] 所有测试通过
- [ ] 无测试警告
