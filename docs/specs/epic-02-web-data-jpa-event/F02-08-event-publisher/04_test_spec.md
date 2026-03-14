# Feature: F02-08 cartisan-event 领域事件发布器 — 测试规格

> 版本：v0.1 | 日期：2026-03-14
> 状态：已完成

---

## 测试策略

### 单元测试

**目标**：验证 `SpringDomainEventPublisher` 的核心逻辑

| 测试方法 | 验证内容 | AC 覆盖 |
|---------|---------|---------|
| `given_publisher_when_publish_then_eventDelegatedToApplicationEventPublisher` | 事件正确委托给 Spring ApplicationEventPublisher | AC1 |
| `given_nullEvent_when_publish_then_throwsNullPointerException` | null 参数抛出 NPE，且不调用 ApplicationEventPublisher | AC1 |

**测试技术**：
- JUnit 5 + Mockito
- `@ExtendWith(MockitoExtension.class)`
- `@Mock` 模拟 `ApplicationEventPublisher`
- `verify()` 验证委托调用

### 集成测试

**目标**：验证 Spring Boot 自动配置和端到端功能

| 测试方法 | 验证内容 | AC 覆盖 |
|---------|---------|---------|
| `given_autoConfig_when_startup_then_domainEventPublisherBeanRegistered` | `DomainEventPublisher` Bean 自动注册 | AC4 |
| `given_eventListener_when_publish_then_eventCanBePublished` | 事件发布机制正常工作 | AC2 |
| `given_componentListener_when_publish_then_listenerExists` | 监听器组件可被扫描 | AC2 |

**测试技术**：
- `@SpringBootTest(classes = TestConfiguration.class)`
- 真实 Spring 上下文
- 验证 Bean 注册和事件发布

---

## 测试用例清单

### 单元测试用例

#### TC-UNIT-001: 正常发布事件
```java
given_publisher_when_publish_then_eventDelegatedToApplicationEventPublisher()
```
- **Given**: 有效的 `DomainEvent` 和 `SpringDomainEventPublisher`
- **When**: 调用 `publish(event)`
- **Then**: `ApplicationEventPublisher.publishEvent(event)` 被调用

#### TC-UNIT-002: null 事件抛出 NPE
```java
given_nullEvent_when_publish_then_throwsNullPointerException()
```
- **Given**: `SpringDomainEventPublisher` 已创建
- **When**: 调用 `publish(null)`
- **Then**: 抛出 `NullPointerException`，且 `ApplicationEventPublisher.publishEvent()` 不被调用

### 集成测试用例

#### TC-INT-001: 自动配置生效
```java
given_autoConfig_when_startup_then_domainEventPublisherBeanRegistered()
```
- **Given**: Spring Boot 启动
- **When**: 注入 `DomainEventPublisher`
- **Then**: Bean 存在且为 `SpringDomainEventPublisher` 实例

#### TC-INT-002: 事件发布机制正常
```java
given_eventListener_when_publish_then_eventCanBePublished()
```
- **Given**: 测试领域事件
- **When**: 发布事件
- **Then**: 无异常，事件属性正确

#### TC-INT-003: 监听器组件可扫描
```java
given_componentListener_when_publish_then_listenerExists()
```
- **Given**: `StaticTestEventListener` 是 `@Component`
- **When**: Spring 上下文启动
- **Then**: 组件存在于上下文中

---

## 测试覆盖范围

| AC | 描述 | 单元测试 | 集成测试 | 状态 |
|----|------|---------|---------|------|
| AC1 | 基础发布功能 | ✅ | ✅ | 已覆盖 |
| AC2 | 监听器可接收事件 | - | ✅ | 已覆盖 |
| AC3 | 支持事务后监听 | - | ⚠️ | 留给 F02-05 |
| AC4 | 自动配置生效 | - | ✅ | 已覆盖 |
| AC5 | 支持用户自定义覆盖 | - | ⚠️ | 实现支持，未单独测试 |

**注**：
- AC3（事务后监听）的完整验证在 F02-05（BaseRepositoryImpl）中进行，因为需要事务环境
- AC5（用户覆盖）通过 `@ConditionalOnMissingBean` 实现，未单独测试

---

## 测试执行结果

```bash
$ ./gradlew :cartisan-event:test

BUILD SUCCESSFUL
5 tests completed, 0 failed
```

**测试文件**：
- `SpringDomainEventPublisherTest.java` - 2 个单元测试
- `EventIntegrationTest.java` - 3 个集成测试

---

## 测试辅助类

### TestDomainEvent
测试用的 `DomainEvent` 子类，包含 `aggregateId` 和 `testData` 字段。

### StaticTestEventListener
测试用的 `@EventListener` 组件，验证监听器可以被 Spring 扫描和调用。

### TestConfiguration
集成测试配置类，使用 `@ImportAutoConfiguration` 导入 `CartisanEventAutoConfiguration`。
