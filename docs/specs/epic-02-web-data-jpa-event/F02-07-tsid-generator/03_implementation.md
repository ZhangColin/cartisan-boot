# Feature: F02-07 TsidGenerator — 实施计划

> **版本**：v0.1 | **日期**：2026-03-14
> **规范参考**：[AI协作开发SOP.md](../../../../sop/AI协作开发SOP.md) Phase 3

## 目标复述

实现基于 TSID（Time-Sorted ID）算法的分布式 ID 生成器：
- `TsidGenerator` 类：`generate() → long` 生成时间排序的全局唯一 ID
- `toInstant(long tsid) → Instant`：从 ID 还原生成时间
- 零外部依赖，仅使用 JDK 标准库
- 满足 5 条验收标准（AC1-AC5）

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 创建 | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/id/TsidGenerator.java` | 主类 |
| 创建 | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/id/package-info.java` | 包文档 |
| 创建 | `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/id/TsidGeneratorTest.java` | 单元测试 |

---

## 核心流程（伪代码）

### generate() 流程
```
1. 获取当前毫秒时间戳（相对于 2024-01-01 epoch）
2. 时间戳部分按 42 位掩码截断（& 0x3FFFFFFFFFFL）
3. 生成 22 位随机数（ThreadLocalRandom.nextInt(0, MAX_RANDOM + 1)）
4. 组合：(timestampPart << 22) | random
5. 返回 tsid
```

### toInstant(long) 流程
```
1. 校验 tsid > 0
2. 提取时间戳部分：tsid >>> 22
3. 加上 epoch，转换为 Instant
4. 校验结果不超过当前时间 + 1 天
5. 返回 Instant
```

---

## 原子任务清单

### Step 1: 创建包结构和 TsidGenerator 类骨架

- **文件**：`cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/id/TsidGenerator.java`
- **内容**：
  - 声明 `public final class TsidGenerator`
  - 定义常量：`TIMESTAMP_BITS = 42`、`RANDOM_BITS = 22`、`MAX_RANDOM`、`EPOCH`
  - 私有构造函数
  - 工厂方法：`newInstance()`、`withRandom(Random)`
  - 存储字段：`private final Random random`
- **验证**：编译通过 `./gradlew :cartisan-data-jpa:compileJava`

### Step 2: 编写 AC1 基本生成能力测试（红灯）

- **文件**：`cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/id/TsidGeneratorTest.java`
- **测试方法**：`given_generator_when_generate_then_returnsNonNullPositiveLong()`
- **预期**：编译通过，测试失败（`NoSuchMethodError` 或 `ClassNotFound`）
- **验证**：
  ```bash
  ./gradlew :cartisan-data-jpa:compileTestJava
  ./gradlew :cartisan-data-jpa:test --tests TsidGeneratorTest.given_generator_when_generate_then_returnsNonNullPositiveLong
  ```
  预期：测试红灯

### Step 3: 实现 generate() 方法（绿灯）

- **文件**：`TsidGenerator.java`
- **内容**：实现 `generate()` 方法
  ```java
  public long generate() {
      long currentMillis = System.currentTimeMillis() - EPOCH_MILLIS;
      long timestampPart = currentMillis & 0x3FFFFFFFFFFL;
      int random = random.nextInt(MAX_RANDOM + 1);
      return (timestampPart << RANDOM_BITS) | random;
  }
  ```
- **验证**：测试绿灯
  ```bash
  ./gradlew :cartisan-data-jpa:test --tests TsidGeneratorTest.given_generator_when_generate_then_returnsNonNullPositiveLong
  ```

### Step 4: 编写 AC2 唯一性测试（红灯）

- **测试方法**：`given_generator_when_generate10000_then_allUnique()`
- **内容**：循环生成 10000 个 ID，放入 Set，断言 size = 10000
- **预期**：测试红灯（方法未实现或 Set 断言失败）
- **验证**：`./gradlew :cartisan-data-jpa:test --tests TsidGeneratorTest.given_generator_when_generate10000_then_allUnique`

### Step 5: 优化生成逻辑确保唯一性（绿灯）

- **文件**：`TsidGenerator.java`
- **内容**：如测试失败，调整随机数生成策略（v1 使用 ThreadLocalRandom 应足够）
- **验证**：测试绿灯

### Step 6: 编写 AC3 时间戳部分不递减测试（红灯）

