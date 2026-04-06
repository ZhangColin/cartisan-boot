# 应用事件架构改造实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 删除领域事件机制，引入应用事件机制，支持Spring和未来MQ发布

**架构思路:**
- 删除领域层的事件复杂性（AbstractAggregateRoot、DomainEvent）
- 在应用层添加应用事件机制（ApplicationEvent、ApplicationEventPublisher）
- 使用 @PublishTo 注解标记发布方式，支持多发布器路由
- 监听器统一放在 endpoints/listener 层

**技术栈:**
- Java 21 / Spring Boot 3.4.x
- Spring Events（当前）、RabbitMQ/Kafka（未来扩展）
- JUnit 5 + AssertJ + Mockito + ArchUnit

---

## 文件结构映射

### cartisan-core 模块

```
cartisan-core/src/main/java/com/cartisan/core/domain/
├── AggregateRoot.java              # 修改：更新泛型参数 <T, ID>
├── AbstractAggregateRoot.java      # 删除：包含事件管理逻辑
└── DomainEvent.java                # 删除：领域事件基类

cartisan-core/src/test/java/com/cartisan/core/domain/
├── AbstractAggregateRootTest.java  # 删除：测试文件
└── DomainEventTest.java            # 删除：测试文件
```

### cartisan-event 模块（完全重写）

```
cartisan-event/src/main/java/com/cartisan/event/
├── ApplicationEvent.java                           # 新增：应用事件接口
├── ApplicationEventPublisher.java                  # 新增：发布器接口
├── PublishTo.java                                  # 新增：发布方式注解
├── CompositeApplicationEventPublisher.java        # 新增：复合发布器
└── impl/
    └── SpringApplicationEventPublisher.java         # 新增：Spring实现

cartisan-event/src/main/java/com/cartisan/event/config/
└── CartisanEventAutoConfiguration.java             # 重写：自动配置

cartisan-event/src/test/java/com/cartisan/event/
├── ApplicationEventTest.java                       # 新增：接口测试
├── CompositeApplicationEventPublisherTest.java     # 新增：复合发布器测试
└── CartisanEventAutoConfigurationTest.java         # 重写：配置测试
```

### cartisan-data-jpa 模块

```
cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/
├── BaseRepositoryImpl.java           # 修改：删除 save() 方法中的事件发布逻辑
└── DomainEventPublisherHolder.java  # 删除：静态持有者

cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/
├── BaseRepositoryImplTest.java       # 修改：更新测试用例
└── RepositoryEventPublishingIntegrationTest.java  # 删除：事件发布集成测试
```

### cartisan-test 模块（ArchUnit规则更新）

```
cartisan-test/src/main/java/com/cartisan/test/archunit/
├── CartisanLayeringRules.java      # 修改：更新聚合根规则
└── [其他规则文件]                  # 保持不变
```

---

## Task 1: 更新 cartisan-core 的 AggregateRoot 接口

**Files:**
- Modify: `cartisan-core/src/main/java/com/cartisan/core/domain/AggregateRoot.java`

- [ ] **Step 1: 更新 AggregateRoot 接口泛型参数**

将接口从 `<T>` 改为 `<T, ID>`：

```java
/**
 * 聚合根标记接口。
 *
 * <p>聚合根是 DDD 中的核心概念，表示一组相关对象的访问入口点。</p>
 *
 * <h3>聚合根特征</h3>
 * <ul>
 *   <li>拥有全局唯一标识</li>
 *   <li>负责维护其内部对象的不变性约束</li>
 *   <li>外部对象只能通过聚合根来访问其内部对象</li>
 * </ul>
 *
 * <h3>用途</h3>
 * <ul>
 *   <li>类型约束：只有聚合根才能有 {@code Repository}</li>
 *   <li>架构验证：通过 ArchUnit 验证聚合根的正确性</li>
 * </ul>
 *
 * @param <T> 聚合根类型
 * @param <ID> 标识符类型
 * @since 0.1.0
 */
public interface AggregateRoot<T, ID> {
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd cartisan-core && mvn compile`
Expected: 编译成功，无错误

- [ ] **Step 3: 运行相关测试**

Run: `cd cartisan-core && mvn test`
Expected: 测试通过

- [ ] **Step 4: 提交变更**

