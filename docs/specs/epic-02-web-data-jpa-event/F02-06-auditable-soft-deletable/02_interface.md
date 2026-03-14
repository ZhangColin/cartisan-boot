# Feature: cartisan-data-jpa 审计与软删除 — 接口契约

> **Feature ID**: F02-06
> **Phase**: 2 — Design
> **注意**：本文档为技术设计文档，使用伪代码和表格描述。Java 源代码在 Phase 4 生成。

---

## 1. 类/接口设计

### 1.1 Auditable 基类

**全名**：`com.cartisan.data.jpa.domain.Auditable`

**职责**：提供 JPA 审计字段的基类，实体继承后自动获得创建时间、修改时间、创建人、修改人 4 个字段。

**伪代码**：

```java
package com.cartisan.data.jpa.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;

/**
 * 可审计实体基类。
 *
 * <p>实体继承此基类后，JPA Auditing 会自动填充审计字段：</p>
 * <ul>
 *   <li>{@code createdAt} — 首次保存时自动填充</li>
 *   <li>{@code lastModifiedDate} — 每次保存时自动更新</li>
 *   <li>{@code createdBy} — 首次保存时从 {@code AuditorAware} 获取</li>
 *   <li>{@code lastModifiedBy} — 每次保存时从 {@code AuditorAware} 获取</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Entity
 * public class Order extends Auditable {
 *     @Id private Long id;
 *     // ...
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
@MappedSuperclass
public abstract class Auditable {

    /**
     * 创建时间，实体首次持久化时自动填充。
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最后修改时间，每次保存时自动更新。
     */
    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    /**
     * 创建人，首次持久化时从 AuditorAware 获取。
     *
     * <p>若容器中不存在 AuditorAware Bean，此字段保持 null。</p>
     */
    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * 最后修改人，每次保存时从 AuditorAware 获取。
     *
     * <p>若容器中不存在 AuditorAware Bean，此字段保持 null。</p>
     */
    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    // Getter 方法（可选，JPA 直接访问字段）
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public String getCreatedBy() { return createdBy; }
    public String getLastModifiedBy() { return lastModifiedBy; }
}
```

**数据库字段**：

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `created_at` | `TIMESTAMP` | `NOT NULL` | 创建时间 |
| `last_modified_date` | `TIMESTAMP` | `NOT NULL` | 最后修改时间 |
| `created_by` | `VARCHAR(100)` | `NULL` | 创建人 |
| `last_modified_by` | `VARCHAR(100)` | `NULL` | 最后修改人 |

---

### 1.2 SoftDeletable 基类

**全名**：`com.cartisan.data.jpa.domain.SoftDeletable`

**职责**：继承 `Auditable`，增加软删除标记和查询过滤能力。

**伪代码**：

```java
package com.cartisan.data.jpa.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

/**
 * 可软删除实体基类。
 *
 * <p>继承 {@link Auditable}，增加软删除能力：</p>
 * <ul>
 *   <li>{@code deleted} 字段标记是否已删除</li>
 *   <li>{@code @SQLRestriction} 在查询时自动过滤 {@code deleted = true} 的记录</li>
 * </ul>
 *
 * <h3>软删除行为</h3>
 * <ul>
 *   <li>调用 {@code repository.delete(entity)} 会将 {@code deleted} 设为 {@code true}</li>
 *   <li>调用 {@code repository.deleteById(id)} 会将对应记录的 {@code deleted} 设为 {@code true}</li>
 *   <li>所有查询（如 {@code findAll()}）自动排除 {@code deleted = true} 的记录</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Entity
 * public class Product extends SoftDeletable {
 *     @Id private Long id;
 *     private String name;
 *     // ...
 * }
 *
 * // 使用
 * productRepository.delete(product);  // deleted = true
 * productRepository.findAll();        // 不包含已删除记录
 * }</pre>
 *
 * @since 0.2.0
 */
@MappedSuperclass
@SQLRestriction("deleted = false")
public abstract class SoftDeletable extends Auditable {

    /**
     * 软删除标记。
     *
     * <p>{@code false} = 未删除（默认），{@code true} = 已删除。</p>
     * <p>注意：@SQLRestriction 会在查询时自动过滤 {@code deleted = true} 的记录。</p>
     */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /**
     * 判断是否已软删除。
     *
     * @return true 表示已删除，false 表示未删除
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * 获取软删除标记值。
     *
     * @return deleted 字段值
     */
    public boolean getDeleted() {
        return deleted;
    }

    /**
     * 设置软删除标记。
     *
     * <p>通常不需要手动调用，通过 {@code repository.delete(entity)} 触发。</p>
     *
     * @param deleted 删除标记
     */
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
```

