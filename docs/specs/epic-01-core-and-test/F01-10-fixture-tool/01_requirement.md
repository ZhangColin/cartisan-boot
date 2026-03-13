# Feature: F01-10 — Fixture 工具

> Epic: 01-core-and-test
> 版本：v0.1 | 日期：2026-03-14
> 状态：Phase 1 需求澄清

---

## 背景

F01-07 至 F01-09 已完成 ArchUnit 规则集、Testcontainers 基类、MVC 测试断言辅助类。但在编写单元测试和集成测试时，构建测试数据（POJO、JPA 实体）仍需大量样板代码。

每次测试都需要手动构造对象、设置字段值，代码重复且容易遗漏。对于复杂嵌套对象，手动构建尤其繁琐。

**目标：** 提供随机数据生成器和泛型 FixtureBuilder，消除测试数据构建的样板代码。

---

## 目标

1. 新增随机数据生成器类：FixtureStrings、FixtureNumbers、FixtureDates
2. 新增 FixtureBuilder 泛型构建器，自动填充 POJO/JPA 实体字段
3. 支持全局种子设置，保证测试可重复性
4. 按职责拆分 Fixture 类，保持 API 清晰易用

---

## 范围

### 包含（In Scope）

| 类别 | 内容 |
|------|------|
| **FixtureStrings** | randomString()（变体：长度/前缀）、randomEmail()、randomUuid()、randomPhoneNumber() |
| **FixtureNumbers** | randomInt()（范围）、randomLong()（范围）、randomId()、randomAmount()、randomDecimal() |
| **FixtureDates** | now()、today()、pastDays()、futureDays()（含基准时间变体） |
| **FixtureSeeds** | setGlobalSeed()、resetSeed()、currentRandom()（包可见） |
| **FixtureBuilder<T>** | of()、with()、maxDepth()、build()，支持 POJO/JPA 实体（无参构造 + setter） |
| **FixtureBuildException** | 继承 RuntimeException，两个构造器 |
| **单元测试** | 覆盖所有公开方法 |

### 不包含（Out of Scope）

| 内容 | 原因 |
|------|------|
| Record 类型支持 | Record 需要特殊处理（canonical 构造函数），留待后续版本 |
| withAll(Map) 批量覆盖 | 可选增强，非必需 |
| Instant 相关方法 | 可选增强，后续补充 |
| JPA @Entity 注解感知 | 不依赖 JPA，仅通过反射处理普通 POJO |

---

## 验收标准（Acceptance Criteria）

### AC1: FixtureStrings 随机字符串生成
- [ ] `randomString()` 返回 12 位字母数字
- [ ] `randomString(int length)` 返回指定位数字符串
- [ ] `randomString(String prefix)` 返回 prefix + 12 位随机字符
- [ ] `randomString(String prefix, int length)` 返回 prefix + length 位随机字符
- [ ] `randomEmail()` 返回格式正确的 Email（xxx@test.local）
- [ ] `randomUuid()` 返回 32 位无连字符 UUID 字符串
- [ ] `randomPhoneNumber()` 返回 1 开头的 12 位数字字符串

### AC2: FixtureNumbers 随机数字生成
- [ ] `randomInt()` 返回正整数
- [ ] `randomInt(min, max)` 返回 [min, max] 范围内的整数
- [ ] `randomLong()` 返回正长整数
- [ ] `randomLong(min, max)` 返回 [min, max] 范围内的长整数
- [ ] `randomId()` 返回从 1 开始的正数 ID
- [ ] `randomAmount()` 返回 2 位小数的 BigDecimal
- [ ] `randomDecimal(min, max, scale)` 返回指定范围和精度的 BigDecimal

### AC3: FixtureDates 日期生成
- [ ] `now()` 返回当前 LocalDateTime
- [ ] `today()` 返回当前 LocalDate
- [ ] `pastDays(n)` 返回约 n 天前的 LocalDateTime
- [ ] `futureDays(n)` 返回约 n 天后的 LocalDateTime
- [ ] `pastDays(base, n)` 返回 base 时间减去 n 天
- [ ] `futureDays(base, n)` 返回 base 时间加上 n 天

### AC4: FixtureSeeds 种子管理
- [ ] `setGlobalSeed(seed)` 后，所有新创建的 Random 使用该种子
- [ ] 同一种子下，重复调用序列结果一致（reset 后再次 set 相同种子验证）
- [ ] `resetSeed()` 清空种子，后续调用恢复随机
- [ ] ThreadLocal 实现，多线程测试互不影响

