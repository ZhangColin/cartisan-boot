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

**变更**：将 `JpaAuditingConfiguration` 合并到 `CartisanDataJpaAutoConfiguration`，提供默认 `AuditorAware` Bean

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
public class CartisanDataJpaAutoConfiguration {

    /**
     * 提供默认 AuditorAware Bean，避免业务系统未配置时启动失败。
     * 业务系统可以通过声明自己的 AuditorAware<String> Bean 来覆盖。
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.empty();  // createdBy/updatedBy 为 null
    }
}
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
- `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/SoftDeletable.java`
  → 更新 JavaDoc 中对 `AbstractSoftDeletable` 的引用
- `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java`
  → 更新对 `AbstractSoftDeletable` 的引用（类型检查）
- `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/SoftDeletableTest.java`
  → 更新测试代码中的引用
- `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/TestSoftDeletableEntity.java`
  → 更新继承关系
- `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AuditableTest.java`
  → 检查是否有相关引用
- `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/softdelete/*.java`
  → 更新软删除测试中的引用

---

### 3. 删除 PageQuery

**变更**：删除 `PageQuery` 类，使用 Spring 自带的 `Pageable` / `PageRequest`

**原因**：
- `org.springframework.data.domain.Pageable` / `PageRequest` 功能完备
- 避免重复造轮子

**迁移指南**：

`PageQuery` 有参数校验功能（page < 1 修正为 1，size > 100 修正为 100），`PageRequest` 没有这些校验。

业务代码迁移方式：

```java
// 修改前
PageQuery query = new PageQuery(page, size);
repository.findAll(PageRequest.of(query.page(), query.size()));

// 修改后：在 Controller 层做校验
@GetMapping("/list")
public Result list(@RequestParam(defaultValue = "1") int page,
                   @RequestParam(defaultValue = "20") int size) {
    page = Math.max(1, page);
    size = Math.max(1, Math.min(100, size));
    // ...
}

// 或直接使用（业务层自行处理边界情况）
PageRequest.of(page, size)
```

**影响文件**：
- 删除 `cartisan-data-query/src/main/java/com/cartisan/data/query/page/PageQuery.java`
- 删除 `cartisan-data-query/src/main/java/com/cartisan/data/query/page/package-info.java`
- 删除 `cartisan-data-query/src/test/java/com/cartisan/data/query/page/PageQueryTest.java`

**文档更新**：
- `docs/guide/cartisan-boot-使用手册.md` — 删除 PageQuery 相关说明
- `docs/cartisan-boot-设计文档.md` — 删除 PageQuery 说明
- `docs/superpowers/specs/2026-03-24-test-coverage-enhancement-design.md` — 删除 PageQuery 覆盖率目标

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
public class CartisanSecurityAutoConfiguration {
    // 已有 saTokenContextFilter, securityExceptionHandler 等其他 Bean
}

// 修改后
@AutoConfiguration
public class CartisanSecurityAutoConfiguration implements WebMvcConfigurer {

    // 保留原有的 ObjectProvider 模式，避免循环依赖
    private final ObjectProvider<SecurityInterceptor> interceptorProvider;
    private final ObjectProvider<CurrentUserMethodArgumentResolver> resolverProvider;
    private final CartisanSecurityProperties properties;

    public CartisanSecurityAutoConfiguration(
            ObjectProvider<SecurityInterceptor> interceptorProvider,
            ObjectProvider<CurrentUserMethodArgumentResolver> resolverProvider,
            CartisanSecurityProperties properties) {
        this.interceptorProvider = interceptorProvider;
        this.resolverProvider = resolverProvider;
        this.properties = properties;
    }

    // 原 SecurityInterceptorConfig 的内容
    @Bean
    @ConditionalOnMissingBean
    public SecurityInterceptor securityInterceptor() {
        return new SecurityInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> pathPatterns = properties.getPathPatterns();
        List<String> excludePathPatterns = properties.getExcludePathPatterns();

        registry.addInterceptor(interceptorProvider.getObject())
            .addPathPatterns(pathPatterns.toArray(new String[0]))
            .excludePathPatterns(excludePathPatterns.toArray(new String[0]));
    }

    // 原 CurrentUserArgumentResolverConfig 的内容
    @Bean
    @ConditionalOnMissingBean
    public CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver() {
        return new CurrentUserMethodArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolverProvider.getObject());
    }

    // 保留其他现有 Bean...
}
```

**影响文件**：
- `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`
- 删除 `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptorConfig.java`
- 删除 `cartisan-security/src/main/java/com/cartisan/security/config/CurrentUserArgumentResolverConfig.java`
- `cartisan-security/src/test/java/com/cartisan/security/config/*Test.java`
  → 可能需要调整测试中的配置引用

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
   - **过渡方案**：保留 `AbstractSoftDeletable` 作为 `@Deprecated` 别名，指向 `AuditableSoftDeletable`

2. **PageQuery 删除**：业务代码如果使用了 `PageQuery` 需要改用 `PageRequest`
   - 需要检查 `cartisan-data-query` 的使用方
   - **过渡方案**：先标记 `@Deprecated`，在下个大版本再删除

### 非破坏性变更

1. **JPA 配置合并**：内部重构，不影响业务代码
2. **Security 配置合并**：内部重构，不影响业务代码

---

## 实施顺序

按风险从低到高：

1. **JPA 配置合并** — 无破坏性，提供默认 Bean 确保兼容性
2. **AuditableSoftDeletable 重命名** — 破坏性，但影响范围小且明确（主要是测试代码）
3. **Security 配置合并** — 非破坏性，但需要验证合并后的配置正确性
4. **PageQuery 删除** — 破坏性，需要文档更新，建议最后处理

---

## 回滚计划

对于破坏性变更，如果出现问题：

1. **AuditableSoftDeletable**：保留 `AbstractSoftDeletable` 作为 `@Deprecated` 别名
   ```java
   @Deprecated
   @SuppressWarnings("removal")
   public abstract class AbstractSoftDeletable extends AuditableSoftDeletable {
       // 空类，仅作为别名
   }
   ```

2. **PageQuery**：先标记 `@Deprecated`，观察一段时间后再删除

---

## 验收标准

- [ ] 所有测试通过（包括单元测试和集成测试）
- [ ] 文档更新
  - [ ] `docs/guide/cartisan-boot-使用手册.md` — 删除 PageQuery 相关，更新 AbstractSoftDeletable → AuditableSoftDeletable
  - [ ] `docs/cartisan-boot-设计文档.md` — 同上
  - [ ] `docs/superpowers/specs/2026-03-24-test-coverage-enhancement-design.md` — 删除 PageQuery 覆盖率目标
  - [ ] `README.md` — 检查是否有相关引用
- [ ] 变更日志记录
- [ ] 破坏性变更的过渡方案实施（@Deprecated 别名）
