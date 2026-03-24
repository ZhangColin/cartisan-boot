# cartisan-boot 框架简化设计文档

**日期**: 2026-03-24
**目标**: 简化配置类结构，优化命名，减少冗余

---

## 背景

cartisan-boot 框架在演进过程中积累了一些可优化的设计：
1. 配置类过于分散，部分配置类内容简单可以合并
2. `AbstractSoftDeletable` 命名不够直观
3. `PageQuery` 与 Spring 自带的 `Pageable` 功能重复

本文档记录这些简化决策，确保框架保持简洁、易用。

---

## 变更清单

### 1. JPA 配置合并

**变更**：将 `JpaAuditingConfiguration` 合并到 `CartisanDataJpaAutoConfiguration`

**原因**：
- `JpaAuditingConfiguration` 只有一个注解 `@EnableJpaAuditing`，内容空
- 业务系统肯定启用审计，不需要条件装配

**修改**：
```java
// 修改前
@AutoConfiguration
@Import(JpaAuditingConfiguration.class)
public class CartisanDataJpaAutoConfiguration { ... }

@Configuration
@ConditionalOnBean(AuditorAware.class)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfiguration { }

// 修改后
@AutoConfiguration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class CartisanDataJpaAutoConfiguration { ... }
```

**影响文件**：
- `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java`
- 删除 `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/JpaAuditingConfiguration.java`

---

### 2. 软删除命名重构

**变更**：`AbstractSoftDeletable` → `AuditableSoftDeletable`

**原因**：
- `AbstractXxx` 命名风格模糊，不直观
- 新名明确表达"可审计 + 可软删除"的语义

**保持不变**：
- `Auditable` 基类 — 供"只审计不软删除"的实体使用
- `SoftDeletable` 接口 — Repository 用于判断软删除能力

**继承关系**：
```
Auditable (审计基类)
    ↑
AuditableSoftDeletable (审计 + 软删除基类)
```

**影响文件**：
- `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/AbstractSoftDeletable.java`
  → 重命名为 `AuditableSoftDeletable.java`

---

### 3. 删除 PageQuery

**变更**：删除 `PageQuery` 类，使用 Spring 自带的 `Pageable` / `PageRequest`

**原因**：
- `org.springframework.data.domain.Pageable` / `PageRequest` 功能完备
- 避免重复造轮子

**影响文件**：
- 删除 `cartisan-data-query/src/main/java/com/cartisan/data/query/page/PageQuery.java`
- 删除 `cartisan-data-query/src/main/java/com/cartisan/data/query/page/package-info.java`
- 删除 `cartisan-data-query/src/test/java/com/cartisan/data/query/page/PageQueryTest.java`

---

### 4. Security 配置合并

**变更**：将 `SecurityInterceptorConfig` 和 `CurrentUserArgumentResolverConfig` 合并到 `CartisanSecurityAutoConfiguration`

**原因**：
- 两个配置类内容简单，主要是一个 Bean 方法 + `WebMvcConfigurer` 实现
- 减少配置类数量，降低认知负担

**修改**：
```java
// 修改前
@AutoConfiguration
@Import({
    SecurityInterceptorConfig.class,
    CurrentUserArgumentResolverConfig.class
})
public class CartisanSecurityAutoConfiguration { ... }

// 修改后
@AutoConfiguration
public class CartisanSecurityAutoConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean
    public SecurityInterceptor securityInterceptor() { ... }

    @Override
    public void addInterceptors(InterceptorRegistry registry) { ... }

    @Bean
    @ConditionalOnMissingBean
    public CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver() { ... }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) { ... }
}
```

**影响文件**：
- `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`
- 删除 `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptorConfig.java`
- 删除 `cartisan-security/src/main/java/com/cartisan/security/config/CurrentUserArgumentResolverConfig.java`

---

## 保持不变的部分

| 组件 | 原因 |
|------|------|
| `DomainEvent.aggregateId` | `String` 类型支持多种 ID 类型，保持灵活性 |
| `Auditable` 基类 | 支持"只审计不软删除"的实体场景 |
| `SoftDeletable` 接口 | Repository 需要此接口判断软删除能力 |
| `BaseRepository` / `BaseRepositoryImpl` | 设计正确，通过 Spring Data JPA 机制关联 |
| `AutoResponseConfiguration` | 配置开关有实际使用价值 |

---

## 影响评估

### 破坏性变更

1. **AuditableSoftDeletable 重命名**：业务代码如果直接引用 `AbstractSoftDeletable` 需要修改
   - 框架内部：修改 `BaseRepositoryImpl` 中的引用
   - 业务代码：通常只继承类，不直接引用类名

2. **PageQuery 删除**：业务代码如果使用了 `PageQuery` 需要改用 `PageRequest`
   - 需要检查 `cartisan-data-query` 的使用方

### 非破坏性变更

1. **配置类合并**：内部重构，不影响业务代码
2. **Security 配置合并**：内部重构，不影响业务代码

---

## 实施顺序

1. **JPA 配置合并** — 无破坏性，优先
2. **Security 配置合并** — 无破坏性
3. **AuditableSoftDeletable 重命名** — 破坏性，需要同步修改引用
4. **PageQuery 删除** — 破坏性，需要确认无外部使用

---

## 验收标准

- [ ] 所有测试通过
- [ ] 文档更新（类引用、示例代码）
- [ ] 变更日志记录