```bash
git add cartisan-core/src/main/java/com/cartisan/core/domain/AggregateRoot.java
git commit -m "refactor(core): update AggregateRoot interface to include ID generic type

- Change from <T> to <T, ID> for better type safety
- Aligns with Repository type constraints
- Supports future getId() method in interface if needed

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 删除 cartisan-core 的 AbstractAggregateRoot

**Files:**
- Delete: `cartisan-core/src/main/java/com/cartisan/core/domain/AbstractAggregateRoot.java`
- Delete: `cartisan-core/src/test/java/com/cartisan/core/domain/AbstractAggregateRootTest.java`

- [ ] **Step 1: 删除 AbstractAggregateRoot 类**

Run: `rm cartisan-core/src/main/java/com/cartisan/core/domain/AbstractAggregateRoot.java`

- [ ] **Step 2: 删除 AbstractAggregateRoot 测试**

Run: `rm cartisan-core/src/test/java/com/cartisan/core/domain/AbstractAggregateRootTest.java`

- [ ] **Step 3: 删除 DomainEvent 类**

Run: `rm cartisan-core/src/main/java/com/cartisan/core/domain/DomainEvent.java`

- [ ] **Step 4: 删除 DomainEvent 测试**

Run: `rm cartisan-core/src/test/java/com/cartisan/core/domain/DomainEventTest.java`

- [ ] **Step 5: 验证编译状态**

Run: `cd cartisan-core && mvn compile`
Expected: 编译可能会有失败，因为其他模块可能还在使用 `AbstractAggregateRoot`。这是预期的，会在后续任务中修复。如果core模块本身编译失败，检查是否有其他依赖问题。

- [ ] **Step 6: 提交变更**

```bash
git add cartisan-core/src/main/java/com/cartisan/core/domain/
git add cartisan-core/src/test/java/com/cartisan/core/domain/
git commit -m "refactor(core): remove domain event mechanism

- Delete AbstractAggregateRoot class with event management
- Delete DomainEvent base class
- Simplify aggregate roots to only implement AggregateRoot<T, ID> interface
- Reduces technical complexity in domain layer

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 创建应用事件接口（cartisan-event）

**Files:**
- Create: `cartisan-event/src/main/java/com/cartisan/event/ApplicationEvent.java`

- [ ] **Step 1: 创建 ApplicationEvent 接口**

```java
package com.cartisan.event;

import java.time.Instant;

/**
 * 应用事件基类。
 *
 * <p>应用事件用于跨上下文、跨系统的异步通信。</p>
 *
 * <p>当前实现：基于Spring事件机制（进程内）</p>
 * <p>未来扩展：支持RabbitMQ、Kafka等消息队列（跨进程）</p>
 *
 * <h3>事件元数据</h3>
 * <ul>
 *   <li>{@code eventId} - 事件的唯一标识符</li>
 *   <li>{@code occurredAt} - 事件发生时间</li>
 *   <li>{@code eventType} - 事件类型名称，用于路由</li>
 * </ul>
 *
 * <h3>实现建议</h3>
 * <p>推荐使用 Java Record 来实现事件类，确保不可变性和可序列化性。</p>
 *
 * @since 0.1.0
 */
public interface ApplicationEvent {

    /**
     * 获取事件唯一标识符。
     * @return 事件ID，格式为UUID字符串
     */
    String eventId();

    /**
     * 获取事件发生时间。
     * @return 事件发生时间（UTC）
     */
    Instant occurredAt();

    /**
     * 获取事件类型名称。
     * <p>用于消息队列的路由key或topic名称</p>
     * @return 事件类型，如 "order.created"
     */
    String eventType();
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd cartisan-event && mvn compile`
Expected: 编译成功

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-event/src/main/java/com/cartisan/event/ApplicationEvent.java
git commit -m "feat(event): add ApplicationEvent interface

- Sealed interface for type-safe application events
- Supports event metadata: eventId, occurredAt, eventType
- Designed for future MQ integration (RabbitMQ, Kafka)

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 创建应用事件发布器接口

**Files:**
- Create: `cartisan-event/src/main/java/com/cartisan/event/ApplicationEventPublisher.java`

- [ ] **Step 1: 创建 ApplicationEventPublisher 接口**

