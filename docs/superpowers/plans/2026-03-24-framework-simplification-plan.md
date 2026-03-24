# Framework Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simplify cartisan-boot framework by merging configuration classes, renaming for clarity, and removing redundant code

**Architecture:** Merge scattered configuration classes into main auto-configuration classes, rename AbstractSoftDeletable to AuditableSoftDeletable for clarity, remove PageQuery in favor of Spring's Pageable

**Tech Stack:** Java 21, Spring Boot 3.4.x, Gradle Kotlin DSL

---

## File Structure

```
cartisan-data-jpa/
  src/main/java/com/cartisan/data/jpa/
    config/
      CartisanDataJpaAutoConfiguration.java  (modify)
      JpaAuditingConfiguration.java          (DELETE)
    domain/
      AbstractSoftDeletable.java             (rename to AuditableSoftDeletable.java)
      SoftDeletable.java                     (modify JavaDoc)
    repository/impl/
      BaseRepositoryImpl.java                (modify references)

cartisan-data-query/
  src/main/java/com/cartisan/data/query/page/
    PageQuery.java                           (DELETE)
    package-info.java                        (DELETE)

cartisan-security/
  src/main/java/com/cartisan/security/config/
    CartisanSecurityAutoConfiguration.java   (modify)
    SecurityInterceptorConfig.java           (DELETE)
    CurrentUserArgumentResolverConfig.java   (DELETE)

docs/
  guide/cartisan-boot-使用手册.md              (modify)
  cartisan-boot-设计文档.md                   (modify)
  superpowers/specs/2026-03-24-test-coverage-enhancement-design.md (modify)
```

---

## Task 1: JPA Configuration Merge

**Files:**
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java`
- Delete: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/JpaAuditingConfiguration.java`

- [ ] **Step 1: Read current CartisanDataJpaAutoConfiguration**

Run: `cat cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/CartisanDataJpaAutoConfiguration.java`

- [ ] **Step 2: Add @EnableJpaAuditing and default auditorAware Bean**

Modify `CartisanDataJpaAutoConfiguration.java`:
- Remove `@Import(JpaAuditingConfiguration.class)`
- Add `@EnableJpaAuditing(auditorAwareRef = "auditorAware")`
- Add default `auditorAware()` Bean method with `@ConditionalOnMissingBean`

```java
@AutoConfiguration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class CartisanDataJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.empty();
    }

    // ... existing beans
}
```

- [ ] **Step 3: Delete JpaAuditingConfiguration.java**

Run: `rm cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/JpaAuditingConfiguration.java`

- [ ] **Step 4: Run JPA module tests**

Run: `./gradlew :cartisan-data-jpa:test --tests "*Config*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/config/
git commit -m "refactor(jpa): merge JpaAuditingConfiguration into CartisanDataJpaAutoConfiguration

- Add @EnableJpaAuditing directly to main auto-config
- Add default AuditorAware<String> bean to avoid startup failure
- Delete JpaAuditingConfiguration.java"
```

---

## Task 2: Rename AbstractSoftDeletable to AuditableSoftDeletable

**Files:**
- Rename: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/AbstractSoftDeletable.java` → `AuditableSoftDeletable.java`
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/SoftDeletable.java` (JavaDoc)
- Modify: `cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/repository/impl/BaseRepositoryImpl.java`
- Modify: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/SoftDeletableTest.java`
- Modify: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/TestSoftDeletableEntity.java`
- Modify: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AuditableTest.java`
- Modify: `cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/softdelete/*.java`

- [ ] **Step 1: Rename the file**

Run: `git mv cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/AbstractSoftDeletable.java cartisan-data-jpa/src/main/java/com/cartisan/data/jpa/domain/AuditableSoftDeletable.java`

- [ ] **Step 2: Update class name in AuditableSoftDeletable.java**

Update class declaration and JavaDoc:

```java
/**
 * 可审计且可软删除实体基类。
 *
 * <p>继承 {@link Auditable}，增加软删除能力：</p>
 * ...
 * @since 0.3.0
 */
@MappedSuperclass
@SQLRestriction("deleted = false")
public abstract class AuditableSoftDeletable extends Auditable implements SoftDeletable {
    // ... existing content
}
```

- [ ] **Step 3: Update SoftDeletable.java JavaDoc**

Update references to `AbstractSoftDeletable` → `AuditableSoftDeletable`:

```java
/**
 * 可软删除实体接口。
 *
 * <p>此接口与 {@link AuditableSoftDeletable} 抽象类配合使用：</p>
 * <ul>
 *   <li>实体可以直接继承 {@link AuditableSoftDeletable} 获得完整实现</li>
 *   <li>或者继承其他基类（如 {@link com.cartisan.core.domain.AbstractAggregateRoot}）并实现此接口</li>
 * </ul>
 */
