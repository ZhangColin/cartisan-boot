# Feature: F02-08 cartisan-event 领域事件发布器 — 实施计划

> 版本：v0.1 | 日期：2026-03-14
> 基于：01_requirement.md + 02_interface.md

---

## 目标复述

实现 cartisan-event 模块，提供领域事件发布基础设施：
- `DomainEventPublisher` 接口：定义事件发布契约
- `SpringDomainEventPublisher` 实现：基于 Spring 4.2+ `ApplicationEventPublisher`
- `CartisanEventAutoConfiguration`：Spring Boot 自动配置
- 单元测试：验证发布和监听功能

---

## 变更范围

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 新增 | `settings.gradle.kts` | 添加 `include(":cartisan-event")` |
| 新增 | `cartisan-event/build.gradle.kts` | 模块构建配置 |
| 新增 | `cartisan-event/src/main/java/com/cartisan/event/DomainEventPublisher.java` | 发布器接口 |
| 新增 | `cartisan-event/src/main/java/com/cartisan/event/SpringDomainEventPublisher.java` | Spring 实现 |
| 新增 | `cartisan-event/src/main/java/com/cartisan/event/config/CartisanEventAutoConfiguration.java` | 自动配置 |
| 新增 | `cartisan-event/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 自动配置发现 |
| 新增 | `cartisan-event/src/test/java/com/cartisan/event/SpringDomainEventPublisherTest.java` | 单元测试 |

---

## 核心流程（伪代码）

### 发布流程
```
1. 业务代码注入 DomainEventPublisher
2. 调用 publish(domainEvent)
3. SpringDomainEventPublisher 委托给 ApplicationEventPublisher.publishEvent()
4. Spring 包装为 PayloadApplicationEvent<DomainEvent>
5. 匹配的 @EventListener 被调用
```

### 自动配置流程
```
1. Spring Boot 启动扫描 AutoConfiguration.imports
2. 加载 CartisanEventAutoConfiguration
3. 检查是否存在 DomainEventPublisher Bean
4. 若不存在，注册 SpringDomainEventPublisher
```

---

## 原子任务清单

### Step 1: 创建模块骨架

**文件**：`settings.gradle.kts`, `cartisan-event/build.gradle.kts`

**内容**：
- 在 `settings.gradle.kts` 添加 `include(":cartisan-event")`
- 创建 `cartisan-event/build.gradle.kts`，配置：
  - 依赖 `cartisan-core`（api）
  - 依赖 `spring-context`（implementation）
  - 依赖 `spring-boot-autoconfigure`（implementation）
  - 依赖 `spring-boot-starter-test`（testImplementation）

**验证**：`./gradlew :cartisan-event:build --dry-run` 成功

---

### Step 2: 编写接口和实现（契约代码化）

**文件**：
- `com.cartisan.event.DomainEventPublisher`
- `com.cartisan.event.SpringDomainEventPublisher`

**内容**：
- `DomainEventPublisher` 接口，定义 `publish(DomainEvent)` 方法
- `SpringDomainEventPublisher` 实现，构造函数注入 `ApplicationEventPublisher`
- 添加 JavaDoc 说明接口契约

**验证**：`./gradlew :cartisan-event:compileJava` 通过

---

### Step 3: 编写单元测试（红灯）

**文件**：`com.cartisan.event.SpringDomainEventPublisherTest`

**内容**：
- 测试正常发布：`given_publisher_when_publish_then_eventReceived`
- 测试 null 参数：`given_nullEvent_when_publish_then throwsNPE`
- 测试监听器接收：`given_eventListener_when_publish_then_listenerReceivedOriginalEvent`

**验证**：
- 编译通过
- 测试全红（实现类尚不完整或 Mock 配置待完成）

---

### Step 4: 完善实现（绿灯）

**文件**：`SpringDomainEventPublisher`

**内容**：
- 完善 `publish()` 方法实现
- 确保 null 检查

**验证**：
- `./gradlew :cartisan-event:test` 全绿
- 覆盖 `given_..._when_..._then_...` 所有测试场景

---

### Step 5: 编写自动配置

**文件**：
- `com.cartisan.event.config.CartisanEventAutoConfiguration`
- `META-INF/spring/...imports`

**内容**：
- `@Configuration` 类
- `@ConditionalOnMissingBean(DomainEventPublisher.class)`
- `@Bean` 方法注册 `SpringDomainEventPublisher`
- 创建自动配置发现文件

**验证**：
- 编译通过
- 文件路径正确

---

### Step 6: 集成测试（可选）

**文件**：`com.cartisan.event.EventIntegrationTest`

**内容**：
- Spring 上下文测试
- 验证自动配置生效
- 验证 `@EventListener` 能接收到事件
- 验证 `@TransactionalEventListener(phase = AFTER_COMMIT)` 行为

**验证**：集成测试通过

---

## 测试命名规范

遵循 `docs/skills/SKILL.md` 中的 **TEST-002** 规则：

```
given_{条件}_when_{操作}_then_{预期结果}
```

**示例**：
- `given_publisher_when_publish_then_eventReceived`
- `given_nullEvent_when_publish_then_throwsNullPointerException`
- `given_transactionalEventListener_when_publishAfterCommit_then_listenerExecutedAfterCommit`

---

## 依赖检查清单

- [ ] cartisan-core 模块已完成（提供 `DomainEvent` 基类）
- [ ] settings.gradle.kts 已添加 `cartisan-event` 模块
- [ ] build.gradle.kts 依赖配置正确

---

## 完成标准

- [ ] 所有测试绿灯
- [ ] ArchUnit 通过（无架构违规）
- [ ] 代码量在 80-120 行范围内
- [ ] JavaDoc 完整
- [ ] 自动配置文件路径正确