```java
package com.cartisan.event;

/**
 * 应用事件发布器接口。
 *
 * <p>定义了发布应用事件的契约，实现类负责将事件发布到具体的
 * 事件总线（如Spring Events、RabbitMQ、Kafka）。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 在应用服务中注入
 * public class OrderService {
 *     private final ApplicationEventPublisher eventPublisher;
 *
 *     public OrderService(ApplicationEventPublisher eventPublisher) {
 *         this.eventPublisher = eventPublisher;
 *     }
 *
 *     @Transactional
 *     public void createOrder(...) {
 *         Order order = new Order(...);
 *         orderRepository.save(order);
 *         // 手动发布应用事件
 *         eventPublisher.publishApplicationEvent(new OrderCreatedEvent(...));
 *     }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public interface ApplicationEventPublisher {

    /**
     * 获取发布器类型标识。
     * @return 类型标识，如 "spring"、"rabbitmq"、"kafka"
     */
    String getType();

    /**
     * 发布应用事件。
     *
     * @param event 要发布的领域事件，不能为 null
     * @throws NullPointerException 如果 event 为 null
     */
    void publishApplicationEvent(ApplicationEvent event);
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd cartisan-event && mvn compile`
Expected: 编译成功

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-event/src/main/java/com/cartisan/event/ApplicationEventPublisher.java
git commit -m "feat(event): add ApplicationEventPublisher interface

- Defines contract for publishing application events
- Supports multiple implementations (Spring, RabbitMQ, Kafka)
- Each publisher has a type identifier for routing

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 创建 @PublishTo 注解

**Files:**
- Create: `cartisan-event/src/main/java/com/cartisan/event/PublishTo.java`

- [ ] **Step 1: 创建 @PublishTo 注解**

```java
package com.cartisan.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 标记事件发布方式。
 *
 * <p>支持同时发布到多个目标（如Spring + RabbitMQ）。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 同时发布到Spring和RabbitMQ
 * @PublishTo({"spring", "rabbitmq"})
 * public record OrderCreatedEvent(...) implements ApplicationEvent { ... }
 *
 * // 只发布到Spring
 * @PublishTo("spring")
 * public record OrderShippedEvent(...) implements ApplicationEvent { ... }
 * }</pre>
 *
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishTo {
    /**
     * 发布目标类型。
     * @return 类型数组，如 {"spring", "rabbitmq"}
     */
    String[] value();
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd cartisan-event && mvn compile`
Expected: 编译成功

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-event/src/main/java/com/cartisan/event/PublishTo.java
git commit -m "feat(event): add @PublishTo annotation

- Marks which publishers should receive an event
- Supports multiple publishers (e.g., spring + rabbitmq)
- Enables flexible event routing strategies

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 6: 创建 Spring 事件发布器实现

**Files:**
- Create: `cartisan-event/src/main/java/com/cartisan/event/impl/SpringApplicationEventPublisher.java`

- [ ] **Step 1: 创建 SpringApplicationEventPublisher 类**

```java
package com.cartisan.event.impl;

import com.cartisan.event.ApplicationEvent;
import com.cartisan.event.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 基于Spring事件机制的应用事件发布器。
 *
 * <p>将 {@link ApplicationEvent} 直接发布到Spring事件总线。</p>
 * <p>发布失败不抛异常，只记录日志，避免影响主业务。</p>
 *
 * @since 0.1.0
 */
@Component
@ConditionalOnProperty(
    name = "cartisan.event.publisher.spring.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SpringApplicationEventPublisher implements ApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringApplicationEventPublisher.class);

    private final org.springframework.context.ApplicationEventPublisher publisher;

    /**
     * 创建Spring事件发布器。
     * @param publisher Spring事件发布器，不能为 null
     */
    public SpringApplicationEventPublisher(
            org.springframework.context.ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher cannot be null");
    }

    @Override
    public String getType() {
        return "spring";
    }

    @Override
    public void publishApplicationEvent(ApplicationEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        try {
            publisher.publishEvent(event);
        } catch (Exception e) {
            log.error("发布Spring事件失败: eventId={}, eventType={}",
                event.eventId(), event.eventType(), e);
            // 不抛出异常，避免影响主业务
        }
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd cartisan-event && mvn compile`
Expected: 编译成功

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-event/src/main/java/com/cartisan/event/impl/
git commit -m "feat(event): add SpringApplicationEventPublisher

