# 自动软删除支持设计

**日期**: 2026-03-22
**模块**: cartisan-data-jpa
**状态**: 设计中

## 需求概述

让 `repository.delete(entity)` 和 `repository.deleteById(id)` 对软删除实体自动执行软删除操作，业务端无需手动设置 `deleted` 字段。

### 核心目标

1. **删除语义一致性**：调用 `delete()` 即表示删除，底层自动软删除
2. **Spring Data 风格**：业务端只定义接口，不写实现类
3. **零侵入**：现有业务代码无需修改

## 设计方案

### 架构

```
┌─────────────────────────────────────────────────────────┐
│              CartisanDataJpaAutoConfiguration           │
│  ┌───────────────────────────────────────────────────┐  │
│  │ JpaRepositoryFactoryEntryCustomizer               │  │
│  │   → 全局注册 BaseRepositoryImpl 作为基类           │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    BaseRepository                        │
│            (JpaRepository + JpaSpecificationExecutor)    │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  BaseRepositoryImpl                      │
│  ┌───────────────────────────────────────────────────┐  │
│  │ delete(T entity)                                  │  │
│  │   └─ instanceof SoftDeletable                     │  │
│  │     └─ markAsDeleted()                            │  │
│  │     └─ save() [复用事件发布逻辑]                   │  │
│  ├───────────────────────────────────────────────────┤  │
│  │ deleteById(ID id)                                 │  │
│  │   └─ findById(id).ifPresent(this::delete)         │  │
│  ├───────────────────────────────────────────────────┤  │
│  │ deleteAll(Iterable<T>)                            │  │
│  │ deleteAll()                                       │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `SoftDeletable.java` | 修改 | 添加 `markAsDeleted()` 领域方法 |
| `BaseRepositoryImpl.java` | 修改 | 重写 `delete()`, `deleteById()`, `deleteAll()` 等方法 |
| `CartisanDataJpaAutoConfiguration.java` | 修改 | 添加 `JpaRepositoryFactoryEntryCustomizer` Bean |
| `SoftDeletableIntegrationTest.java` | 修改 | 更新测试验证自动软删除行为 |

### 详细设计

#### 1. SoftDeletable 添加领域方法

```java
@MappedSuperclass
@SQLRestriction("deleted = false")
public abstract class SoftDeletable extends Auditable {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /**
     * 标记为已删除（领域方法）。
     *
     * <p>供 Repository.delete() 调用，业务端通常不需要直接调用。</p>
     */
    public void markAsDeleted() {
        this.deleted = true;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean getDeleted() {
        return deleted;
    }

    // 保留 protected setDeleted 供序列化框架使用
    protected void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
```

#### 2. BaseRepositoryImpl 重写删除方法

```java
public class BaseRepositoryImpl<T extends AggregateRoot<?>, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> {

    // ... 现有代码 ...

    @Override
    public void delete(T entity) {
        if (entity instanceof SoftDeletable softDeletable) {
            softDeletable.markAsDeleted();
            save(entity);  // 复用 save() 的事件发布逻辑
        } else {
            super.delete(entity);  // 非软删除实体，物理删除
        }
    }

    @Override
    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        List<T> softDeletable = new ArrayList<>();
        List<T> physicalDelete = new ArrayList<>();

        entities.forEach(e -> {
            if (e instanceof SoftDeletable) {
                softDeletable.add(e);
            } else {
                physicalDelete.add(e);
            }
        });

        softDeletable.forEach(e -> ((SoftDeletable) e).markAsDeleted());
        saveAll(softDeletable);
        super.deleteAll(physicalDelete);
    }

    @Override
    public void deleteAll() {
        if (isSoftDeletableEntityType()) {
            // 软删除实体：批量 UPDATE
            @Query("UPDATE {entity} e SET e.deleted = true")
            // 执行批量更新...
        } else {
            super.deleteAll();
        }
    }

    private boolean isSoftDeletableEntityType() {
        return SoftDeletable.class.isAssignableFrom(getDomainClass());
    }
}
```

#### 3. AutoConfiguration 全局注册

```java
@AutoConfiguration
@Import(JpaAuditingConfiguration.class)
public class CartisanDataJpaAutoConfiguration {

    // ... 现有 DomainEventPublisher 配置 ...

    /**
     * 全局配置 Repository 基类。
     *
     * <p>所有继承 BaseRepository 的接口自动使用 BaseRepositoryImpl，
     * 业务端无需手动指定 repositoryBaseClass。</p>
     */
    @Bean
    public JpaRepositoryFactoryEntryCustomizer repositoryFactoryEntryCustomizer() {
        return (JpaRepositoryFactoryBean<?, ?, ?> factoryBean) -> {
            factoryBean.setRepositoryBaseClass(BaseRepositoryImpl.class);
        };
    }
}
```

### 使用示例

**业务端代码（无需修改）**：

```java
public interface ProductRepository extends BaseRepository<Product, Long> {
    Optional<Product> findByName(String name);
}

// 使用
productRepository.delete(product);          // 自动软删除
productRepository.deleteById(productId);    // 自动软删除
productRepository.findAll();                // 自动过滤已删除
```

## 测试策略

### 单元测试

| 测试场景 | 预期行为 |
|----------|----------|
| `delete(entity)` 软删除实体 | `deleted=true`，查询时过滤 |
| `deleteById(id)` 软删除实体 | `deleted=true`，查询时过滤 |
| `delete(entity)` 非软删除实体 | 物理删除 |
| `deleteAll()` 混合实体 | 软删除实体 UPDATE，其他 DELETE |

### 集成测试

验证 `CartisanDataJpaAutoConfiguration` 自动配置生效：
1. 业务项目只定义接口，不指定 `repositoryBaseClass`
2. 确认 `delete()` 自动软删除

## 向后兼容性

- **不破坏现有代码**：未继承 `SoftDeletable` 的实体行为不变
- **测试需更新**：现有 `SoftDeletableIntegrationTest` 中手动 `setDeleted(true)` 的测试可保留，新增 `delete()` 测试

## 未解决问题

无
