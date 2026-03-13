# Feature: F01-10 — Fixture 工具 — 实施计划

> Epic: 01-core-and-test
> 版本：v0.1 | 日期：2026-03-14
> 状态：Phase 3 实施计划

---

## 目标复述

为 cartisan-test 模块新增 Fixture 工具包，包含：
1. 随机数据生成器：FixtureStrings、FixtureNumbers、FixtureDates
2. 种子管理：FixtureSeeds（支持全局种子设置，测试可重复）
3. 对象构建器：FixtureBuilder（支持 POJO/JPA 实体，递归深度限制）
4. 自定义异常：FixtureBuildException

按职责拆分类，一期仅支持 POJO/JPA 实体（Record 留待后续）。

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `cartisan-test/src/main/java/com/cartisan/test/fixture/package-info.java` | 包文档 |
| 新增 | `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureStrings.java` | 字符串随机生成 |
| 新增 | `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureNumbers.java` | 数字随机生成 |
| 新增 | `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureDates.java` | 日期随机生成 |
| 新增 | `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureSeeds.java` | 种子管理 |
| 新增 | `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureBuilder.java` | 对象构建器 |
| 新增 | `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureBuildException.java` | 自定义异常 |
| 新增 | `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureStringsTest.java` | 单元测试 |
| 新增 | `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureNumbersTest.java` | 单元测试 |
| 新增 | `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureDatesTest.java` | 单元测试 |
| 新增 | `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureSeedsTest.java` | 单元测试 |
| 新增 | `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureBuilderTest.java` | 单元测试 |

---

## 核心流程（伪代码）

```
1. 创建 FixtureBuildException
   - 继承 RuntimeException
   - 两个构造器：(String message), (String message, Throwable cause)

2. 创建 FixtureSeeds
   - AtomicReference<Long> GLOBAL_SEED（null = 未设置）
   - ThreadLocal<Random> RANDOM_CACHE
   - setGlobalSeed(seed): 设置种子 + 清空缓存
   - resetSeed(): 清空种子 + 清空缓存
   - currentRandom(): 包可见，返回 Random 实例

3. 创建 FixtureStrings
   - 所有方法通过 FixtureSeeds.currentRandom() 获取 Random
   - randomString(): 生成 12 位字母数字
   - randomString(length): 指定长度
   - randomString(prefix): prefix + 12 位随机
   - randomString(prefix, length): prefix + length 位随机
   - randomEmail(): randomString() + "@test.local"
   - randomUuid(): UUID 去连字符
   - randomPhoneNumber(): "1" + 11 位随机数字

4. 创建 FixtureNumbers
   - 所有方法通过 FixtureSeeds.currentRandom() 获取 Random
   - randomInt(): Random.nextInt()
   - randomInt(min, max): min + Random.nextInt(max - min + 1)
   - randomLong(): Random.nextLong() 取绝对值
   - randomId(): randomLong(1, Long.MAX_VALUE)
   - randomAmount(): BigDecimal(随机 double, scale=2)
   - randomDecimal(min, max, scale): BigDecimal(范围, scale)

5. 创建 FixtureDates
   - now(): LocalDateTime.now()
   - today(): LocalDate.now()
   - pastDays(n): now().minusDays(n)
   - futureDays(n): now().plusDays(n)
   - pastDays(base, n): base.minusDays(n)
   - futureDays(base, n): base.plusDays(n)

6. 创建 FixtureBuilder
   - of(clazz): 验证无参构造，检测 Record 拒绝
   - with(fieldName, value): 存入覆盖映射
   - maxDepth(depth): 设置最大深度
   - build():
       a. clazz.getDeclaredConstructor().newInstance()
       b. 递归填充字段（深度 < maxDepth）
       c. 应用 with() 覆盖
       d. 失败时抛 FixtureBuildException

7. 编写单元测试
   - 每个 Fixture 类的测试
   - FixtureSeeds 的种子可重复性测试
   - FixtureBuilder 的各种场景测试
```

---

## 原子任务清单

### Step 1: 创建 fixture 包结构

- **文件**:
  - `cartisan-test/src/main/java/com/cartisan/test/fixture/package-info.java`
  - `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureBuildException.java`
- **内容**:
  - package-info.java: 包文档说明
  - FixtureBuildException: 继承 RuntimeException，两个构造器
- **验证**: 编译通过

---