**数据库字段**（在 Auditable 基础上增加）：

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `deleted` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | 软删除标记 |

---

### 1.3 JpaAuditingConfiguration 配置类

**全名**：`com.cartisan.data.jpa.config.JpaAuditingConfiguration`

**职责**：配置 JPA Auditing，条件装配 `AuditorAware`。

**伪代码**：

```java
package com.cartisan.data.jpa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 自动配置。
 *
 * <p>仅在容器中存在 {@link AuditorAware} Bean 时启用 JPA Auditing。</p>
 *
 * <h3>条件装配</h3>
 * <ul>
 *   <li>若存在 {@code AuditorAware<?>} Bean：启用 auditing，注入该 AuditorAware</li>
 *   <li>若不存在：不启用 auditing，@CreatedBy/@LastModifiedBy 保持 null</li>
 * </ul>
 *
 * <h3>业务项目集成示例</h3>
 * <pre>{@code
 * // 在 cartisan-security 或业务项目中
 * @Bean
 * public AuditorAware<String> auditorAware() {
 *     return () -> {
 *         // 从 SecurityContext 获取当前用户
 *         String currentUser = SecurityContext.getCurrentUser();
 *         return Optional.ofNullable(currentUser);
 *     };
 * }
 * }</pre>
 *
 * @since 0.2.0
 */
@Configuration
@ConditionalOnBean(AuditorAware.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfiguration {
    // 无需额外代码，注解即完成配置
}
```

**设计说明**：
- `@ConditionalOnBean(AuditorAware.class)` 确保只有存在 `AuditorAware` Bean 时才启用
- `auditorAwareRef = "auditorAware"` 默认查找名为 `auditorAware` 的 Bean
- 业务项目可通过 `@Bean public AuditorAware<String> auditorAware()` 提供实现

---

## 2. 与现有模块集成

### 2.1 CartisanDataJpaAutoConfiguration 扩展

**现有**：`CartisanDataJpaAutoConfiguration` 已配置 `DomainEventPublisherHolder`

**变更**：无需修改现有配置，`JpaAuditingConfiguration` 是独立的 `@Configuration`，会自动被 Spring Boot 扫描。

**包结构**：

```
cartisan-data-jpa/
├── src/main/java/com/cartisan/data/jpa/
│   ├── config/
│   │   ├── CartisanDataJpaAutoConfiguration.java  (已有)
│   │   └── JpaAuditingConfiguration.java          (新增)
│   ├── domain/
│   │   ├── Auditable.java                         (新增)
│   │   └── SoftDeletable.java                     (新增)
│   └── repository/
│       ├── BaseRepository.java                    (已有)
│       └── impl/
│           └── BaseRepositoryImpl.java            (已有)
```

### 2.2 依赖关系

| 模块 | 关系 |
|------|------|
| `cartisan-data-jpa` | 提供 `Auditable`、`SoftDeletable` 基类 |
| `cartisan-security` (未来) | 实现 `AuditorAware<String>`，从 Sa-Token 获取当前用户 |
| `cartisan-event` | 现有，用于领域事件发布 |

---

## 3. 数据库变更描述

### 3.1 DDL 语句模板（PostgreSQL）

业务项目使用 `Auditable` 的实体表需包含审计字段：

```sql
-- 审计字段（Auditable）
ALTER TABLE your_table ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE your_table ADD COLUMN last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE your_table ADD COLUMN created_by VARCHAR(100);
ALTER TABLE your_table ADD COLUMN last_modified_by VARCHAR(100);

-- 软删除字段（SoftDeletable，在 Auditable 基础上）
ALTER TABLE your_table ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
```

### 3.2 Flyway 迁移示例（业务项目）

```sql
-- V1__create_order_table.sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL,

    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),

    -- 软删除字段
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
```

---

## 4. 核心流程（伪代码）

### 4.1 审计字段填充流程

