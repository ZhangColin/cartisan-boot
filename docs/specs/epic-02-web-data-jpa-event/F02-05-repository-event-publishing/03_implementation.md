# Feature: F02-05 Repository 保存时自动发布领域事件 — 实施计划

> **依赖**: 01_requirement.md + 02_interface.md
> **实施方式**: TDD（先测试红灯，后实现绿灯）

---

## 目标复述

实现 `BaseRepositoryImpl` 继承 `SimpleJpaRepository`，重写 `save()` 方法，在 JPA 持久化后自动发布聚合根的领域事件并通过自定义 `JpaRepositoryFactoryBean` + `JpaRepositoryFactory` 注入 `DomainEventPublisher`。

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新建 | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java` | Repository 实现类 |
| 新建 | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/CartisanJpaRepositoryFactory.java` | 自定义 RepositoryFactory |
| 新建 | `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/CartisanJpaRepositoryFactoryBean.java` | 自定义 FactoryBean |
| 修改 | `cartisan-data-jpa/build.gradle.kts` | 添加 cartisan-event 依赖 |
| 新建 | `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImplTest.java` | 单元测试 |
| 新建 | `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/RepositoryEventPublishingIntegrationTest.java` | 集成测试 |
| 新建 | `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/TestAggregateRoot.java` | 测试用聚合根 |
| 新建 | `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/TestDomainEvent.java` | 测试用领域事件 |

---

## 核心流程（伪代码）

```
save(entity):
    1. savedEntity = super.save(entity)           // JPA 持久化
    2. publishDomainEvents(savedEntity)           // 发布事件
    3. return savedEntity

publishDomainEvents(entity):
    1. if (entity instanceof AbstractAggregateRoot)
    2.     events = entity.getDomainEvents()
    3.     events.forEach(eventPublisher::publish)
    4.     entity.clearDomainEvents()
```

---

## 原子任务清单

### Step 1: 添加依赖

- **文件**: `cartisan-data-jpa/build.gradle.kts`
- **内容**: 添加 `api(project(":cartisan-event"))`
- **验证**: `./gradlew :cartisan-data-jpa:compileJava` 通过

### Step 2: 创建测试实体和事件

- **文件**:
  - `TestAggregateRoot.java` - 继承 `AbstractAggregateRoot<TestAggregateRoot>`
  - `TestDomainEvent.java` - 继承 `DomainEvent`
- **内容**: 测试用聚合根，提供 `registerTestEvent()` 方法
- **验证**: 编译通过

### Step 3: 编写 BaseRepositoryImpl 单元测试（红灯）

- **文件**: `BaseRepositoryImplTest.java`
- **内容**:
  - `given_aggregateRootWithEvent_when_save_then_eventPublished()` - AC1
  - `given_aggregateRootWithEvent_when_save_then_eventsCleared()` - AC2
  - `given_nonAggregateRoot_when_save_then_noEventPublished()` - AC3
- **验证**: 编译通过 + 测试全红（类不存在）

### Step 4: 实现 BaseRepositoryImpl（绿灯）

- **文件**: `BaseRepositoryImpl.java`
- **包**: `com.cartisan.data.jpa.repository.impl`
- **内容**:
  ```java
  public class BaseRepositoryImpl<T extends AggregateRoot<?>, ID extends Serializable>
          extends SimpleJpaRepository<T, ID> {
      private final DomainEventPublisher eventPublisher;

      public BaseRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
                                 EntityManager entityManager,
                                 DomainEventPublisher eventPublisher) {
          super(entityInformation, entityManager);
          this.eventPublisher = Objects.requireNonNull(eventPublisher);
      }

      @Override
      public <S extends T> S save(S entity) {
          S savedEntity = super.save(entity);
          publishDomainEvents(savedEntity);
          return savedEntity;
      }

      private void publishDomainEvents(T entity) {
          if (entity instanceof AbstractAggregateRoot aggregateRoot) {
              List<DomainEvent> events = aggregateRoot.getDomainEvents();
              events.forEach(eventPublisher::publish);
              aggregateRoot.clearDomainEvents();
          }
      }
  }
  ```
- **验证**: 测试全绿 + ArchUnit 通过

### Step 5: 编写 CartisanJpaRepositoryFactory 单元测试（红灯）