### Step 2: 创建 FixtureSeeds 骨架

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureSeeds.java`
- **内容**:
  - `private static final AtomicReference<Long> GLOBAL_SEED = new AtomicReference<>()`
  - `private static final ThreadLocal<Random> RANDOM_CACHE = ThreadLocal.withInitial(...)`
  - `public static void setGlobalSeed(long seed)`
  - `public static void resetSeed()`
  - `static Random currentRandom()`（包可见）
- **验证**: 编译通过

---

### Step 3: 编写 FixtureSeeds 测试（红灯）

- **文件**: `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureSeedsTest.java`
- **内容**:
  - `shouldUseSameSeed_whenSetGlobalSeed()` — 验证同一种子序列可重复
  - `shouldResetSeed_whenResetSeedCalled()` — 验证 reset 后恢复随机
  - `shouldBeThreadSafe_whenMultipleThreadsAccess()` — 验证线程安全
- **验证**: 编译通过，测试全红（方法不存在）

---

### Step 4: 实现 FixtureSeeds（绿灯）

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureSeeds.java`
- **内容**: 实现 Step 2 中的所有方法
- **验证**: 编译通过 + Step 3 测试全绿

---

### Step 5: 创建 FixtureStrings 骨架

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureStrings.java`
- **内容**:
  - `private static final int DEFAULT_LENGTH = 12`
  - `private static final String ALPHANUMERIC = "..."`（字母数字字符集）
  - 所有 random*() 方法签名（返回空字符串或固定值）
- **验证**: 编译通过

---

### Step 6: 编写 FixtureStrings 测试（红灯）

- **文件**: `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureStringsTest.java`
- **内容**:
  - `shouldReturnDefaultLength_whenRandomString()` — 验证默认长度 12
  - `shouldReturnSpecifiedLength_whenRandomStringWithLength()` — 验证指定长度
  - `shouldStartWithPrefix_whenRandomStringWithPrefix()` — 验证前缀
  - `shouldHaveValidFormat_whenRandomEmail()` — 验证 Email 格式
  - `shouldHave32Chars_whenRandomUuid()` — 验证 UUID 32 位
  - `shouldStartWith1_whenRandomPhoneNumber()` — 验证手机号 1 开头
- **验证**: 编译通过，测试全红

---

### Step 7: 实现 FixtureStrings（绿灯）

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureStrings.java`
- **内容**: 实现 Step 5 中的所有方法，使用 `FixtureSeeds.currentRandom()`
- **验证**: 编译通过 + Step 6 测试全绿

---

### Step 8: 创建 FixtureNumbers 骨架

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureNumbers.java`
- **内容**: 所有 random*() 方法签名（返回 0 或固定值）
- **验证**: 编译通过

---

### Step 9: 编写 FixtureNumbers 测试（红灯）

- **文件**: `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureNumbersTest.java`
- **内容**:
  - `shouldReturnPositive_whenRandomInt()` — 验证正数
  - `shouldReturnInRange_whenRandomIntWithRange()` — 验证范围边界
  - `shouldReturnPositiveId_whenRandomId()` — 验证 ID >= 1
  - `shouldHaveScale2_whenRandomAmount()` — 验证金额 2 位小数
  - `shouldReturnInDecimalRange_whenRandomDecimal()` — 验证 BigDecimal 范围
- **验证**: 编译通过，测试全红

---

### Step 10: 实现 FixtureNumbers（绿灯）

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureNumbers.java`
- **内容**: 实现 Step 8 中的所有方法
- **验证**: 编译通过 + Step 9 测试全绿

---

### Step 11: 创建 FixtureDates 骨架

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureDates.java`
- **内容**: 所有 now/today/pastDays/futureDays 方法签名
- **验证**: 编译通过

---

### Step 12: 编写 FixtureDates 测试（红灯）

- **文件**: `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureDatesTest.java`
- **内容**:
  - `shouldReturnCurrentTime_whenNow()` — 验证当前时间
  - `shouldReturnPastTime_whenPastDays()` — 验证过去时间
  - `shouldReturnFutureTime_whenFutureDays()` — 验证未来时间
  - `shouldRelativeTime_whenPastDaysWithBase()` — 验证相对时间
- **验证**: 编译通过，测试全红

---

### Step 13: 实现 FixtureDates（绿灯）

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureDates.java`
- **内容**: 实现 Step 11 中的所有方法
- **验证**: 编译通过 + Step 12 测试全绿

---

