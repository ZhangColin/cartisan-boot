# Feature: F01-10 — Fixture 工具 — 接口契约

> Epic: 01-core-and-test
> 版本：v0.1 | 日期：2026-03-14
> 状态：Phase 2 接口设计

---

## 1. 类设计

### 1.1 类清单

| 类名 | 职责 | 类型 | 包路径 |
|------|------|------|--------|
| **FixtureStrings** | 字符串随机生成 | Final 工具类 | `com.cartisan.test.fixture` |
| **FixtureNumbers** | 数字随机生成 | Final 工具类 | `com.cartisan.test.fixture` |
| **FixtureDates** | 日期随机生成 | Final 工具类 | `com.cartisan.test.fixture` |
| **FixtureSeeds** | 全局种子管理 | Final 工具类 | `com.cartisan.test.fixture` |
| **FixtureBuilder<T>** | 泛型对象构建器 | Final 类 | `com.cartisan.test.fixture` |
| **FixtureBuildException** | 自定义异常 | Exception | `com.cartisan.test.fixture` |

### 1.2 依赖关系

```
FixtureStrings / FixtureNumbers / FixtureDates
    └── JDK: java.util.Random (通过 FixtureSeeds.currentRandom())
    └── JDK: java.math.BigDecimal
    └── JDK: java.time.*

FixtureSeeds
    └── JDK: java.util.Random
    └── JDK: java.util.concurrent.atomic.AtomicReference
    └── JDK: java.lang.ThreadLocal

FixtureBuilder<T>
    └── JDK: java.lang.reflect (Constructor, Field, Modifier)
    └── 本包: FixtureStrings, FixtureNumbers, FixtureDates
    └── 本包: FixtureBuildException

FixtureBuildException
    └── JDK: java.lang.RuntimeException
```

**关键约束：**
- 零依赖 Spring / JPA / 其他第三方库
- 仅依赖 JDK 标准库和 cartisan-core（通过反射不需要）

---

## 2. 方法签名描述（伪代码）

### 2.1 FixtureStrings

#### randomString() → String

**描述：** 生成 12 位随机字母数字字符串

**前置条件：** 无

**后置条件：** 返回长度为 12 的字符串，字符范围为 `[a-zA-Z0-9]`

**伪代码：**
```
randomString():
    return generateRandomString(12, ALPHANUMERIC)
```

---

#### randomString(int length) → String

**描述：** 生成指定位数的随机字母数字字符串

**前置条件：** `length > 0`

**后置条件：** 返回长度为 `length` 的字符串

---

#### randomString(String prefix) → String

**描述：** 生成带前缀的随机字符串

**前置条件：** `prefix` 非空

**后置条件：** 返回 `prefix + 12位随机字符`

---

#### randomString(String prefix, int length) → String

**描述：** 生成带前缀的随机字符串

**前置条件：**
- `prefix` 非空
- `length > 0`

**后置条件：** 返回 `prefix + length位随机字符`

**设计说明：** `length` 是随机部分的长度，不含前缀

---

#### randomEmail() → String

**描述：** 生成随机 Email 地址

**前置条件：** 无

**后置条件：** 返回格式为 `{randomString()}@test.local` 的字符串

---

#### randomUuid() → String

**描述：** 生成随机 UUID 字符串（无连字符）

**前置条件：** 无

**后置条件：** 返回 32 位十六进制字符串

**伪代码：**
```
randomUuid():
    return UUID.randomUUID().toString().replace("-", "")
```

---

#### randomPhoneNumber() → String

**描述：** 生成随机手机号

**前置条件：** 无

**后置条件：** 返回 `1` 开头的 12 位数字字符串

**伪代码：**
```
randomPhoneNumber():
    return "1" + generateRandomDigits(11)
```

---

### 2.2 FixtureNumbers

#### randomInt() → int

**描述：** 生成随机正整数

**前置条件：** 无

**后置条件：** 返回 `[0, Integer.MAX_VALUE]` 范围内的整数

---

#### randomInt(int min, int max) → int

