# Feature: F02-08 cartisan-event 领域事件发布器 — 接口契约

> 版本：v0.1 | 日期：2026-03-14
> 基于：01_requirement.md

---

## 领域接口描述（伪代码）

### 服务接口：DomainEventPublisher

**职责**：发布领域事件到 Spring 事件总线

**方法签名**：
```
publish(event: DomainEvent) → void
```

**前置条件**：
- `event` 不能为 null
- 事件应包含有效的 `aggregateId`

**后置条件**：
- 事件被发布到 Spring 事件总线
- Spring 自动将事件包装为 `PayloadApplicationEvent<DomainEvent>`

**异常**：
- `NullPointerException`：当 `event` 为 null 时抛出

**实现约束**：
- 使用 Spring 的 `ApplicationEventPublisher.publishEvent(Object)` 方法
- 不做自定义包装，直接传递 `DomainEvent` 实例

---

## 类结构设计

### DomainEventPublisher（接口）
```
包路径：com.cartisan.event

职责：
- 定义领域事件发布器的契约

方法：
- publish(DomainEvent event): void
```

### SpringDomainEventPublisher（实现类）
```
包路径：com.cartisan.event

职责：
- 实现 DomainEventPublisher 接口
- 委托给 Spring 的 ApplicationEventPublisher

依赖：
- ApplicationEventPublisher（通过构造函数注入）

方法：
- publish(DomainEvent event): void
  - 调用 applicationEventPublisher.publishEvent(event)
```

### CartisanEventAutoConfiguration（自动配置）
```
包路径：com.cartisan.event.config

职责：
- 注册 SpringDomainEventPublisher 为 Bean
- 支持用户自定义实现覆盖

注解：
- @Configuration
- @ConditionalOnMissingBean(DomainEventPublisher.class)

方法：
- @Bean DomainEventPublisher domainEventPublisher(ApplicationEventPublisher)
```

---

## 自动配置发现文件

**文件路径**：
```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**内容**：
```
com.cartisan.event.config.CartisanEventAutoConfiguration
```

**作用**：
- Spring Boot 2.7+ 自动配置发现机制
- 引入 cartisan-event 依赖后自动加载配置

---

## 核心流程

### 发布流程
```
1. 业务代码调用 DomainEventPublisher.publish(event)
   ↓
2. SpringDomainEventPublisher 委托给 ApplicationEventPublisher
   ↓
3. Spring 将事件包装为 PayloadApplicationEvent<DomainEvent>
   ↓
4. Spring 调用匹配的 @EventListener 方法
   ↓
5. 监听器接收原始 DomainEvent 子类实例
```

### 事务后监听流程
```
1. @TransactionalEventListener(phase = AFTER_COMMIT) 标注监听器
   ↓
2. 事务内调用 publish(event)
   ↓
3. 事务提交前，监听器不执行
   ↓
4. 事务成功提交后，Spring 调用监听器
   ↓
5. 如果事务回滚，监听器永不执行
```

---

## 使用示例（伪代码）

### 业务代码发布事件
```
// 聚合根中注册事件（cartisan-core 已提供）
class Order extends AbstractAggregateRoot<Order> {
    void confirm() {
        this.status = CONFIRMED;
        registerEvent(new OrderConfirmedEvent(id.value()));
    }
}

// Repository 中自动发布（F02-05 实现）
class BaseRepositoryImpl<T> {
    save(T entity) {
        entityManager.persist(entity);
        domainEventPublisher.publish(entity.getDomainEvents());
        entity.clearDomainEvents();
    }
}
```

### 业务监听器接收事件
```
@Component
class OrderEventHandler {

    // 推荐：事务提交后执行
    @TransactionalEventListener(phase = AFTER_COMMIT)
    void handle(OrderConfirmedEvent event) {
        // 发送通知
        notificationService.send(event);
    }

    // 或：事务内同步执行
    @EventListener
    void handle2(OrderConfirmedEvent event) {
        // 同库操作
    }
}
```

---

## 依赖配置

### cartisan-event/build.gradle.kts
```
dependencies {
    // 依赖 cartisan-core 获取 DomainEvent 基类
    api(project(":cartisan-core"))

    // Spring 上下文，提供 ApplicationEventPublisher
    implementation("org.springframework:spring-context")

    // Spring Boot 自动配置注解
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // 测试依赖
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

---

## 无数据库变更

本 Feature 不涉及数据库操作，无需 Flyway 迁移脚本。

---

## 扩展点

### 用户自定义发布器实现
```
// 用户可通过定义自己的 Bean 覆盖默认实现
@Configuration
class CustomEventConfig {
    @Bean
    DomainEventPublisher customPublisher() {
        return new KafkaDomainEventPublisher(); // 例如发到 Kafka
    }
}
```

由于 `@ConditionalOnMissingBean(DomainEventPublisher.class)` 的存在，
用户定义 Bean 后，默认的 `SpringDomainEventPublisher` 不会注册。
