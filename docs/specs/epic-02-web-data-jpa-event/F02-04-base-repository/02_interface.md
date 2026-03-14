# Feature: F02-04 BaseRepository — 接口契约

## 模块结构

```
cartisan-data-jpa/
├── build.gradle.kts
├── src/main/java/com/cartisan/data/jpa/
│   ├── repository/
│   │   ├── BaseRepository.java        # 核心接口
│   │   └── package-info.java          # 包说明
│   └── package-info.java              # 模块说明
```

## 领域接口描述（伪代码）

### BaseRepository 接口签名

```java
// 伪代码：接口定义
package com.cartisan.data.jpa.repository;

@NoRepositoryBean
interface BaseRepository<T extends AggregateRoot<?>, ID extends Serializable>
    extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>
```

### 泛型参数说明

| 参数 | 约束 | 说明 |
|------|------|------|
| `T` | `extends AggregateRoot<?>` | 聚合根类型，通配符表示不关心聚合根自身的泛型参数 |
| `ID` | `extends Serializable` | 聚合根标识符类型，与 Spring Data JPA 约定一致 |

### 继承的方法

通过继承 `JpaRepository<T, ID>` 获得：

| 方法 | 说明 |
|------|------|
| `save(T entity)` | 保存实体（新增或更新） |
| `findById(ID id)` | 根据 ID 查询 |
| `findAll()` | 查询所有 |
| `findAllById(Iterable<ID> ids)` | 根据 ID 列表查询 |
| `delete(T entity)` | 删除实体 |
| `deleteById(ID id)` | 根据 ID 删除 |
| `count()` | 统计记录数 |
| `existsById(ID id)` | 判断 ID 是否存在 |

通过继承 `JpaSpecificationExecutor<T>` 获得：

| 方法 | 说明 |
|------|------|
| `findOne(Specification<T> spec)` | 按条件查询单条 |
| `findAll(Specification<T> spec)` | 按条件查询列表 |
| `findAll(Specification<T> spec, Sort sort)` | 按条件查询并排序 |
| `findAll(Specification<T> spec, Pageable pageable)` | 按条件分页查询 |
| `count(Specification<T> spec)` | 按条件统计 |

## 核心流程

### 编译期约束流程

```
1. 业务代码定义 Repository
   └─> public interface OrderRepository extends BaseRepository<Order, Long> {}

2. 编译器检查 Order 是否实现 AggregateRoot<?>
   ├─> 是 → 编译通过
   └─> 否 → 编译错误

3. Spring Data 自动创建 OrderRepository 的代理实现
```

### 运行时流程

```
1. Spring 扫描到 BaseRepository 的子接口（如 OrderRepository）
2. @NoRepositoryBean 确保 BaseRepository 本身不被实例化
3. Spring Data 为子接口生成 JDK 动态代理
4. 代理方法调用委托给 JpaRepository / JpaSpecificationExecutor 的默认实现
```

## 依赖描述

### 外部依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| `cartisan-core` | project | AggregateRoot 标记接口 |
| `spring-boot-starter-data-jpa` | managed | JpaRepository / JpaSpecificationExecutor |
| `jakarta.persistence-api` | provided | JPA 注解 |

### 模块间依赖

```
cartisan-data-jpa
    │
    └──> cartisan-core (AggregateRoot)
```

## 使用示例

### 示例 1：定义聚合根 Repository

```java
// 聚合根
public class Order extends AbstractAggregateRoot<Order> {
    private final Long id;
    // ...
}

// Repository 接口（无需手写实现）
public interface OrderRepository extends BaseRepository<Order, Long> {
    // Spring Data 自动提供 CRUD，也可声明查询方法
    Optional<Order> findByCustomerName(String name);
}
```

### 示例 2：使用 Repository

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    // 构造函数注入...

    public void createOrder(Order order) {
        // 业务逻辑...
        order.registerEvent(new OrderCreatedEvent(order.getId()));
        orderRepository.save(order);  // F02-05 后 save 会自动发布事件
    }
}
```

## 错误场景

| 场景 | 编译期行为 |
|------|-----------|
| 非聚合根实体尝试继承 BaseRepository | 编译错误：类型参数不匹配 |
| ID 类型不实现 Serializable | 编译错误：类型参数不匹配 |

## 注解说明

| 注解 | 来源 | 作用 |
|------|------|------|
| `@NoRepositoryBean` | Spring Data | 防止为 BaseRepository 本身创建代理 Bean |
| `@Aggregate` | cartisan-core | 标注在聚合根类上（可选，用于 ArchUnit 验证） |