**描述：** 生成指定范围内的随机整数

**前置条件：**
- `min >= 0`
- `max > min`

**后置条件：** 返回 `[min, max]` 范围内的整数

---

#### randomLong() → long

**描述：** 生成随机正长整数

**前置条件：** 无

**后置条件：** 返回 `[0, Long.MAX_VALUE]` 范围内的长整数

---

#### randomLong(long min, long max) → long

**描述：** 生成指定范围内的随机长整数

**前置条件：**
- `min >= 0`
- `max > min`

**后置条件：** 返回 `[min, max]` 范围内的长整数

---

#### randomId() → long

**描述：** 生成随机正数 ID

**前置条件：** 无

**后置条件：** 返回 `[1, Long.MAX_VALUE]` 范围内的长整数

**伪代码：**
```
randomId():
    return randomLong(1, Long.MAX_VALUE)
```

---

#### randomAmount() → BigDecimal

**描述：** 生成随机金额

**前置条件：** 无

**后置条件：** 返回 2 位小数的 BigDecimal，范围 `(0, 1000000]`

**伪代码：**
```
randomAmount():
    value = randomDouble(0.01, 1000000.00)
    return BigDecimal.valueOf(value).setScale(2, HALF_UP)
```

---

#### randomDecimal(BigDecimal min, BigDecimal max, int scale) → BigDecimal

**描述：** 生成指定范围和精度的随机 BigDecimal

**前置条件：**
- `min < max`
- `scale >= 0`

**后置条件：** 返回 `[min, max]` 范围内，精度为 `scale` 的 BigDecimal

---

### 2.3 FixtureDates

#### now() → LocalDateTime

**描述：** 获取当前时间

**前置条件：** 无

**后置条件：** 返回当前系统时间的 LocalDateTime

---

#### today() → LocalDate

**描述：** 获取当前日期

**前置条件：** 无

**后置条件：** 返回当前系统日期的 LocalDate

---

#### pastDays(int days) → LocalDateTime

**描述：** 获取过去 N 天的时间

**前置条件：** `days >= 0`

**后置条件：** 返回 `当前时间 - days 天`

**伪代码：**
```
pastDays(days):
    return now().minusDays(days)
```

---

#### futureDays(int days) → LocalDateTime

**描述：** 获取未来 N 天的时间

**前置条件：** `days >= 0`

**后置条件：** 返回 `当前时间 + days 天`

**伪代码：**
```
futureDays(days):
    return now().plusDays(days)
```

---

#### pastDays(LocalDateTime base, int days) → LocalDateTime

**描述：** 获取基准时间的过去 N 天

**前置条件：**
- `base` 非空
- `days >= 0`

**后置条件：** 返回 `base - days 天`

---

#### futureDays(LocalDateTime base, int days) → LocalDateTime

**描述：** 获取基准时间的未来 N 天

**前置条件：**
- `base` 非空
- `days >= 0`

**后置条件：** 返回 `base + days 天`

---

### 2.4 FixtureSeeds

#### setGlobalSeed(long seed) → void

**描述：** 设置全局随机种子

**前置条件：** 无

**后置条件：**
- `GLOBAL_SEED.set(seed)`
- 清空 ThreadLocal 缓存
- 后续新创建的 Random 实例都会使用该种子

**伪代码：**
```
setGlobalSeed(seed):
    GLOBAL_SEED.set(seed)
    RANDOM_CACHE.remove()
```

---

#### resetSeed() → void

**描述：** 重置为随机种子

**前置条件：** 无

**后置条件：**
- `GLOBAL_SEED.set(null)`
- 清空 ThreadLocal 缓存
- 后续新创建的 Random 实例使用随机种子

**伪代码：**
```
resetSeed():
    GLOBAL_SEED.set(null)
    RANDOM_CACHE.remove()
```

---

#### currentRandom() → Random (包可见)

**描述：** 获取当前线程的 Random 实例

**前置条件：** 无

**后置条件：** 返回 ThreadLocal 缓存的 Random 实例