- Implements ApplicationEventPublisher using Spring Events
- Enabled by default, can be disabled via configuration
- Publishes events synchronously within the same JVM

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 7: 创建复合事件发布器

**Files:**
- Create: `cartisan-event/src/main/java/com/cartisan/event/CompositeApplicationEventPublisher.java`

- [ ] **Step 1: 创建 CompositeApplicationEventPublisher 类**

```java
package com.cartisan.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 复合应用事件发布器。
 *
 * <p>根据事件的 {@link PublishTo} 注解自动路由到对应的发布器。</p>
 *
 * <h3>路由规则</h3>
 * <ul>
 *   <li>如果事件有 {@code @PublishTo} 注解，按注解指定的发布器路由</li>
 *   <li>如果没有注解，默认使用 Spring 发布器</li>
 *   <li>支持同时发布到多个发布器（如 {@code @PublishTo({"spring", "rabbitmq"})}）</li>
 * </ul>
 *
 * @since 0.1.0
 */
@Component
public class CompositeApplicationEventPublisher implements ApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CompositeApplicationEventPublisher.class);

    private final Map<String, ApplicationEventPublisher> publishers;

    /**
     * 创建复合发布器。
     * @param publisherList 所有可用的发布器
     */
    public CompositeApplicationEventPublisher(List<ApplicationEventPublisher> publisherList) {
        this.publishers = publisherList.stream()
            .collect(Collectors.toMap(
                ApplicationEventPublisher::getType,
                Function.identity()
            ));

        log.info("Initialized event publishers: {}", publishers.keySet());
    }

    @Override
    public String getType() {
        return "composite";
    }

    @Override
    public void publishApplicationEvent(ApplicationEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        PublishTo annotation = event.getClass().getAnnotation(PublishTo.class);

        if (annotation == null) {
            // 默认使用Spring发布器
            publishTo("spring", event);
            return;
        }

        // 按注解标记发布到多个目标
        for (String type : annotation.value()) {
            publishTo(type, event);
        }
    }

    private void publishTo(String type, ApplicationEvent event) {
        ApplicationEventPublisher publisher = publishers.get(type);
        if (publisher == null) {
            throw new IllegalArgumentException(
                "No publisher found for type: " + type +
                ". Available types: " + publishers.keySet()
            );
        }

        try {
            publisher.publishApplicationEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish event to {}: eventId={}, eventType={}",
                type, event.eventId(), event.eventType(), e);
            // 不抛出异常，避免影响主业务
        }
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd cartisan-event && mvn compile`
Expected: 编译成功

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-event/src/main/java/com/cartisan/event/CompositeApplicationEventPublisher.java
git commit -m "feat(event): add CompositeApplicationEventPublisher

