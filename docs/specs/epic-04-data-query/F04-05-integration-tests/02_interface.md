# Feature: jOOQ 集成测试 — 接口契约

## 一、接口定义

### 1.1 cartisan-security 新增接口

#### TenantTestSupport.runWithTenant()

**完整签名：**
```java
public static void runWithTenant(Long tenantId, Runnable runnable)
```

**前置条件：**
- `runnable` 非空

**后置条件：**
- 执行 `runnable.run()` 时，`TenantContext.getCurrentTenantId()` 返回指定的 `tenantId`
- 执行结束后，租户上下文被清除（不影响后续测试）

**异常：**
- 若 `runnable` 为 null，抛出 `NullPointerException`

**行为说明：**
- 若 `tenantId` 为 null，执行时租户上下文为空
- 使用 `ScopedValue.where()` 实现租户上下文绑定

---

### 1.2 cartisan-data-query 集成测试

#### JooqIntegrationTest

**基类：**
```java
IntegrationTestBase
```

**依赖：**
```java
@Autowired DSLContext dslContext
```

**测试方法签名（伪代码）：**

| 方法 | 描述 |
|------|------|
| `given_dslContextAutoConfigured_whenExecuteSimpleQuery_thenReturnsResult()` | 验证 DSLContext 能执行简单查询 |
| `given_tenantContextAndTableWithTenantId_whenQueryWithEqTenantId_thenFiltersByTenant()` | 验证租户过滤功能 |
| `given_pageQuery_whenQueryWithLimitOffset_thenExecutesSuccessfully()` | 验证分页参数传递（可选） |

---

## 二、核心流程（伪代码）

### 2.1 DSLContext 简单查询

```java
@Test
@DisplayName("given_dslContextAutoConfigured_whenExecuteSimpleQuery_thenReturnsResult")
void given_dslContextAutoConfigured_whenExecuteSimpleQuery_thenReturnsResult() {
    // Given: DSLContext 由自动配置创建

    // When: 执行简单查询
    Result<Record1<Integer>> result = dslContext
        .select(DSL.one())
        .fetch();

    // Then: 返回结果
    assertThat(result).hasSize(1);
    assertThat(result.get(0).value1()).isEqualTo(1);
}
```

### 2.2 租户过滤测试

```java
@Test
@DisplayName("given_tenantContextAndTableWithTenantId_whenQueryWithEqTenantId_thenFiltersByTenant")
void given_tenantContextAndTableWithTenantId_whenQueryWithEqTenantId_thenFiltersByTenant() {
    // Given: 创建临时表并插入多租户数据
    dslContext.execute("""
        CREATE TEMP TABLE test_user (
            id BIGINT,
            tenant_id BIGINT,
            name VARCHAR
        )
        """);
    dslContext.execute("INSERT INTO test_user VALUES (1, 100, 'Alice')");
    dslContext.execute("INSERT INTO test_user VALUES (2, 200, 'Bob')");

    // 构造 tenant_id 字段引用
    TableField<Record, Long> tenantIdField = DSL.tableField(
        DSL.name("test_user", "tenant_id"),
        SQLDataType.BIGINT
    );

    // When: 在租户 100 上下文中执行带租户过滤的查询
    AtomicReference<List<Record>> results = new AtomicReference<>();
    TenantTestSupport.runWithTenant(100L, () -> {
        results.set(dslContext
            .selectFrom(DSL.table("test_user"))
            .where(JooqTenantSupport.eqTenantId(tenantIdField))
            .fetch());
    });

    // Then: 只返回租户 100 的数据
    assertThat(results.get()).hasSize(1);
    assertThat(results.get().get(0).get("name", String.class)).isEqualTo("Alice");
}
```

### 2.3 分页参数测试（可选）

```java
@Test
@DisplayName("given_pageQuery_whenQueryWithLimitOffset_thenExecutesSuccessfully")
void given_pageQuery_whenQueryWithLimitOffset_thenExecutesSuccessfully() {
    // Given: PageQuery
    PageQuery pageQuery = PageQuery.of(2, 10);  // offset = 10

    // When: 执行带 LIMIT/OFFSET 的查询
    Result<Record1<Integer>> result = dslContext
        .resultQuery("SELECT 1 LIMIT ? OFFSET ?", pageQuery.size(), pageQuery.offset())
        .fetch();

    // Then: 查询成功执行（无断言，只验证不报错）
    assertThat(result).isNotNull();
}
```

---

## 三、数据库变更

### 3.1 临时表结构

**表名：** `test_user`（临时表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户 ID |
| name | VARCHAR | 名称 |

**生命周期：**
- 每个测试方法内 CREATE
- 测试结束后 DROP（通过 `@AfterEach` 或 `try-finally`）

---

## 四、设计决策

### 4.1 为何使用临时表而非生成代码

| 方案 | 优点 | 缺点 |
|------|------|------|
| 临时表 + DSL.tableField | 无需代码生成，框架独立 | 不能测试类型安全 DSL |
| 测试内代码生成 | 完整测试 | 构建复杂，增加维护成本 |

**决策：** 使用临时表，类型安全 DSL 留给业务项目验证。

### 4.2 为何需要 TenantTestSupport

**问题：** `TenantContext.runWithTenant()` 是 package-private，测试无法直接访问。

**方案对比：**

| 方案 | 优点 | 缺点 |
|------|------|------|
| 新增 TenantTestSupport | 不破坏封装，可复用 | 需要修改 cartisan-security |
| 反射访问 | 无需修改代码 | 破坏封装，维护性差 |
| 同包测试类 | 无需修改 security | 归属不清，易失效 |

**决策：** 在 cartisan-security 添加 `TenantTestSupport` 测试工具类。

---

## 五、Spring 配置

### 5.1 测试配置类

```java
@SpringBootTest(classes = JooqIntegrationTest.TestApp.class)
@ImportAutoConfiguration({
    PostgresTestContainer.class,
    DataSourceAutoConfiguration.class,
    JooqAutoConfiguration.class
})
class JooqIntegrationTest extends IntegrationTestBase {
    // ...
}
```

### 5.2 依赖关系

```
JooqIntegrationTest
    ├── IntegrationTestBase（自动数据清理）
    ├── PostgresTestContainer（PostgreSQL 容器）
    ├── DataSourceAutoConfiguration（DataSource）
    └── JooqAutoConfiguration（DSLContext）
```