### Step 14: 创建 FixtureBuilder 骨架

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureBuilder.java`
- **内容**:
  - `private final Class<T> clazz`
  - `private final Map<String, Object> overrides`
  - `private int maxDepth = 3`
  - `private FixtureBuilder(Class<T> clazz)`（私有构造）
  - `public static <T> FixtureBuilder<T> of(Class<T> clazz)`（骨架实现）
  - `public FixtureBuilder<T> with(String fieldName, Object value)`
  - `public FixtureBuilder<T> maxDepth(int depth)`
  - `public T build()`（返回 null）
- **验证**: 编译通过

---

### Step 15: 编写 FixtureBuilder 基础测试（红灯）

- **文件**: `cartisan-test/src/test/java/com/cartisan/test/fixture/FixtureBuilderTest.java`
- **内容**:
  - `shouldBuild_whenClassHasNoArgConstructor()` — 验证正常构建
  - `shouldThrowException_whenClassIsRecord()` — 验证 Record 拒绝
  - `shouldThrowException_whenClassHasNoNoArgConstructor()` — 验证无参构造检测
  - `shouldOverrideField_whenWithCalled()` — 验证字段覆盖
  - `shouldThrowException_whenFieldNotExist()` — 验证字段不存在异常
- **验证**: 编译通过，测试全红

---

### Step 16: 实现 FixtureBuilder 基础功能（绿灯）

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureBuilder.java`
- **内容**:
  - `of()`: 验证无参构造，检测 Record
  - `build()`: 创建实例，填充基本字段
  - `with()`: 存入覆盖映射
- **验证**: 编译通过 + Step 15 测试全绿

---

### Step 17: 编写 FixtureBuilder 递归测试（红灯）

- **文件**: 扩展 `FixtureBuilderTest.java`
- **内容**:
  - 测试嵌套对象（Order → Customer）
  - `shouldLimitDepth_whenMaxDepthSet()` — 验证深度限制
  - `shouldReturnNull_whenDepthExceededForObject()` — 验证超深对象为 null
  - `shouldReturnDefaultValue_whenDepthExceededForPrimitive()` — 验证超深基本类型为默认值
- **验证**: 编译通过，测试全红

---

### Step 18: 实现 FixtureBuilder 递归填充（绿灯）

- **文件**: `cartisan-test/src/main/java/com/cartisan/test/fixture/FixtureBuilder.java`
- **内容**:
  - 递归填充字段逻辑
  - 深度限制逻辑
  - 类型映射（String→randomString, BigDecimal→randomAmount, etc.）
- **验证**: 编译通过 + Step 17 测试全绿

---

### Step 19: 全量验证

- **命令**:
  - `./gradlew :cartisan-test:compileJava`
  - `./gradlew :cartisan-test:test`
  - `./gradlew :cartisan-test:check`
- **验证**: 全部通过

---

## 任务规模评估

| Step | 预估代码行数 | 预估时间 |
|------|-------------|---------|
| Step 1 | ~20 行 | 5 分钟 |
| Step 2 | ~30 行 | 10 分钟 |
| Step 3 | ~60 行 | 15 分钟 |
| Step 4 | ~20 行 | 10 分钟 |
| Step 5 | ~40 行 | 5 分钟 |
| Step 6 | ~80 行 | 15 分钟 |
| Step 7 | ~60 行 | 15 分钟 |
| Step 8 | ~30 行 | 5 分钟 |
| Step 9 | ~80 行 | 15 分钟 |
| Step 10 | ~70 行 | 15 分钟 |
| Step 11 | ~30 行 | 5 分钟 |
| Step 12 | ~60 行 | 10 分钟 |
| Step 13 | ~40 行 | 10 分钟 |
| Step 14 | ~50 行 | 10 分钟 |
| Step 15 | ~80 行 | 15 分钟 |
| Step 16 | ~80 行 | 20 分钟 |
| Step 17 | ~60 行 | 10 分钟 |
| Step 18 | ~100 行 | 25 分钟 |
| Step 19 | 验证 | 10 分钟 |
| **总计** | **~1005 行** | **~225 分钟（约 0.5 天）** |

---

## AC 映射

| AC | 对应 Step |
|----|----------|
| AC1: FixtureStrings 随机字符串生成 | Step 5, Step 6, Step 7 |
| AC2: FixtureNumbers 随机数字生成 | Step 8, Step 9, Step 10 |
| AC3: FixtureDates 日期生成 | Step 11, Step 12, Step 13 |
| AC4: FixtureSeeds 种子管理 | Step 2, Step 3, Step 4 |
| AC5: FixtureBuilder 基本构建 | Step 14, Step 15, Step 16 |
| AC6: FixtureBuilder 字段覆盖 | Step 15, Step 16 |
| AC7: FixtureBuilder 递归深度限制 | Step 17, Step 18 |
| AC8: FixtureBuildException | Step 1 |
| AC9: 单元测试覆盖 | Step 3, 6, 9, 12, 15, 17 |

---

## 参考资料

- 需求文档: `01_requirement.md`
- 接口设计: `02_interface.md`
- JDK 文档: java.util.Random, java.lang.reflect, java.time