- **测试方法**：`given_generator_when_generateMultiple_then_timestampPartNonDecreasing()`
- **内容**：生成 1000 个 ID，提取每个的时间戳部分，断言 `current >= previous`
- **预期**：测试红灯（`toInstant()` 方法未实现）
- **验证**：`./gradlew :cartisan-data-jpa:test --tests TsidGeneratorTest.given_generator_when_generateMultiple_then_timestampPartNonDecreasing`

### Step 7: 编写 AC4 时间戳可提取测试（红灯）

- **测试方法**：`given_tsid_when_toInstant_then_withinOneMillisecond()`
- **内容**：生成 ID，调用 `toInstant()`，与 `Instant.now()` 比较差值 < 1000ms
- **预期**：测试红灯（`toInstant()` 方法未实现）
- **验证**：`./gradlew :cartisan-data-jpa:test --tests TsidGeneratorTest.given_tsid_when_toInstant_then_withinOneMillisecond`

### Step 8: 实现 toInstant() 方法（绿灯）

- **文件**：`TsidGenerator.java`
- **内容**：实现 `toInstant(long tsid)` 方法
  ```java
  public Instant toInstant(long tsid) {
      if (tsid <= 0) {
          throw new IllegalArgumentException("TSID must be positive: " + tsid);
      }
      long timestampMillis = (tsid >>> RANDOM_BITS) + EPOCH_MILLIS;
      Instant result = Instant.ofEpochMilli(timestampMillis);
      if (result.isAfter(Instant.now().plus(Duration.ofDays(1)))) {
          throw new IllegalArgumentException("TSID timestamp is too far in the future: " + tsid);
      }
      return result;
  }
  ```
- **验证**：AC3、AC4 测试绿灯

### Step 9: 编写 AC5 并发安全测试（红灯）

- **测试方法**：`given_concurrentGeneration_when_10Threads1000Each_then_allUnique()`
- **内容**：使用 `ExecutorService` 启动 10 个线程，每个生成 1000 个 ID，收集到 `ConcurrentHashMap` 或同步 Set，断言唯一性
- **预期**：测试红灯（如并发问题导致重复）
- **验证**：`./gradlew :cartisan-data-jpa:test --tests TsidGeneratorTest.given_concurrentGeneration_when_10Threads1000Each_then_allUnique`

### Step 10: 验证并发安全（绿灯）

- **文件**：`TsidGenerator.java`
- **内容**：确认使用 `ThreadLocalRandom`（线程安全），无需额外修改
- **验证**：测试绿灯

### Step 11: 编写参数校验测试（红灯）

- **测试方法**：
  - `given_zeroTsid_when_toInstant_then_throwsIllegalArgumentException()`
  - `given_negativeTsid_when_toInstant_then_throwsIllegalArgumentException()`
  - `given_futureTsid_when_toInstant_then_throwsIllegalArgumentException()`
- **预期**：测试红灯（校验逻辑未实现或异常消息不匹配）
- **验证**：`./gradlew :cartisan-data-jpa:test --tests "*Invalid*"`

### Step 12: 完善参数校验（绿灯）

- **文件**：`TsidGenerator.java`
- **内容**：确保 `toInstant()` 的校验逻辑完整，异常消息清晰
- **验证**：所有测试绿灯

### Step 13: 全量验证

- **运行**：
  ```bash
  ./gradlew :cartisan-data-jpa:test
  ./gradlew :cartisan-data-jpa:check
  ```
- **预期**：所有测试绿灯，ArchUnit 通过

### Step 14: 添加 JavaDoc

- **文件**：`TsidGenerator.java`
- **内容**：为公共方法和常量添加完整的 JavaDoc（遵循 STYLE-001）
- **验证**：`./gradlew :cartisan-data-jpa:javadoc`

---

## 代码量预估

| 文件 | 预估行数 |
|------|---------|
| `TsidGenerator.java` | 60-80 行 |
| `TsidGeneratorTest.java` | 150-200 行 |
| `package-info.java` | 10-20 行 |
| **总计** | **220-300 行** |

---

## 依赖的外部文档

- [01_requirement.md](./01_requirement.md) — 验收标准
- [02_interface.md](./02_interface.md) — 接口契约
- [SKILL.md](../../../../skills/SKILL.md) — 测试命名规则（TEST-002）、断言工具（TEST-001）
