# Feature: F02-07 TsidGenerator — 接口契约

> **版本**：v0.1 | **日期**：2026-03-14

## 接口定义

### 公共接口：TsidGenerator

**包路径**：`com.cartisan.data.jpa.id.TsidGenerator`

**职责**：生成时间排序的全局唯一 ID（TSID），支持从 ID 还原生成时间

#### 方法签名（伪代码）

```
public final class TsidGenerator {

    // ========== 工厂方法 ==========

    /**
     * 创建默认的 TsidGenerator 实例。
     *
     * <p>使用 ThreadLocalRandom 作为随机数源。</p>
     */
    public static TsidGenerator newInstance()

    /**
     * 创建指定随机数源的 TsidGenerator 实例（测试用）。
     *
     * @param random 随机数生成器（用于测试时固定 seed）
     */
    public static TsidGenerator withRandom(Random random)

    // ========== 核心方法 ==========

    /**
     * 生成一个新的 TSID。
     *
     * <p>TSID 格式：42 位时间戳 + 22 位随机数</p>
     *
     * @return 全局唯一、时间排序的正 long 值（不会为 null）
     */
    public long generate()

    /**
     * 从 TSID 提取生成时间。
     *
     * @param tsid 由本生成器产生的 ID
     * @return 生成该 ID 的大致时间（误差 < 1ms）
     * @throws IllegalArgumentException 如果 tsid 不是有效的 TSID
     */
    public Instant toInstant(long tsid)

    // ========== 常量（用于测试验证） ==========

    /** 时间戳位数 */
    public static final int TIMESTAMP_BITS = 42

    /** 随机数位数 */
    public static final int RANDOM_BITS = 22

    /** 最大随机数值 */
    public static final int MAX_RANDOM = 4194303

    /** 自定义 epoch */
    public static final Instant EPOCH = Instant.parse("2024-01-01T00:00:00Z")
}
```

---

## 核心流程（伪代码）

### generate() 流程

```
function generate() -> long:
    # 1. 获取当前毫秒时间戳（相对于 epoch）
    currentMillis = currentTimeMillis() - epochMillis

    # 2. 时间戳部分按 42 位掩码截断（避免溢出）
    timestampPart = currentMillis & 0x3FFFFFFFFFFL  # (1L << 42) - 1

    # 3. 生成随机数（22 位）
    random = nextInt(MAX_RANDOM + 1)  # [0, 4194303]

    # 4. 组合：时间戳左移 22 位 + 随机数
    tsid = (timestampPart << RANDOM_BITS) | random

    return tsid  # 正数，因为 timestampPart 最大值左移后仍不会超过 Long.MAX_VALUE
```

**单调性保证说明**：
- 跨毫秒：ID 严格递增（时间戳部分增加）
- 同一毫秒内：随机数可能使 ID 不严格递增（先大后小）
- v1 约定：只保证**时间戳部分不递减**，不保证同一毫秒内严格递增
- AC3 验证：连续生成的 ID，其时间戳部分应单调不递减（`extractTimestamp(id_n) >= extractTimestamp(id_{n-1})`）

**冲突概率**：使用 `ThreadLocalRandom.nextInt()`，同一毫秒内冲突概率为 1/4,194,304（约 0.000024%）

---

### toInstant(long) 流程

```
function toInstant(tsid) -> Instant:
    # 1. 提取时间戳部分（高 42 位）
    timestampMillis = tsid >>> RANDOM_BITS

    # 2. 加上 epoch
    return Instant.ofEpochMilli(epochMillis + timestampMillis)
```

---

## 数据结构

### TSID 位布局

```
Bit 63                                     Bit 0
|------------------------------------------|
|              42 bits Timestamp           |   22 bits Random   |
|  (milliseconds since 2024-01-01 UTC)    |  (0 ~ 4,194,303)   |
|------------------------------------------|

示例值（2024-03-14 10:30:00.123 UTC）：
  timestamp = 69 天 * 86400秒 * 1000毫秒 + ... ≈ 5,961,600,000
  random = 12345
  tsid = (5961600000 << 22) | 12345 ≈ 25,100,342,755,037,577

值域：
  - timestamp: 0 ~ 4,398,046,511,103（约 69 年）
  - random: 0 ~ 4,194,303
  - tsid: 0 ~ 9,223,372,036,854,775,807（Long.MAX_VALUE，保证正数）
```

---

## 错误处理

### toInstant() 参数校验

| 错误场景 | 抛出异常 | 错误码 |
|---------|---------|--------|
| tsid ≤ 0 | `IllegalArgumentException` | 无 |
| tsid 提取的时间戳 > 当前时间 + 1 天 | `IllegalArgumentException` | 无 |

**校验策略**：只校验"明显无效"的 tsid（≤0、或提取出的时间超出合理范围），避免对"历史很久以前"的合法 ID 误判。

**决策**：不引入 CodeMessage，参数校验直接抛出 `IllegalArgumentException`，消息包含无效值

---

## 常量定义表

| 常量名 | 值 | 说明 |
|--------|---|------|
| `TIMESTAMP_BITS` | 42 | 时间戳位数 |
| `RANDOM_BITS` | 22 | 随机数位数 |
| `MAX_RANDOM` | 4,194,303 | `(1 << 22) - 1` |
| `EPOCH` | `2024-01-01T00:00:00Z` | 自定义时间起点 |

---

## 实现约束

### 线程安全
- `generate()` 方法必须是线程安全的
- 使用 `ThreadLocalRandom` 作为随机数源（无锁、高性能）

### 性能要求
- 单线程生成速度 > 100万/秒
- 避免使用 `synchronized` 或 `lock`

### 时钟回拨处理
- 检测到时钟回拨（当前时间 < 上次记录时间）
- 策略：忽略，依赖随机数部分保证唯一性（极小概率冲突）
- **不在 v1 处理**：时钟回拨是运维问题，框架层不增加复杂度

---

## 与 Spring 集成（Phase 4 实现）

### AutoConfiguration 配置

```
@Configuration
@ConditionalOnMissingBean(TsidGenerator.class)
public class TsidGeneratorAutoConfiguration {

    @Bean
    public TsidGenerator tsidGenerator() {
        return TsidGenerator.newInstance();
    }
}
```

### JPA@Id 使用方式（业务项目）

```
@Entity
public class Order extends AbstractAggregateRoot {

    @Id
    private Long id;

    @Autowired
    private TsidGenerator tsidGenerator;

    @PrePersist
    void generateId() {
        if (id == null) {
            id = tsidGenerator.generate();
        }
    }
}
```

**注意**：应注入单例 TsidGenerator 实例，而非每次调用 `newInstance()`。
