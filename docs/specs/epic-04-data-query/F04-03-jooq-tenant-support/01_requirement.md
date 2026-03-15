# Feature: F04-03 JooqTenantSupport — 多租户查询支持

> **Epic:** Data-Query (Epic 4)
> **依赖:** F04-01（模块骨架 + PageQuery）✅ 已完成
> **复杂度:** S（Small，30-50 行）

---

## 背景

在使用 jOOQ 进行数据库查询时，多租户系统需要为每个查询添加租户过滤条件。jOOQ 原生没有提供租户上下文集成，开发者需要在每个查询中手动处理 `TenantContext.getCurrentTenantId()`，容易遗漏或出错。

本 Feature 提供一个工具方法 `JooqTenantSupport.eqTenantId(TableField<?, Long>)`，统一处理租户过滤逻辑：
- 有租户上下文时 → 自动添加 `tenant_id = ?` 条件
- 无租户上下文时 → 返回 `noCondition()`（不添加过滤）

---

## 目标

1. 提供 `JooqTenantSupport` 工具类，支持按表字段生成租户过滤条件
2. 集成 `cartisan-security` 的 `TenantContext`，获取当前租户 ID
3. 无租户上下文时优雅降级（不报错、不添加过滤）
4. 对 `cartisan-security` 的依赖为可选（`compileOnly`），由使用方决定是否引入

---

## 范围

### 包含（In Scope）

| 项 | 说明 |
|---|------|
| **JooqTenantSupport 工具类** | 位于 `com.cartisan.data.query.support` 包 |
| **eqTenantId 静态方法** | 接收 `TableField<?, Long>` 参数，返回 jOOQ `Condition` |
| **有租户时的行为** | 返回 `tenantIdField.eq(TenantContext.getCurrentTenantId())` |
| **无租户时的行为** | 返回 `DSL.noCondition()`（空条件，不影响 WHERE 子句） |
| **单元测试** | 覆盖有租户 / 无租户两个场景 |
| **Gradle 依赖配置** | 添加 `compileOnly(project(":cartisan-security"))` |

### 不包含（Out of Scope）

| 项 | 原因 |
|---|------|
| **neTenantId / inTenantIds 等方法** | 当前无明确需求，按 YAGNI 原则暂不实现 |
| **真实数据库查询验证** | 留给 F04-05 集成测试 |
| **自动拦截器 / AOP** | 显式调用更清晰，不引入隐藏行为 |

---

## 验收标准（Acceptance Criteria）

### AC1: 有租户上下文时返回等值条件

**Given:** 当前租户 ID 为 `123L`
**When:** 调用 `JooqTenantSupport.eqTenantId(USER.TENANT_ID)`
**Then:** 返回的 `Condition` 等价于 `USER.TENANT_ID.eq(123L)`

### AC2: 无租户上下文时返回空条件

**Given:** 当前无租户上下文（`TenantContext.getCurrentTenantId()` 返回 `null`）
**When:** 调用 `JooqTenantSupport.eqTenantId(USER.TENANT_ID)`
**Then:** 返回 `DSL.noCondition()`（不是 null）

### AC3: 依赖为可选（compileOnly）

**Given:** 业务项目未引入 `cartisan-security`
**When:** 只使用 `PageQuery`、`DSLContext`，不调用 `JooqTenantSupport`
**Then:** 项目正常编译和运行

**注意:** 若调用 `JooqTenantSupport.eqTenantId()` 但未引入 `cartisan-security`，运行时会报 `NoClassDefFoundError`，这是预期行为（文档中说明）。

### AC4: API 设计符合 jOOQ 惯用法

- 方法名为 `eqTenantId`，与 jOOQ 的 `eq`、`ne` 等命名一致
- 参数类型为 `TableField<?, Long>`，任意表的租户字段都可传入
- 返回类型为 `Condition`，可直接用于 `WHERE` 子句

---

## 约束

### 技术约束

| 约束 | 说明 |
|------|------|
| **JDK 版本** | Java 21 |
| **依赖范围** | `cartisan-security` 使用 `compileOnly` |
| **包结构** | 与 `page`、`config` 同级，新建 `support` 包 |

### API 设计约束

- **纯静态工具类**：`JooqTenantSupport` 不允许实例化（private 构造函数）
- **无状态**：方法不持有或修改任何实例变量
- **线程安全**：依赖 `TenantContext` 的线程安全实现（ScopedValue）

---

## 使用示例

```java
// 在 Repository 或 QueryService 中使用
import static com.cartisan.data.query.support.JooqTenantSupport.eqTenantId;
import static com.example.db.Tables.USER;

public List<UserRecord> findUsersByName(String name) {
    return dslContext.selectFrom(USER)
        .where(USER.NAME.like("%" + name + "%"))
        .and(eqTenantId(USER.TENANT_ID))  // 自动添加租户过滤
        .fetch();
}

// 无租户上下文时，eqTenantId 返回 noCondition()
// WHERE 子句变为：WHERE NAME LIKE '%xxx%'（无租户过滤）
```

---

## 后续扩展方向（非本 Feature 范围）

- `neTenantId(TableField<?, Long>)` — 排除指定租户
- `inTenantIds(TableField<?, Long>, Collection<Long>)` — 租户 IN 查询
- `tenantIdIsNull(TableField<?, Long>)` — 查询租户为空的记录