### AC5: FixtureBuilder 基本构建
- [ ] `of(Class).build()` 能创建无参构造的实例
- [ ] 自动填充基本类型字段（int=0, boolean=false 等）
- [ ] 自动填充 String 字段为随机字符串
- [ ] 自动填充 BigDecimal 字段为随机金额
- [ ] 自动填充 LocalDateTime 字段为当前时间
- [ ] 无无参构造时抛出 FixtureBuildException

### AC6: FixtureBuilder 字段覆盖
- [ ] `with(fieldName, value)` 能覆盖指定字段值
- [ ] 支持链式调用，多次 with 都生效
- [ ] 字段不存在时抛出 FixtureBuildException
- [ ] setter 调用失败时抛出 FixtureBuildException（含 cause）

### AC7: FixtureBuilder 递归深度限制
- [ ] 默认最大深度为 3
- [ ] `maxDepth(n)` 能自定义最大深度
- [ ] 超过深度时，对象字段为 null，基本类型字段为默认值
- [ ] Record 类型被检测并拒绝，抛出 FixtureBuildException

### AC8: FixtureBuildException
- [ ] 继承 RuntimeException
- [ ] 提供 `FixtureBuildException(String message)` 构造器
- [ ] 提供 `FixtureBuildException(String message, Throwable cause)` 构造器

### AC9: 单元测试覆盖
- [ ] 每个 Fixture 类的公开方法都有对应测试
- [ ] 种子可重复性测试（set → 调用序列 → reset → set → 相同序列 → 相同结果）
- [ ] FixtureBuilder 各种场景测试（正常/覆盖/深度/异常）
- [ ] 测试覆盖正常场景和边界场景

---

## 约束

### 技术约束
- 使用 JDK 标准库 `java.util.Random`
- 反射使用 `clazz.getDeclaredConstructor().newInstance()`（非已废弃的 `newInstance()`）
- ThreadLocal 保证线程安全

### 架构约束
- 包路径：`com.cartisan.test.fixture`
- 依赖：仅依赖 `cartisan-core` 和 JDK 标准库
- 零依赖 Spring / JPA / 其他第三方库

### 设计原则
- 按职责拆分：FixtureStrings/Numbers/Dates 各司其职
- 方法命名清晰：`randomString()` 而非 `string()`，避免与其他工具类冲突
- 非受检异常：FixtureBuildException 继承 RuntimeException
- 种子保留策略：种子值保留到 resetSeed()，非一次性清空

---

## 使用示例

### 随机数据生成

```java
// 字符串
String name = FixtureStrings.randomString("USER_", 8); // USER_a1b2c3d4
String email = FixtureStrings.randomEmail(); // x7k9z3m2@test.local

// 数字
long id = FixtureNumbers.randomId(); // 42
BigDecimal amount = FixtureNumbers.randomAmount(); // 123.45

// 日期
LocalDateTime yesterday = FixtureDates.pastDays(1);
LocalDateTime nextWeek = FixtureDates.futureDays(7);
```

### FixtureBuilder

```java
// 自动填充
Order order = FixtureBuilder.of(Order.class).build();
// order.id != null, order.orderNo != null, order.amount != null

// 覆盖字段
Order order = FixtureBuilder.of(Order.class)
    .with("status", OrderStatus.CANCELLED)
    .with("amount", new BigDecimal("99.99"))
    .build();

// 控制递归深度
Order order = FixtureBuilder.of(Order.class)
    .maxDepth(1)
    .build();
// order.customer == null（深度 1 仅填充 Order 一层）
```

### 种子可重复性

```java
FixtureSeeds.setGlobalSeed(42);
String a1 = FixtureStrings.randomString();
String a2 = FixtureStrings.randomString();

FixtureSeeds.resetSeed();
FixtureSeeds.setGlobalSeed(42);
String b1 = FixtureStrings.randomString();
String b2 = FixtureStrings.randomString();

assertThat(b1).isEqualTo(a1);
assertThat(b2).isEqualTo(a2);
```

---

## 参考资料

- Epic Backlog：`docs/specs/epic-01-core-and-test/00_epic_backlog.md` F01-10
- 前序 Feature：F01-09 MVC 测试断言辅助类（参考其文档结构）