**伪代码：**
```
currentRandom():
    seed = GLOBAL_SEED.get()
    if seed != null:
        return new Random(seed)
    return new Random()
```

**设计说明：** 包可见，仅限同包的 FixtureStrings/Numbers/Dates 使用

---

### 2.5 FixtureBuilder<T>

#### of(Class<T> clazz) → FixtureBuilder<T>

**描述：** 创建指定类的 FixtureBuilder

**前置条件：**
- `clazz` 非空
- `clazz` 有无参构造
- `clazz` 不是 Record 类型

**后置条件：** 返回 FixtureBuilder 实例

**异常：**
- `FixtureBuildException` — 无无参构造或为 Record 类型

**伪代码：**
```
of(clazz):
    if clazz.isRecord():
        throw FixtureBuildException("Record types not supported yet")
    try:
        clazz.getDeclaredConstructor() // 验证无参构造存在
    catch NoSuchMethodException:
        throw FixtureBuildException("has no no-arg constructor")
    return new FixtureBuilder<>(clazz, maxDepth=3)
```

---

#### with(String fieldName, Object value) → FixtureBuilder<T>

**描述：** 设置字段覆盖值

**前置条件：** `fieldName` 非空

**后置条件：** 将 `fieldName: value` 存入覆盖映射

**异常：** 无（延迟到 build() 验证字段存在）

---

#### maxDepth(int depth) → FixtureBuilder<T>

**描述：** 设置递归填充的最大深度

**前置条件：** `depth > 0`

**后置条件：** 更新最大深度设置

---

#### build() → T

**描述：** 构建对象实例

**前置条件：** 已通过 `of()` 创建

**后置条件：** 返回填充后的对象实例

**异常：**
- `FixtureBuildException` — 字段不存在、setter 失败、实例创建失败

**伪代码：**
```
build():
    try:
        instance = clazz.getDeclaredConstructor().newInstance()
        fillFields(instance, currentDepth=0)
        applyOverrides(instance)
        return instance
    catch Exception e:
        throw FixtureBuildException("Failed to build", e)

fillFields(obj, depth):
    if depth >= maxDepth:
        setDefaultValues(obj)  // 对象=null，基本类型=默认值
        return
    for each field in obj.declaredFields:
        if field in overrides:
            continue
        value = generateValueByType(field.type, depth + 1)
        setFieldValue(obj, field, value)

generateValueByType(type, depth):
    switch type:
        case int, long: return 0
        case boolean: return false
        case String: return FixtureStrings.randomString()
        case BigDecimal: return FixtureNumbers.randomAmount()
        case LocalDateTime: return FixtureDates.now()
        case custom type: return FixtureBuilder.of(type).maxDepth(maxDepth - depth).build()
        case Collection, Map: return empty instance
        default: return null
```

---

### 2.6 FixtureBuildException

#### FixtureBuildException(String message)

**描述：** 创建仅含消息的异常

#### FixtureBuildException(String message, Throwable cause)

**描述：** 创建含消息和原因的异常

---

## 3. 常量定义

### 3.1 FixtureStrings 常量

| 常量 | 值 | 说明 |
|------|-----|------|
| DEFAULT_LENGTH | 12 | 默认随机字符串长度 |
| ALPHANUMERIC | "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" | 字母数字字符集 |
| EMAIL_DOMAIN | "test.local" | 测试 Email 域名 |

### 3.2 FixtureNumbers 常量

| 常量 | 值 | 说明 |
|------|-----|------|
| DEFAULT_AMOUNT_SCALE | 2 | 金额默认小数位数 |
| DEFAULT_AMOUNT_MAX | 1000000 | 金额默认上限 |

### 3.3 FixtureBuilder 常量

| 常量 | 值 | 说明 |
|------|-----|------|
| DEFAULT_MAX_DEPTH | 3 | 默认最大递归深度 |

---

## 4. 异常处理

