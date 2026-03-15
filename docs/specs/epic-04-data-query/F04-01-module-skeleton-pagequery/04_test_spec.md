# F04-01 测试规格

## 测试概览

**特性**: F04-01 模块骨架 + PageQuery
**测试日期**: 2026-03-15
**测试框架**: JUnit 5 + AssertJ
**测试数量**: 10 个测试用例
**测试通过率**: 100% (10/10)

---

## 测试用例列表

| # | 测试方法 | 描述 | 预期结果 |
|---|---------|------|---------|
| 1 | `given_pageLessThanOne_when_construct_then_pageNormalizedToOne` | page=0 时构造 | page 修正为 1 |
| 2 | `given_negativePage_when_construct_then_pageNormalizedToOne` | page=-5 时构造 | page 修正为 1 |
| 3 | `given_sizeLessThanOne_when_construct_then_sizeNormalizedToTwenty` | size=0 时构造 | size 修正为 20 |
| 4 | `given_sizeGreaterThanMax_when_construct_then_sizeNormalizedToHundred` | size=150 时构造 | size 修正为 100 |
| 5 | `given_validPageAndSize_when_construct_then_valuesUnchanged` | page=2, size=50 时构造 | 值保持不变 |
| 6 | `given_boundaryValues_when_offset_then_returnZero` | page=1, size=1 时计算 offset | 返回 0 |
| 7 | `given_pageTwoSizeTwenty_when_offset_then_returnCorrectValue` | page=2, size=20 时计算 offset | 返回 20 |
| 8 | `given_pageThreeSizeTen_when_offset_then_returnCorrectValue` | page=3, size=10 时计算 offset | 返回 20 |
| 9 | `given_pageAndSize_when_of_then_returnPageQuery` | 调用 PageQuery.of(5, 30) | 返回正确实例 |
| 10 | `given_invalidParams_when_of_then_applyNormalization` | 调用 PageQuery.of(-1, 200) | 应用修正规则 |

---

## 验收测试映射

### AC1: 模块被 Gradle 识别
```bash
./gradlew :cartisan-data-query:build
# 结果: BUILD SUCCESSFUL
```

### AC2: PageQuery 使用 Record 实现
- 测试: `given_pageAndSize_when_of_then_returnPageQuery`
- 验证: PageQuery(5, 30).page() == 5, PageQuery(5, 30).size() == 30

### AC3: 参数校验和边界修正
| 测试 | 验证项 |
|------|--------|
| 测试 1, 2 | page < 1 → 1 |
| 测试 3 | size < 1 → 20 |
| 测试 4 | size > 100 → 100 |
| 测试 5 | 有效值保持不变 |

### AC4: offset() 方法正确计算
| 测试 | 输入 | 预期输出 |
|------|------|---------|
| 测试 6 | page=1, size=1 | 0 |
| 测试 7 | page=2, size=20 | 20 |
| 测试 8 | page=3, size=10 | 20 |

### AC5: of() 静态工厂方法
- 测试 9: 正常参数创建实例
- 测试 10: 非法参数触发修正

---

## 测试执行结果

```
testsuite name="PageQuery 分页查询参数测试"
  tests="10"
  skipped="0"
  failures="0"
  errors="0"
  timestamp="2026-03-15T09:40:51.976Z"
  time="0.043"
```

**状态**: ✅ 全部通过

---

## 覆盖率分析

### 代码覆盖
- **PageQuery 构造器**: 100% (所有分支覆盖)
- **offset() 方法**: 100% (边界值测试)
- **of() 方法**: 100%

### 边界值覆盖
- page 最小边界: 0, -5 → 1
- size 最小边界: 0 → 20
- size 最大边界: 150 → 100
- offset 零边界: (1-1)*1 = 0

---

## 代码审查结果

**审查时间**: 2026-03-15
**审查范围**: BASE_SHA(8537947) → HEAD_SHA(f5dffea)
**审查结果**: ✅ APPROVED

### 审查发现
- **Critical**: 0
- **Important**: 0
- **Minor**: 1 (jOOQ 版本从 3.19.15 更新到 3.19.29，这是兼容的升级)

### 审查意见
- TDD 方法论执行规范
- 测试命名符合 `given-when-then` 约定
- JavaDoc 完整且格式正确
- 架构符合 DDD 和六边形原则
- 代码质量高，无明显技术债

---

## PIT 变异测试

**状态**: N/A (cartisan-data-query 模块未配置 PIT)

**说明**: PIT 变异测试当前仅在 cartisan-core 模块配置。后续可根据需要扩展到其他模块。

---

## 构建验证

```bash
# 完整构建
./gradlew :cartisan-data-query:check
# 结果: BUILD SUCCESSFUL

# JavaDoc 生成
./gradlew :cartisan-data-query:javadoc
# 结果: BUILD SUCCESSFUL
```

---

## 归档状态

| 文档 | 状态 |
|------|------|
| 01_requirement.md | ✅ 已创建 |
| 02_interface.md | ✅ 已创建 |
| 03_implementation.md | ✅ 已创建 |
| 04_test_spec.md | ✅ 已创建 |
| DECISIONS.md | ✅ 已更新 (ADR-061~ADR-063) |
| 代码审查报告 | ✅ APPROVED |

---

## 完成确认

- [x] 所有测试通过 (10/10)
- [x] JavaDoc 编译成功
- [x] 代码审查通过
- [x] 规格文档齐全
- [x] 验收标准全部满足

**F04-01 状态**: ✅ **完成**