```
1. 业务代码：repository.save(order)
   ↓
2. Spring Data JPA：在持久化前拦截
   ↓
3. JPA Auditing：
   - 检测到 @CreatedDate 注解
   - 若 createdAt 为 null，设置为 LocalDateTime.now()
   - 检测到 @LastModifiedDate 注解
   - 设置 lastModifiedDate = LocalDateTime.now()
   - 检测到 @CreatedBy 注解
   - 若存在 AuditorAware Bean，调用 auditorAware.getCurrentAuditor()
   - 将返回值设置到 createdBy
   - 检测到 @LastModifiedBy 注解
   - 同上，设置到 lastModifiedBy
   ↓
4. 持久化到数据库
```

### 4.2 软删除流程

```
1. 业务代码：repository.delete(product)
   ↓
2. BaseRepositoryImpl：
   - 不执行 DELETE SQL
   - 执行 UPDATE SET deleted = true WHERE id = ?
   ↓
3. 数据库：deleted 字段 = true
   ↓
4. 后续查询：
   - @SQLRestriction("deleted = false") 自动附加 WHERE 条件
   - 已删除记录被过滤
```

---

## 5. 备选方案与取舍

### 5.1 AuditorAware 获取方式

| 方案 | 描述 | 优点 | 缺点 | 选择 |
|------|------|------|------|------|
| **A. Spring Security** | 从 `SecurityContextHolder` 获取 | Spring Data JPA 原生支持 | 硬依赖 Spring Security，与 Sa-Token 冲突 | ❌ |
| **B. 自定义 Auditor** | 提供接口，由业务实现 | 灵活，不绑定具体安全框架 | 需要业务额外配置 | ✅ |
| **C. 默认占位值** | 返回 "system" 或 "anonymous" | 开箱即用 | 数据库大量无意义值 | ❌ |

**选择 B 的理由**：符合设计文档「从 SecurityContext 获取」的定位，不硬依赖 Spring Security，由 cartisan-security 统一实现。

---

### 5.2 JpaAuditingConfiguration 配置方式

| 方案 | 描述 | 优点 | 缺点 | 选择 |
|------|------|------|------|------|
| **A. 条件装配** | `@ConditionalOnBean(AuditorAware.class)` | 无 AuditorAware 时不启用，by 字段保持 null | 需要业务显式配置 | ✅ |
| **B. 无条件启用** | 总是启用，提供默认实现 | 总是有值 | 默认值无意义，误导 | ❌ |

**选择 A 的理由**：语义清晰，没有用户上下文时 by 字段为 null 比占位值更准确。

---

### 5.3 软删除实现方式

| 方案 | 描述 | 优点 | 缺点 | 选择 |
|------|------|------|------|------|
| **A. @SQLRestriction** | Hibernate 注解，自动过滤查询 | 简洁，Hibernate 原生支持 | 只影响 Hibernate 查询，@Query 需手动处理 | ✅ |
| **B. @Where** | JPA 标准注解 | JPA 标准 | 功能较弱，不如 @SQLRestriction | ❌ |
| **C. 自定义 Repository** | 覆盖所有查询方法 | 完全可控 | 代码量大，易遗漏 | ❌ |

**选择 A 的理由**：Spring Data JPA + Hibernate 生态的标准做法，简洁有效。

---

## 6. 错误处理

| 场景 | 处理方式 |
|------|---------|
| `AuditorAware.getCurrentAuditor()` 返回 `Optional.empty()` | `@CreatedBy`/`@LastModifiedBy` 保持 null，不抛异常 |
| 容器中不存在 `AuditorAware` Bean | 不启用 JPA Auditing（对人的审计），时间字段仍正常填充 |
| 保存已软删除实体 | 允许，`@SQLRestriction` 不阻止 UPDATE |
| 多次删除同一实体 | 允许，幂等，`deleted` 保持 `true` |

---

## 7. API/接口清单

| 接口/类 | 类型 | 职责 |
|---------|------|------|
| `Auditable` | `@MappedSuperclass` | 审计字段基类 |
| `SoftDeletable` | `@MappedSuperclass` | 软删除基类 |
| `JpaAuditingConfiguration` | `@Configuration` | JPA Auditing 自动配置 |

无 HTTP 接口（此 Feature 为基础设施，无 REST API）。

---

## 8. 参考资料

- Spring Data JPA Auditing：https://docs.spring.io/spring-data/jpa/reference/jpa/auditing.html
- Hibernate @SQLRestriction：https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#filters
- 设计文档：[cartisan-boot-设计文档.md](../../cartisan-boot-设计文档.md)
- 需求文档：[01_requirement.md](./01_requirement.md)