| 场景 | 抛出异常 | 错误消息格式 |
|------|---------|-------------|
| 无无参构造 | `FixtureBuildException` | `"{className} has no no-arg constructor"` |
| Record 类型 | `FixtureBuildException` | `"Record types not supported yet: {className}"` |
| 字段不存在 | `FixtureBuildException` | `"Field '{fieldName}' not found in {className}"` |
| setter 失败 | `FixtureBuildException` | `"Failed to set field '{fieldName}': {cause.message}"`, 含 cause |
| 实例创建失败 | `FixtureBuildException` | `"Failed to create instance: {cause.message}"`, 含 cause |

---

## 5. 设计决策

### 决策 1：按职责拆分 Fixture 类

**选择：** 拆分为 FixtureStrings/Numbers/Dates 三个专用类

**理由：**
- 单一职责原则，每个类职责清晰
- 避免单一类静态方法过多，维护困难
- 方法命名可简化：`randomString()` 而非 `Fixture.randomString()`
- 符合常见测试工具（如 Apache Commons Lang）的惯例

**备选方案（未采纳）：**
- 单一 Fixture 类 → 方法过多，维护不便

---

### 决策 2：一期仅支持 POJO/JPA 实体

**选择：** FixtureBuilder 仅支持无参构造 + setter 的普通类

**理由：**
- POJO/JPA 实体是最常见的测试数据类型
- 通过反射设置字段，实现简单
- Record 需要解析 RecordComponents 和调用 canonical 构造函数，复杂度高

**备选方案（未采纳）：**
- 同时支持 Record → 增加实现复杂度，测试边界情况多

---

### 决策 3：种子保留到 resetSeed()

**选择：** 种子值一直保留，直到调用 `resetSeed()` 清空

**理由：**
- 支持多线程测试场景，每个线程创建的 Random 都使用相同种子
- 用户可显式控制种子生命周期

**备选方案（未采纳）：**
- 使用一次后清空 → 只有第一个线程得到确定性，多线程测试无法整体复现

---

### 决策 4：使用 AtomicReference<Long> 表示种子

**选择：** `AtomicReference<Long>`，null 表示未设置

**理由：**
- 语义清晰，null 直观表示"未设置"
- 读写和清空操作都简单

**备选方案（未采纳）：**
- `AtomicLong` + 非法值（如 Long.MIN_VALUE） → 需要额外文档说明

---

### 决策 5：递归深度限制策略

**选择：** 最大深度 3，超深时对象=null，基本类型=默认值

**理由：**
- 防止循环引用导致栈溢出
- 大多数嵌套场景深度不超过 3
- 超深时返回 null/默认值，测试可明确感知边界

---

### 决策 6：FixtureBuildException 继承 RuntimeException

**选择：** 非受检异常

**理由：**
- 测试代码中无需到处写 try-catch
- 构建失败属于异常情况，应该中断测试

---

## 6. 变更范围

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

## 7. 测试策略

### 7.1 单元测试覆盖

| 类 | 测试重点 |
|------|---------|
| FixtureStringsTest | 长度验证、前缀验证、格式验证（Email/UUID/手机号） |
| FixtureNumbersTest | 范围边界、金额精度、ID 正数 |
| FixtureDatesTest | 日期偏移量、与基准时间相对性 |
| FixtureSeedsTest | 同一种子下序列可重复、reset 清空种子 |
| FixtureBuilderTest | 正常构建、with 覆盖、无参构造检测、Record 拒绝、字段不存在、setter 失败、递归深度限制 |

### 7.2 集成测试场景

```java
// 嵌套对象深度限制测试
class Order { Long id; Customer customer; }
class Customer { Long id; String name; List<Order> orders; }

Order order = FixtureBuilder.of(Order.class).maxDepth(2).build();
assertThat(order.customer).isNotNull();       // 深度 2
assertThat(order.customer.orders).isNull();   // 超过深度 2
```

---

## 8. 参考资料

- Epic Backlog：`docs/specs/epic-01-core-and-test/00_epic_backlog.md` F01-10
- 前序 Feature：F01-09 MVC 测试断言辅助类（参考文档结构）
- JDK 文档：java.util.Random、java.lang.reflect、java.time
