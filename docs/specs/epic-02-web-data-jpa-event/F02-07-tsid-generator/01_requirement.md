# Feature: F02-07 TsidGenerator — 分布式 ID 生成器

> **版本**：v0.1 | **日期**：2026-03-14
> **来源**：设计文档 4.5 节（cartisan-data-jpa）

## 背景

在分布式系统中，使用数据库自增 ID 作为主键存在以下问题：
1. 需要先持久化才能获得 ID，违反「聚合根先有 ID 再持久化」的 DDD 原则
2. 分库分表场景下可能产生 ID 冲突
3. 数据库自增 ID 暴露了业务规模信息

传统 UUID（随机）虽然唯一但无序，不利于索引性能和范围查询。Snowflake 算法需要配置 workerId/dataCenterId，增加了部署复杂度。

## 目标

提供基于 TSID（Time-Sorted ID）算法的分布式 ID 生成器：
- **全局唯一**：分布式环境下保证唯一性
- **时间有序**：ID 按生成时间排序，支持索引优化和范围查询
- **零配置**：无需 workerId/dataCenterId 配置，即插即用
- **Long 类型**：与 JPA `@Id` + `Long` 类型无缝集成

## 范围

### 包含（In Scope）

- **TsidGenerator 类**：
  - `generate() → Long`：生成时间排序的全局唯一 ID
  - `toInstant(long tsid) → Instant`：从 ID 还原生成时间（便于排查）
- **TSID 算法自实现**：
  - 按 TSID 规范自行实现，不引入 tsid-creator 等第三方库
  - 时间戳部分（毫秒级，42 位）
  - 随机部分（保证同一毫秒内的唯一性，22 位）
  - 组合方式符合 TSID 规范
- **单元测试**：
  - 验证唯一性（批量生成 10000 个无重复）
  - 验证单调不递减（连续生成的 ID 不递减）
  - 验证时间戳可提取（从 ID 中还原生成时间）

### 不包含（Out of Scope）

| 不包含内容 | 原因 |
|-----------|------|
| 自定义前缀（如 `ORD-xxx`） | 业务展示层关注点，业务自行拼接 |
| workerId/dataCenterId 配置 | TSID 算法设计目标就是零配置 |
| 批量生成方法 `generate(int n)` | v1 不需要，要多个就多次调用 |
| 字符串 ID 返回 | 返回 Long 足够，业务可自行转换 |

## 验收标准（Acceptance Criteria）

### AC1：基本生成能力
- **given** TsidGenerator 实例
- **when** 调用 `generate()` 方法
- **then** 返回一个非 null 的 Long 值

### AC2：唯一性
- **given** TsidGenerator 实例
- **when** 连续生成 10000 个 ID
- **then** 所有 ID 互不相同

### AC3：时间戳部分不递减
- **given** TsidGenerator 实例
- **when** 连续生成多个 ID
- **then** 每个后续 ID 的时间戳部分 ≥ 前一个 ID 的时间戳部分
  - 即 `toInstant(id_n) >= toInstant(id_{n-1})`
  - 同一毫秒内生成的 ID，时间戳部分相等，但随机数部分可能乱序

### AC4：时间戳可提取
- **given** TsidGenerator 实例
- **when** 生成 ID 后调用 `toInstant(id)`
- **then** 返回的 Instant 与生成时间差值 < 1000ms

### AC5：并发安全
- **given** 10 个线程同时调用 TsidGenerator
- **when** 每个线程生成 1000 个 ID
- **then** 所有 10000 个 ID 互不相同

## 约束

### 性能约束
- 单线程生成速度 > 100万/秒
- 并发生成无性能瓶颈

### 兼容性约束
- 生成的 Long 值必须为正数（避免数据库主键约束问题）
- 兼容 Java 21

### 架构约束
- 位置：`cartisan-data-jpa` 模块
- 包路径：`com.cartisan.data.jpa.id`
- 零外部依赖：按 TSID 规范自行实现，不引入 tsid-creator 等第三方库

## 技术参考

- **TSID 规范**：https://github.com/f4b6a3/tsid-creator（仅参考格式规范，代码自实现）
- **TSID 格式**：
  ```
  |------ 42 位时间戳 ------|-- 22 位随机数 --|
  |    毫秒级（约 69 年）    |    同一毫秒内    |
  ```
- **总位数**：64 位（Long）
- **时间起点（epoch）**：`2024-01-01T00:00:00Z`（固定值）
- **实现方式**：在 cartisan-data-jpa 内按 TSID 规范自行实现，使用 JDK 标准库（java.time、ThreadLocalRandom、AtomicLong 等）