- Routes events to appropriate publishers based on @PublishTo annotation
- Default to Spring publisher if no annotation present
- Supports multiple publishers per event
- Handles publishing failures gracefully (logs error, doesn't throw)

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 8: 更新 cartisan-event 自动配置

**Files:**
- Modify: `cartisan-event/src/main/java/com/cartisan/event/config/CartisanEventAutoConfiguration.java`

- [ ] **Step 1: 更新自动配置类**

完全替换文件内容为：

```java
package com.cartisan.event.config;

import com.cartisan.event.ApplicationEventPublisher;
import com.cartisan.event.CompositeApplicationEventPublisher;
import com.cartisan.event.impl.SpringApplicationEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * cartisan-event 模块的 Spring Boot 自动配置。
 *
 * <p>自动配置应用事件发布器：</p>
 * <ul>
 *   <li>{@link CompositeApplicationEventPublisher} - 复合发布器，根据注解路由</li>
 *   <li>{@link SpringApplicationEventPublisher} - Spring事件发布器</li>
 * </ul>
 *
 * @since 0.1.0
 */
@AutoConfiguration
public class CartisanEventAutoConfiguration {

    /**
     * 注册Spring事件发布器 Bean。
     *
     * @param springPublisher Spring事件发布器
     * @return Spring事件发布器实例
     */
    @Bean
    public ApplicationEventPublisher springEventPublisher(
            ApplicationEvent springPublisher) {
        return new SpringApplicationEventPublisher(springPublisher);
    }

    /**
     * 注册复合事件发布器 Bean。
     *
     * @param publishers 所有可用的发布器
     * @return 复合发布器实例
     */
    @Bean
    public ApplicationEventPublisher applicationEventPublisher(
            List<ApplicationEventPublisher> publishers) {
        return new CompositeApplicationEventPublisher(publishers);
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd cartisan-event && mvn compile`
Expected: 编译成功

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-event/src/main/java/com/cartisan/event/config/CartisanEventAutoConfiguration.java
git commit -m "refactor(event): update auto-configuration for application events

- Remove domain event publisher configuration
- Add composite publisher with Spring implementation
- Auto-wire all available publishers for routing

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 9: 删除 cartisan-event 的旧文件

**Files:**
- Delete: `cartisan-event/src/main/java/com/cartisan/event/DomainEventPublisher.java`
- Delete: `cartisan-event/src/main/java/com/cartisan/event/SpringDomainEventPublisher.java`
- Delete: `cartisan-event/src/test/java/com/cartisan/event/SpringDomainEventPublisherTest.java`
- Delete: `cartisan-event/src/test/java/com/cartisan/event/EventIntegrationTest.java`

- [ ] **Step 1: 删除旧文件**

Run:
```bash
rm cartisan-event/src/main/java/com/cartisan/event/DomainEventPublisher.java
rm cartisan-event/src/main/java/com/cartisan/event/SpringDomainEventPublisher.java
rm cartisan-event/src/test/java/com/cartisan/event/SpringDomainEventPublisherTest.java
rm cartisan-event/src/test/java/com/cartisan/event/EventIntegrationTest.java
```

- [ ] **Step 2: 提交变更**

```bash
git add cartisan-event/src/main/java/com/cartisan/event/
git add cartisan-event/src/test/java/com/cartisan/event/
git commit -m "refactor(event): remove domain event publishers

- Delete DomainEventPublisher interface
- Delete SpringDomainEventPublisher implementation
- Remove associated tests
- Clean up for new application event mechanism

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 10: 删除 cartisan-data-jpa 的事件发布逻辑

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java`
- Delete: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/DomainEventPublisherHolder.java`

- [ ] **Step 1: 更新 BaseRepositoryImpl 的 save() 方法**

查找并删除事件发布相关代码：

1. 删除 `save()` 方法中的 `publishDomainEvents(entity);` 调用
2. 删除完整的 `publishDomainEvents()` 方法（如果存在）
3. 删除以下import语句：
   - `import com.cartisan.core.domain.AbstractAggregateRoot;`
   - `import com.cartisan.core.domain.DomainEvent;`

更新后的 `save()` 方法应该只保留JPA保存逻辑：

```java
@Override
@SuppressWarnings("unchecked")
public <S extends T> S save(S entity) {
    // 只保留 JPA 保存逻辑，删除事件发布
    return super.save(entity);
}
```

- [ ] **Step 2: 删除 DomainEventPublisherHolder**

Run: `rm cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/DomainEventPublisherHolder.java`

- [ ] **Step 3: 验证编译通过**

Run: `cd cartisan-data-jpa && mvn compile`
Expected: 编译成功

- [ ] **Step 4: 提交变更**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/
git commit -m "refactor(data-jpa): remove automatic domain event publishing

- Remove event publishing logic from BaseRepositoryImpl.save()
- Remove DomainEventPublisherHolder utility class
- Event publishing now handled manually by application services

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 11: 删除 cartisan-data-jpa 的事件相关测试

**Files:**
- Check: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/RepositoryEventPublishingIntegrationTest.java`

- [ ] **Step 1: 检查并删除事件发布集成测试**

首先检查文件是否存在：
Run: `ls cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/`

如果存在 `RepositoryEventPublishingIntegrationTest.java` 或其他事件相关的测试文件，删除它：
Run: `rm cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/RepositoryEventPublishingIntegrationTest.java`

- [ ] **Step 2: 验证测试通过**

Run: `cd cartisan-data-jpa && mvn test`
Expected: 测试通过

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/
git commit -m "test(data-jpa): remove domain event publishing tests

- Delete RepositoryEventPublishingIntegrationTest
- Remove tests for automatic domain event publishing
- Event testing now part of application layer tests

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 12: 更新 cartisan-core 示例代码

**Files:**
- Modify: `cartisan-core/src/test/java/com/cartisan/examples/domain/Order.java`

- [ ] **Step 1: 更新 Order 聚合根示例**

将 `Order` 类从继承 `AbstractAggregateRoot` 改为实现 `AggregateRoot<Order, Long>`，并删除事件相关代码：

```java
package com.cartisan.examples.domain;

import com.cartisan.core.domain.AggregateRoot;
// 其他必要的imports...

public class Order implements AggregateRoot<Order, OrderId> {

    private final OrderId id;
    private final List<OrderItem> items;
    private ShippingAddress shippingAddress;
    private OrderStatus status;

    private Order(OrderId id, List<OrderItem> items, ShippingAddress shippingAddress, OrderStatus status) {
        this.id = Objects.requireNonNull(id, "Order ID cannot be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "Items cannot be null"));
        this.shippingAddress = shippingAddress;
        this.status = status != null ? status : OrderStatus.PENDING;
    }

    public static Order create(List<OrderItem> items, ShippingAddress shippingAddress) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        Order order = new Order(OrderId.generate(), items, shippingAddress, OrderStatus.PENDING);
        // 不再发布事件
        return order;
    }

    public static Order reconstruct(OrderId id, List<OrderItem> items, ShippingAddress shippingAddress, OrderStatus status) {
        return new Order(id, items, shippingAddress, status);
    }

    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
        // 不再发布事件
    }

    public void ship() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be shipped");
        }
        if (shippingAddress == null) {
            throw new IllegalStateException("Cannot ship order without shipping address");
        }
        this.status = OrderStatus.SHIPPED;
        // 不再发布事件
    }

    // ... 其他方法保持不变
}
```

删除所有事件类：`OrderCreatedEvent`、`OrderConfirmedEvent`、`OrderShippedEvent`

- [ ] **Step 2: 验证编译通过**

Run: `cd cartisan-core && mvn compile`
Expected: 编译成功

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-core/src/test/java/com/cartisan/examples/domain/
git commit -m "refactor(core): update Order example to remove domain events

- Change from AbstractAggregateRoot to AggregateRoot<Order, Long>
- Remove event registration calls (registerEvent)
- Remove event classes (OrderCreatedEvent, OrderConfirmedEvent, OrderShippedEvent)
- Simplify aggregate root design

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 13: 添加应用事件示例

**Files:**
- Create: `cartisan-event/src/test/java/com/cartisan/event/example/OrderCreatedEvent.java`

- [ ] **Step 1: 创建应用事件示例**

```java
package com.cartisan.event.example;

import com.cartisan.event.ApplicationEvent;
import com.cartisan.event.PublishTo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 订单创建事件（示例）。
 */
@PublishTo({"spring", "rabbitmq"})
public record OrderCreatedEvent(
    String eventId,
    Instant occurredAt,
    Long orderId,
    String customerId,
    BigDecimal totalAmount
) implements ApplicationEvent {

    public OrderCreatedEvent(Long orderId, String customerId, BigDecimal totalAmount) {
        this(UUID.randomUUID().toString(), Instant.now(), orderId, customerId, totalAmount);
    }

    @Override
    public String eventType() {
        return "order.created";
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd cartisan-event && mvn compile`
Expected: 编译成功

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-event/src/test/java/com/cartisan/event/example/
git commit -m "feat(event): add OrderCreatedEvent example

- Demonstrates ApplicationEvent with Record
- Shows @PublishTo annotation for multiple publishers
- Includes eventId, occurredAt, eventType metadata
- Ready for testing and reference

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 14: 添加复合发布器测试

**Files:**
- Create: `cartisan-event/src/test/java/com/cartisan/event/CompositeApplicationEventPublisherTest.java`

- [ ] **Step 1: 创建复合发布器测试**

```java
package com.cartisan.event;

import com.cartisan.event.example.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeApplicationEventPublisherTest {

    @Mock
    private ApplicationEventPublisher springPublisher;

    @Mock
    private ApplicationEventPublisher rabbitmqPublisher;

    private CompositeApplicationEventPublisher compositePublisher;

    @BeforeEach
    void setUp() {
        when(springPublisher.getType()).thenReturn("spring");
        when(rabbitmqPublisher.getType()).thenReturn("rabbitmq");

        compositePublisher = new CompositeApplicationEventPublisher(
            List.of(springPublisher, rabbitmqPublisher)
        );
    }

    @Test
    void should_publish_to_spring_when_annotation_has_single_type() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(1L, "customer", BigDecimal.valueOf(100));

        // When
        compositePublisher.publishApplicationEvent(event);

        // Then
        verify(springPublisher).publishApplicationEvent(event);
        verify(rabbitmqPublisher, never()).publishApplicationEvent(any());
    }

    @Test
    void should_publish_to_multiple_publishers_when_annotation_has_multiple_types() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(1L, "customer", BigDecimal.valueOf(100));

        // When
        compositePublisher.publishApplicationEvent(event);

        // Then
        verify(springPublisher).publishApplicationEvent(event);
        verify(rabbitmqPublisher).publishApplicationEvent(event);
    }

    @Test
    void should_throw_exception_when_publisher_type_not_found() {
        // Given
        ApplicationEvent event = new ApplicationEvent() {
            @Override
            public String eventId() { return "test-id"; }
            @Override
            public Instant occurredAt() { return Instant.now(); }
            @Override
            public String eventType() { return "test.event"; }
        };

        // When & Then
        assertThatThrownBy(() -> compositePublisher.publishApplicationEvent(event))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No publisher found for type");
    }
}
```

- [ ] **Step 2: 验证测试通过**

Run: `cd cartisan-event && mvn test -Dtest=CompositeApplicationEventPublisherTest`
Expected: 测试全部通过

- [ ] **Step 3: 提交变更**

```bash
git add cartisan-event/src/test/java/com/cartisan/event/CompositeApplicationEventPublisherTest.java
git commit -m "test(event): add CompositeApplicationEventPublisher tests

- Test single publisher routing
- Test multiple publisher routing
- Test exception when publisher not found
- Uses Mockito to mock publishers

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 15: 更新 ArchUnit 规则

**Files:**
- Modify: `cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java`

- [ ] **Step 1: 检查现有ArchUnit规则**

首先使用grep检查是否存在关于 `AbstractAggregateRoot` 的规则：
Run: `grep -n "AbstractAggregateRoot" cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java`

如果找到了相关规则，继续Step 2。如果没有找到，跳过删除步骤，直接执行Step 3。

- [ ] **Step 2: 删除旧规则（如果存在）**

如果Step 1找到了 `aggregates_should_extend_AbstractAggregateRoot` 或类似的规则，完全删除它。

- [ ] **Step 3: 添加新规则**

在 `CartisanLayeringRules.java` 中添加以下规则：

```java
// ✅ 新增规则：聚合根必须实现AggregateRoot接口
@ArchTest
static final ArchRule aggregates_should_implement_AggregateRoot = classes()
    .that().areAnnotatedWith(Aggregate.class)
    .should().implementInterface(AggregateRoot.class);
```

- [ ] **Step 4: 验证测试通过**

Run: `cd cartisan-test && mvn test -Dtest=CartisanLayeringRules`
Expected: 测试通过

- [ ] **Step 5: 提交变更**

```bash
git add cartisan-test/src/main/java/com/cartisan/test/archunit/CartisanLayeringRules.java
git commit -m "refactor(test): update ArchUnit rules for application events

- Remove rule: aggregates must extend AbstractAggregateRoot
- Add rule: aggregates must implement AggregateRoot interface
- Align with new aggregate root design (no base class)

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 16: 更新 PITFALLS.md 文档

**Files:**
- Modify: `docs/PITFALLS.md`

- [ ] **Step 1: 添加应用事件相关规则**

在文档末尾添加：

```markdown
## 应用事件 (ApplicationEvent)

### 规则 EVENT-001：应用事件使用Record + @PublishTo

**推荐做法**：
```java
@PublishTo({"spring", "rabbitmq"})
public record OrderCreatedEvent(
    String eventId,
    Instant occurredAt,
    Long orderId,
    String customerId
) implements ApplicationEvent {
    // 自动生成 eventId、occurredAt
    public OrderCreatedEvent(Long orderId, String customerId) {
        this(UUID.randomUUID().toString(), Instant.now(), orderId, customerId);
    }

    @Override
    public String eventType() {
        return "order.created";  // 用于MQ路由
    }
}
```

**记忆口诀**：应用事件用Record，@PublishTo标记发布方式。

---

### 规则 EVENT-002：事件发布在事务提交后

**推荐做法**：
```java
// 应用服务
@Transactional
public void createOrder(...) {
    orderRepository.save(order);
    // 事件发布在事务提交后
}

// 监听器
@Component
public class OrderEventListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(OrderCreatedEvent event) {
        // 在新事务中执行
    }
}
```

**记忆口诀**：事件监听用AFTER_COMMIT，新事务隔离。

---

### 规则 EVENT-003：应用事件发布失败不影响主业务

**推荐做法**：
```java
@Override
public void publishApplicationEvent(ApplicationEvent event) {
    try {
        publisher.publishEvent(event);
    } catch (Exception e) {
        log.error("发布事件失败: eventId={}", event.eventId(), e);
        // 不抛异常，避免影响主业务
    }
}
```

**记忆口诀**：事件发布失败吞异常，记录日志即可。
```

