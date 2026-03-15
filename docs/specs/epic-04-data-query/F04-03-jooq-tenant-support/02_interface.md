# Feature: F04-03 JooqTenantSupport — 接口契约

> **版本:** v1.0
> **日期:** 2026-03-15

---

## 接口定义

### 领域接口描述（伪代码）

**工具类：** `JooqTenantSupport`

**位置：** `com.cartisan.data.query.support`

**职责：** 提供 jOOQ 查询的多租户条件生成工具方法

---

## 公共 API

### 方法：eqTenantId

**签名（伪代码）：**
```java
public final class JooqTenantSupport {
    /**
     * 为指定表的租户字段生成等值过滤条件。
     *
     * @param tenantIdField 表的租户 ID 字段（如 USER.TENANT_ID）
     * @return jOOQ Condition 对象
     *         - 有租户上下文时：返回 tenantIdField.eq(tenantId)
     *         - 无租户上下文时：返回 DSL.noCondition()
     */
    public static Condition eqTenantId(TableField<?, Long> tenantIdField) {
        // 实现逻辑见下方"核心流程"
    }

    // 私有构造函数，防止实例化
    private JooqTenantSupport() {
        throw new UnsupportedOperationException("Utility class");
    }
}
```

**前置条件：**
- `tenantIdField` 参数非 null（否则抛 `NullPointerException`）

**后置条件：**
- 返回值非 null（永远返回有效 Condition 对象）

**异常：**
| 异常 | 触发条件 |
|------|---------|
| `NullPointerException` | `tenantIdField` 为 null |
| `NoClassDefFoundError` | 运行时缺少 `cartisan-security` 依赖（仅在调用此方法时） |

---

## 依赖类型

### 外部依赖

| 类型 | 来源 | 用途 |
|------|------|------|
| `TableField<?, Long>` | `org.jooq` | jOOQ 表字段类型，租户 ID 字段 |
| `Condition` | `org.jooq` | jOOQ 条件对象，用于 WHERE 子句 |
| `DSL` | `org.jooq` | jOOQ 工具类，提供 `noCondition()` |

### 内部依赖

| 类型 | 来源 | 依赖范围 | 用途 |
|------|------|---------|------|
| `TenantContext` | `cartisan-security` | `compileOnly` | 获取当前租户 ID |

---

## 核心流程（伪代码）

```pseudocode
FUNCTION eqTenantId(tenantIdField: TableField<?, Long>) -> Condition
    // 前置检查
    IF tenantIdField IS NULL THEN
        THROW NullPointerException

    // 获取当前租户 ID
    tenantId = TenantContext.getCurrentTenantId()
    // 注意：若未引入 cartisan-security，此处编译不通过
    //       运行时若缺少 TenantContext 类，抛 NoClassDefFoundError

    // 根据租户上下文返回条件
    IF tenantId IS NOT NULL THEN
        RETURN tenantIdField.eq(tenantId)
    ELSE
        RETURN DSL.noCondition()
    END IF
END FUNCTION
```

---

## 数据库变更

**本 Feature 无数据库变更。**

---

## Gradle 依赖配置

```kotlin
// cartisan-data-query/build.gradle.kts

dependencies {
    // 现有依赖...

    // cartisan-security - 可选依赖（编译期需要，运行时由使用方提供）
    // 用于 JooqTenantSupport 访问 TenantContext
    compileOnly(project(":cartisan-security"))
}
```

**配置说明：**
- `compileOnly`：编译期需要 `TenantContext` 类型解析
- 运行时：若业务项目使用 `JooqTenantSupport`，需自行引入 `cartisan-security`
- 未使用 `JooqTenantSupport` 的项目，可不引入 `cartisan-security`

---

## 包结构

```
com.cartisan.data.query
├── page/                      # F04-01：分页支持
│   └── PageQuery.java
├── config/                    # F04-02：自动配置
│   ├── JooqAutoConfiguration.java
│   └── JooqProperties.java
└── support/                   # F04-03：查询支持工具（新增）
    ├── package-info.java
    └── JooqTenantSupport.java
```

---

## 备选方案与取舍

### 方案 A：静态工具类（采用）

**描述：** 纯静态方法，无状态，直接调用。

**优点：**
- 简单直接，符合 jOOQ 使用习惯（DSL 多为静态导入）
- 无需依赖注入，测试方便
- API 清晰，意图明确

**缺点：**
- 无法 Mock（但本类逻辑简单，无需 Mock）

---

### 方案 B：Spring Bean 服务类

**描述：** `@Component` + `@RequiredArgsConstructor`，注入 `TenantContext`。

**优点：**
- 可 Mock，便于单元测试

**缺点：**
- 需要依赖注入，增加复杂度
- 与 jOOQ DSL 的函数式风格不一致
- `TenantContext` 本身就是静态工具类，包装无意义

**结论：** 不采用，方案 A 更符合工具类定位。

---

### 方案 C：反射检测 + 降级

**描述：** 运行时检测 `TenantContext` 类是否存在，不存在则返回 `noCondition()`。

**优点：**
- 未引入 `cartisan-security` 时调用不报错

**缺点：**
- 反射代码复杂，维护成本高
- 掩盖依赖问题，容易漏引入
- 性能略有损耗

**结论：** 不采用。若业务方使用租户功能，应显式引入依赖，编译期检查更安全。

---

## 单元测试策略

### 测试范围

| 测试场景 | 说明 |
|---------|------|
| **有租户上下文** | 验证返回 `tenantIdField.eq(123L)` |
| **无租户上下文** | 验证返回 `DSL.noCondition()` |
| **null 参数** | 验证抛 `NullPointerException` |

### 测试实现方式

由于 `TenantContext` 使用 `ScopedValue`，测试需要在其作用域内执行：

```java
// 使用 TenantContext.runWithTenant() 模拟租户上下文
TenantContext.runWithTenant(123L, () -> {
    Condition result = JooqTenantSupport.eqTenantId(USER.TENANT_ID);
    // 断言 result 是等值条件
});
```

**注意：** F04-03 单元测试不依赖 Spring、不连接数据库，仅验证条件生成逻辑。真实查询验证留给 F04-05 集成测试。