```

- [ ] **Step 4: Update BaseRepositoryImpl.java**

Find and replace `AbstractSoftDeletable` → `AuditableSoftDeletable`:
- Type references in instanceof checks
- JavaDoc comments

- [ ] **Step 5: Update SoftDeletableTest.java**

Run: `sed -i '' 's/AbstractSoftDeletable/AuditableSoftDeletable/g' cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/SoftDeletableTest.java`

- [ ] **Step 6: Update TestSoftDeletableEntity.java**

Run: `sed -i '' 's/AbstractSoftDeletable/AuditableSoftDeletable/g' cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/TestSoftDeletableEntity.java`

- [ ] **Step 7: Update AuditableTest.java**

Run: `sed -i '' 's/AbstractSoftDeletable/AuditableSoftDeletable/g' cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/domain/AuditableTest.java`

- [ ] **Step 8: Update softdelete test directory**

Run: `sed -i '' 's/AbstractSoftDeletable/AuditableSoftDeletable/g' cartisan-data-jpa/src/test/java/com/cartisan/data/jpa/repository/impl/softdelete/*.java`

- [ ] **Step 9: Verify no remaining references**

Run: `grep -r "AbstractSoftDeletable" cartisan-data-jpa/src/ --include="*.java"`
Expected: No results

- [ ] **Step 10: Run all JPA module tests**

Run: `./gradlew :cartisan-data-jpa:test`
Expected: PASS

- [ ] **Step 11: Commit**

```bash
git add cartisan-data-jpa/src/
git commit -m "refactor(jpa): rename AbstractSoftDeletable to AuditableSoftDeletable

- More descriptive name combining audit + soft-delete capabilities
- Update all references in domain, repository, and tests"
```

---

## Task 3: Delete PageQuery

**Files:**
- Delete: `cartisan-data-query/src/main/java/com/cartisan/data/query/page/PageQuery.java`
- Delete: `cartisan-data-query/src/main/java/com/cartisan/data/query/page/package-info.java`
- Delete: `cartisan-data-query/src/test/java/com/cartisan/data/query/page/PageQueryTest.java`

- [ ] **Step 1: Verify no external usage**

Run: `grep -r "PageQuery" cartisan-data-query/src/ --include="*.java" | grep -v "PageQuery.java"`
Expected: Only PageQuery.java itself and its test

- [ ] **Step 2: Delete PageQuery.java**

Run: `rm cartisan-data-query/src/main/java/com/cartisan/data/query/page/PageQuery.java`

- [ ] **Step 3: Delete package-info.java**

Run: `rm cartisan-data-query/src/main/java/com/cartisan/data/query/page/package-info.java`

- [ ] **Step 4: Delete PageQueryTest.java**

Run: `rm cartisan-data-query/src/test/java/com/cartisan/data/query/page/PageQueryTest.java`

- [ ] **Step 5: Verify no remaining references in codebase**

Run: `grep -r "PageQuery" . --include="*.java" --exclude-dir=".git"`
Expected: Only in docs/

- [ ] **Step 6: Run data-query module tests**

Run: `./gradlew :cartisan-data-query:test`
Expected: PASS (or skip if no other tests)

- [ ] **Step 7: Commit**

```bash
git add cartisan-data-query/
git commit -m "refactor(query): remove PageQuery in favor of Spring Pageable

Users should use org.springframework.data.domain.Pageable/PageRequest instead.
PageQuery provided minimal value over Spring's built-in solution."
```

---

## Task 4: Security Configuration Merge

**Files:**
- Modify: `cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`
- Delete: `cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptorConfig.java`
- Delete: `cartisan-security/src/main/java/com/cartisan/security/config/CurrentUserArgumentResolverConfig.java`
- Modify: `cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityAutoConfigurationTest.java`
- Delete: `cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorConfigTest.java` (if exists)
- Delete: `cartisan-security/src/test/java/com/cartisan/security/config/CurrentUserArgumentResolverConfigTest.java` (if exists)

- [ ] **Step 1: Read all three config files**

Run: `cat cartisan-security/src/main/java/com/cartisan/security/config/CartisanSecurityAutoConfiguration.java`
Run: `cat cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptorConfig.java`
Run: `cat cartisan-security/src/main/java/com/cartisan/security/config/CurrentUserArgumentResolverConfig.java`

- [ ] **Step 2: Check existing test file**

Run: `cat cartisan-security/src/test/java/com/cartisan/security/config/CartisanSecurityAutoConfigurationTest.java`

Note: Check if it imports the config classes to be deleted.

- [ ] **Step 3: Modify CartisanSecurityAutoConfiguration to implement WebMvcConfigurer**

Add `implements WebMvcConfigurer` to class declaration.

- [ ] **Step 4: Add CartisanSecurityProperties field**

Since the current class uses method injection for properties, add a field:

```java
private final CartisanSecurityProperties properties;

public CartisanSecurityAutoConfiguration(CartisanSecurityProperties properties) {
    this.properties = properties;
}
```

- [ ] **Step 5: Add ObjectProvider fields**

Add to constructor parameters:

```java
private final ObjectProvider<SecurityInterceptor> interceptorProvider;
private final ObjectProvider<CurrentUserMethodArgumentResolver> resolverProvider;

public CartisanSecurityAutoConfiguration(
        ObjectProvider<SecurityInterceptor> interceptorProvider,
        ObjectProvider<CurrentUserMethodArgumentResolver> resolverProvider,
        CartisanSecurityProperties properties) {
    this.interceptorProvider = interceptorProvider;
    this.resolverProvider = resolverProvider;
    this.properties = properties;
}
```

- [ ] **Step 6: Add securityInterceptor() Bean**

```java
@Bean
@ConditionalOnMissingBean
public SecurityInterceptor securityInterceptor() {
    return new SecurityInterceptor();
}
```

- [ ] **Step 7: Add addInterceptors() method**

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    List<String> pathPatterns = properties.getPathPatterns();
    List<String> excludePathPatterns = properties.getExcludePathPatterns();

    registry.addInterceptor(interceptorProvider.getObject())
        .addPathPatterns(pathPatterns.toArray(new String[0]))
        .excludePathPatterns(excludePathPatterns.toArray(new String[0]));
}
```

- [ ] **Step 8: Add currentUserMethodArgumentResolver() Bean**

```java
@Bean
@ConditionalOnMissingBean
public CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver() {
    return new CurrentUserMethodArgumentResolver();
}
```

- [ ] **Step 9: Add addArgumentResolvers() method**

```java
@Override
public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(resolverProvider.getObject());
}
```

- [ ] **Step 10: Remove @Import annotation**

Remove `@Import({ SecurityInterceptorConfig.class, CurrentUserArgumentResolverConfig.class })`

- [ ] **Step 11: Update existing Bean methods to use field instead of parameter**

Find Bean methods that take `CartisanSecurityProperties` as parameter and change to use `this.properties`.

- [ ] **Step 12: Update CartisanSecurityAutoConfigurationTest.java**

Remove deleted config classes from `@SpringBootTest` annotation.
Remove test methods that verify deleted config beans.

- [ ] **Step 13: Delete SecurityInterceptorConfig.java**

Run: `rm cartisan-security/src/main/java/com/cartisan/security/config/SecurityInterceptorConfig.java`

- [ ] **Step 14: Delete CurrentUserArgumentResolverConfig.java**

Run: `rm cartisan-security/src/main/java/com/cartisan/security/config/CurrentUserArgumentResolverConfig.java`

- [ ] **Step 15: Delete config test files if they exist**

Run: `rm -f cartisan-security/src/test/java/com/cartisan/security/config/SecurityInterceptorConfigTest.java`
Run: `rm -f cartisan-security/src/test/java/com/cartisan/security/config/CurrentUserArgumentResolverConfigTest.java`

- [ ] **Step 16: Run security module tests**

Run: `./gradlew :cartisan-security:test`
Expected: PASS

- [ ] **Step 17: Commit**

```bash
git add cartisan-security/src/
git commit -m "refactor(security): merge config classes into CartisanSecurityAutoConfiguration

- Merge SecurityInterceptorConfig and CurrentUserArgumentResolverConfig
- Implement WebMvcConfigurer directly
- Add constructor injection for dependencies
- Update tests to remove deleted config classes"
```

---

## Task 5: Update Documentation

**Files:**
- Modify: `docs/guide/cartisan-boot-使用手册.md`
- Modify: `docs/cartisan-boot-设计文档.md`
- Modify: `docs/superpowers/specs/2026-03-24-test-coverage-enhancement-design.md`
- Check: `README.md`

- [ ] **Step 1: Update cartisan-boot-使用手册.md**

Find and replace:
- `AbstractSoftDeletable` → `AuditableSoftDeletable`
- Remove PageQuery sections (能力清单, API 说明, 使用示例, QUERY-003 规则)

- [ ] **Step 2: Update cartisan-boot-设计文档.md**

Find and replace:
- `AbstractSoftDeletable` → `AuditableSoftDeletable`
- Remove PageQuery 说明

- [ ] **Step 3: Update test-coverage-enhancement-design.md**

Remove PageQuery coverage target reference.

- [ ] **Step 4: Check and update README.md**

Run: `grep -n "AbstractSoftDeletable\|PageQuery" README.md`
If found, update references.

- [ ] **Step 5: Commit docs**

```bash
git add docs/
git commit -m "docs: update for framework simplification

- AbstractSoftDeletable → AuditableSoftDeletable
- Remove PageQuery references"
```

---

## Task 6: Final Verification

- [ ] **Step 1: Run full test suite**

Run: `./gradlew test`
Expected: ALL PASS

- [ ] **Step 2: Verify no orphaned references**

Run: `grep -r "AbstractSoftDeletable\|PageQuery" . --include="*.java" --exclude-dir=".git" | grep -v "Binary"`
Expected: Only in git history or comments explaining the change

- [ ] **Step 3: Final commit**

```bash
git commit --allow-empty -m "chore: framework simplification complete

All changes implemented:
- JPA config merged
- AuditableSoftDeletable renamed
- PageQuery removed
- Security config merged
- Documentation updated"
```

---

## Notes

- Each task commits independently for easy rollback
- Tests should pass after each task before proceeding
- Use `git status` frequently to verify staged changes
