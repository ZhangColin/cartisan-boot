# Feature: F02-05 Repository 保存时自动发布领域事件 — 接口契约

> **注意**：本文档使用伪代码和表格描述接口，Java 源代码在 Phase 4 生成。

---

## 1. 类/接口设计

### 1.1 类层次结构

```
                                    SimpleJpaRepository<T, ID>
                                                ▲
                                                │ 继承
                                                │
                        ┌───────────────────────┴───────────────────────┐
                        │           BaseRepositoryImpl                  │
                        │   (cartisan-data-jpa 模块)                    │
                        │                                               │
                        │   - DomainEventPublisher eventPublisher      │
                        │   + save(S entity): S                         │
                        │   - publishDomainEvents(T entity)            │
                        └───────────────────────────────────────────────┘

                                    JpaRepositoryFactory
                                                ▲
                                                │ 继承
                                                │
                        ┌───────────────────────┴───────────────────────┐
                        │     CartisanJpaRepositoryFactory              │
                        │   (cartisan-data-jpa 模块)                    │
                        │                                               │
                        │   - ApplicationContext applicationContext    │
                        │   + getTargetRepository(RepositoryInformation)│
                        └───────────────────────────────────────────────┘

                                 JpaRepositoryFactoryBean<T, S, ID>
                                                ▲
                                                │ 继承
                                                │
                        ┌───────────────────────┴───────────────────────┐
                        │  CartisanJpaRepositoryFactoryBean<T, S, ID>   │
                        │   (cartisan-data-jpa 模块)                    │
                        │                                               │
                        │   + createRepositoryFactory(EntityManager)    │
                        └───────────────────────────────────────────────┘
```

### 1.2 类职责

| 类 | 职责 |
|----|------|
| `BaseRepositoryImpl` | Repository 实现类，重写 save() 实现事件自动发布 |
| `CartesianJpaRepositoryFactory` | RepositoryFactory，负责创建 BaseRepositoryImpl 实例 |
| `CartesianJpaRepositoryFactoryBean` | FactoryBean，负责创建 Factory 并注入 ApplicationContext |

---

## 2. BaseRepositoryImpl 接口描述

### 2.1 类签名（伪代码）

```
类: BaseRepositoryImpl<T extends AggregateRoot<?>, ID extends Serializable>
继承: SimpleJpaRepository<T, ID>
可见性: public (包: com.cartisan.data.jpa.repository.impl)
```

### 2.2 字段

| 字段 | 类型 | 可见性 | 说明 |
|------|------|--------|------|
| `eventPublisher` | `DomainEventPublisher` | private final | 事件发布器，通过构造器注入 |

### 2.3 构造器

```
构造器: BaseRepositoryImpl(
    entityInformation: JpaEntityInformation<T, ?>
    entityManager: EntityManager
    eventPublisher: DomainEventPublisher
)
可见性: public
前置条件: 所有参数非 null
后置条件: 字段被初始化，父类构造器被调用
异常: NullPointerException 如果任何参数为 null
```

### 2.4 方法

#### save(S entity)

```
方法: <S extends T> save(S entity): S
可见性: public @Override
前置条件: entity 非 null
后置条件:
  1. 实体被持久化到数据库
  2. 如果 entity 是 AbstractAggregateRoot，事件被发布
  3. 如果 entity 是 AbstractAggregateRoot，事件列表被清空
异常: RuntimeException 如果持久化或事件发布失败
```

**核心流程（伪代码）：**
```
1. savedEntity = super.save(entity)           // JPA 持久化
2. publishDomainEvents(savedEntity)           // 发布事件
3. return savedEntity
```

#### publishDomainEvents(T entity)

```
方法: publishDomainEvents(entity: T): void
可见性: private
前置条件: entity 非 null
后置条件: 如果 entity 是 AbstractAggregateRoot，事件被发布并清空
```

**核心流程（伪代码）：**
```
1. if (entity instanceof AbstractAggregateRoot)
2.     events = entity.getDomainEvents()
3.     events.forEach(event -> eventPublisher.publish(event))
4.     entity.clearDomainEvents()
```

---

## 3. CartisanJpaRepositoryFactory 接口描述

### 3.1 类签名（伪代码）

```
类: CartisanJpaRepositoryFactory
继承: JpaRepositoryFactory
可见性: public (包: com.cartisan.data.jpa.repository.impl)
```

### 3.2 字段

| 字段 | 类型 | 可见性 | 说明 |
|------|------|--------|------|
| `applicationContext` | `ApplicationContext` | private final | Spring 上下文，用于获取 DomainEventPublisher |
| `entityManager` | `EntityManager` | private final | JPA 实体管理器 |

### 3.3 构造器

```
构造器: CartisanJpaRepositoryFactory(
    entityManager: EntityManager
    applicationContext: ApplicationContext
)
可见性: public
前置条件: 所有参数非 null
```