- [ ] **Step 2: 提交变更**

```bash
git add docs/PITFALLS.md
git commit -m "docs: add application event rules to PITFALLS.md

- EVENT-001: ApplicationEvent uses Record + @PublishTo
- EVENT-002: Event publishing AFTER_COMMIT with new transaction
- EVENT-003: Event publishing failures don't break main business

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 17: 更新使用手册

**Files:**
- Modify: `docs/guide/cartisan-boot-使用手册.md`
- Modify: `docs/guide/限界上下文代码编写规范.md`

- [ ] **Step 1: 更新使用手册**

查找并删除/更新领域事件相关内容，添加应用事件说明。

- [ ] **Step 2: 更新编写规范**

查找并更新聚合根示例，删除领域事件相关内容。

- [ ] **Step 3: 提交变更**

```bash
git add docs/guide/
git commit -m "docs: update guides for application events

- Remove domain event references
- Add application event usage examples
- Update aggregate root examples
- Update event listener examples

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## Task 18: 运行全量测试验证

**Files:**
- Test all modules

- [ ] **Step 1: 运行 cartisan-core 测试**

Run: `cd cartisan-core && mvn test`
Expected: 测试通过

- [ ] **Step 2: 运行 cartisan-event 测试**

Run: `cd cartisan-event && mvn test`
Expected: 测试通过