- **文件**: `CartisanJpaRepositoryFactoryTest.java`
- **内容**: 验证 `getTargetRepository()` 返回使用三参构造的 `BaseRepositoryImpl`
- **验证**: 编译通过 + 测试全红

### Step 6: 实现 CartisanJpaRepositoryFactory（绿灯）

- **文件**: `CartisanJpaRepositoryFactory.java`
- **内容**:
  ```java
  public class CartisanJpaRepositoryFactory extends JpaRepositoryFactory {
      private final EntityManager entityManager;
      private final ApplicationContext applicationContext;

      public CartisanJpaRepositoryFactory(EntityManager entityManager,
                                           ApplicationContext applicationContext) {
          super(entityManager);
          this.entityManager = entityManager;
          this.applicationContext = applicationContext;
      }

      @Override
      protected Object getTargetRepository(RepositoryInformation information) {
          JpaEntityInformation<?, ?> entityInformation =
              getEntityInformation(information.getDomainType());
          DomainEventPublisher eventPublisher =
              applicationContext.getBean(DomainEventPublisher.class);
          return new BaseRepositoryImpl<>(entityInformation, entityManager, eventPublisher);
      }
  }
  ```
- **验证**: 测试全绿

### Step 7: 编写 CartisanJpaRepositoryFactoryBean 单元测试（红灯）

- **文件**: `CartisanJpaRepositoryFactoryBeanTest.java`
- **内容**: 验证 `createRepositoryFactory()` 返回 `CartisanJpaRepositoryFactory`
- **验证**: 编译通过 + 测试全红

### Step 8: 实现 CartisanJpaRepositoryFactoryBean（绿灯）

- **文件**: `CartisanJpaRepositoryFactoryBean.java`
- **内容**:
  ```java
  public class CartisanJpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
          extends JpaRepositoryFactoryBean<T, S, ID> {

      public CartisanJpaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
          super(repositoryInterface);
      }

      @Override
      protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
          return new CartisanJpaRepositoryFactory(entityManager, getApplicationContext());
      }
  }
  ```
- **验证**: 测试全绿

### Step 9: 编写集成测试（红灯）

- **文件**: `RepositoryEventPublishingIntegrationTest.java`
- **内容**:
  - `given_aggregateRootWithEvent_when_save_then_eventReceivedByListener()` - AC1
  - `given_multipleAggregates_when_saveAll_then_allEventsPublished()` - AC4
  - `given_listenerThrowsException_when_save_then_transactionRolledBack()` - AC5
- **验证**: 编译通过 + 测试全红（需要配置 `@EnableJpaRepositories`）

### Step 10: 配置自动配置（绿灯）

- **文件**: `CartisanDataJpaAutoConfiguration.java`（新建或更新）
- **内容**:
  ```java
  @Configuration
  @EnableJpaRepositories(
      repositoryBaseClass = BaseRepositoryImpl.class,
      repositoryFactoryBeanClass = CartisanJpaRepositoryFactoryBean.class
  )
  public class CartisanDataJpaAutoConfiguration {
  }
  ```
- **验证**: 集成测试全绿

### Step 11: 全量验证

- **命令**:
  ```bash
  ./gradlew :cartisan-data-jpa:check
  ```
- **验收**: 所有测试绿灯 + ArchUnit 通过

---

## 依赖模块状态

| 模块 | 组件 | 状态 |
|------|------|------|
| cartisan-core | `AbstractAggregateRoot<T>` | ✅ 已完成 |
| cartisan-core | `DomainEvent` | ✅ 已完成 |
| cartisan-event | `DomainEventPublisher` | ✅ 已完成 (F02-08) |
| cartisan-data-jpa | `BaseRepository<T, ID>` | ✅ 已完成 (F02-04) |

---

## 注意事项

1. **构造器参数顺序**: `JpaEntityInformation`, `EntityManager`, `DomainEventPublisher`
2. **instanceof 模式匹配**: 使用 `instanceof AbstractAggregateRoot aggregateRoot` (Java 21)
3. **泛型约束**: `<T extends AggregateRoot<?>, ID extends Serializable>`
4. **package-info.java**: 在 `impl` 子包中添加 package-info 声明包用途
5. **测试隔离**: 单元测试使用 Mockito，集成测试使用 Testcontainers