### 3.4 方法

#### getTargetRepository(RepositoryInformation)

```
方法: getTargetRepository(information: RepositoryInformation): Object
可见性: protected @Override
返回: BaseRepositoryImpl 实例
```

**核心流程（伪代码）：**
```
1. entityInformation = getEntityInformation(information.getDomainType())
2. eventPublisher = applicationContext.getBean(DomainEventPublisher.class)
3. return new BaseRepositoryImpl(entityInformation, entityManager, eventPublisher)
```

---

## 4. CartisanJpaRepositoryFactoryBean 接口描述

### 4.1 类签名（伪代码）

```
类: CartisanJpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
继承: JpaRepositoryFactoryBean<T, S, ID>
可见性: public (包: com.cartisan.data.jpa.repository.impl)
```

### 4.2 泛型参数

| 参数 | 约束 |
|------|------|
| T | 必须继承 `Repository<S, ID>` |
| S | 实体类型 |
| ID | ID 类型 |

### 4.3 构造器

```
构造器: CartisanJpaRepositoryFactoryBean(repositoryInterface: Class<? extends T>)
可见性: public
```

### 4.4 方法

#### createRepositoryFactory(EntityManager)

```
方法: createRepositoryFactory(entityManager: EntityManager): RepositoryFactorySupport
可见性: protected @Override
返回: new CartisanJpaRepositoryFactory(entityManager, getApplicationContext())
```

---

## 5. 自动配置

### 5.1 配置类（伪代码）

```
@Configuration
@EnableJpaRepositories(
    repositoryBaseClass = BaseRepositoryImpl.class,
    repositoryFactoryBeanClass = CartisanJpaRepositoryFactoryBean.class
)
类: CartisanDataJpaAutoConfiguration
```

### 5.2 Bean 依赖

```
CartisanJpaRepositoryFactoryBean 需要:
  └─ ApplicationContext
      └─ DomainEventPublisher (Bean)
```

---

## 6. 异常处理

| 场景 | 异常类型 | HTTP 状态 | 处理方式 |
|------|---------|----------|---------|
| eventPublisher.publish() 抛异常 | 传播原始异常 | 500 | 全局异常处理器捕获，事务回滚 |
| ApplicationContext 中无 DomainEventPublisher Bean | NoSuchBeanDefinitionException | 500 | 启动时失败，提示缺少 cartisan-event 依赖 |

---

## 7. 事务语义

| 操作 | 事务行为 |
|------|---------|
| super.save(entity) | 在当前事务内执行 |
| eventPublisher.publish(event) | 在当前事务内同步执行 |
| 监听器抛异常 | 整个事务回滚，entity 不被持久化 |

**时序图：**
```
@Transactional
  ├─ baseRepository.save(order)
  │   ├─ super.save(order) ──────> DB (INSERT/UPDATE)
  │   └─ publishDomainEvents(order)
  │       └─ eventPublisher.publish(event)
  │           └─ @EventListener handler
  │               ├─ 成功 → 事务提交
  │               └─ 抛异常 → 事务回滚
```

---

## 8. 与现有组件的集成

### 8.1 依赖的组件

| 组件 | 来源 | 用途 |
|------|------|------|
| `BaseRepository<T, ID>` | cartisan-data-jpa (F02-04) | 基础 Repository 接口 |
| `DomainEventPublisher` | cartisan-event (F02-08) | 事件发布器 |
| `AbstractAggregateRoot<T>` | cartisan-core | 事件管理基类 |
| `DomainEvent` | cartisan-core | 领域事件基类 |

### 8.2 包结构

```
com.cartisan.data.jpa.repository.impl
  ├── BaseRepositoryImpl.java           (实现类)
  ├── CartisanJpaRepositoryFactory.java (工厂)
  └── CartisanJpaRepositoryFactoryBean.java (工厂Bean)
```

---

## 9. 测试接口描述

### 9.1 集成测试验证点

| 测试场景 | 验证方式 |
|---------|---------|
| AC1: 事件被发布 | 使用 `@EventListener` 捕获事件，验证非空 |
| AC2: 事件被清空 | save 后调用 `entity.getDomainEvents()` 验证为空 |
| AC3: 非 AggregateRoot 不发布 | 普通实体 save 后无事件 |
| AC4: saveAll 发布多个事件 | saveAll 多个聚合根，验证事件数量匹配 |
| AC5: 异常回滚 | 监听器抛异常，验证 DB 中无记录 |

---

## 10. 数据库变更

**本 Feature 不涉及数据库变更。**

---

## 11. 配置属性

**本 Feature 不需要新增配置属性。**

依赖自动配置：
- `DomainEventPublisher` Bean 由 `cartisan-event` 自动配置提供
- `@EnableJpaRepositories` 在 `CartisanDataJpaAutoConfiguration` 中配置