- [ ] **Step 3: 运行 cartisan-data-jpa 测试**

Run: `cd cartisan-data-jpa && mvn test`
Expected: 测试通过

- [ ] **Step 4: 运行 cartisan-test ArchUnit 测试**

Run: `cd cartisan-test && mvn test`
Expected: 测试通过

- [ ] **Step 5: 提交最终变更**

```bash
git add .
git commit -m "test: verify all modules pass after application event migration

- cartisan-core: all tests pass
- cartisan-event: all tests pass
- cartisan-data-jpa: all tests pass
- cartisan-test: ArchUnit rules pass

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

## 验收标准

### 功能验收

- [ ] AggregateRoot 接口更新为 `<T, ID>` 泛型
- [ ] AbstractAggregateRoot、DomainEvent 已删除
- [ ] ApplicationEvent 接口已创建
- [ ] ApplicationEventPublisher 接口已创建
- [ ] @PublishTo 注解已创建
- [ ] CompositeApplicationEventPublisher 已创建
- [ ] SpringApplicationEventPublisher 已创建
- [ ] BaseRepositoryImpl 不再自动发布事件
- [ ] ArchUnit 规则已更新

### 测试验收

- [ ] cartisan-core 测试通过
- [ ] cartisan-event 测试通过
- [ ] cartisan-data-jpa 测试通过
- [ ] cartisan-test ArchUnit 规则通过

### 文档验收

- [ ] PITFALLS.md 已添加应用事件规则
- [ ] 使用手册已更新
- [ ] 编写规范已更新

---

**实施计划完成！** 总计18个任务，预计耗时：2-3小时

**下一步：** 使用 superpowers:subagent-driven-development 或 superpowers:executing-plans 执行此计划
