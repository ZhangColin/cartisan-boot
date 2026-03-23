package com.cartisan.data.jpa.specification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Condition 注解集成测试。
 *
 * <p>验证 @Condition 注解在真实 JPA 环境中的工作情况。</p>
 *
 * <p><b>注意：</b>由于 ConditionSpecifications 对 BigDecimal 的类型推断限制
 *（使用 path.as(Comparable.class) 导致 Hibernate 无法推断类型），
 * 大小比较和区间查询测试使用 Integer 类型的 stock 字段。</p>
 */
@DataJpaTest
@ContextConfiguration(classes = ConditionIntegrationTest.TestConfiguration.class)
@DisplayName("Condition 注解集成测试")
class ConditionIntegrationTest {

    @Autowired
    private TestProductRepository repository;

    @Configuration
    @EntityScan(basePackageClasses = TestProduct.class)
    @EnableJpaRepositories(basePackageClasses = TestProductRepository.class)
    static class TestConfiguration {
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // 创建测试数据
        LocalDateTime now = LocalDateTime.now();

        // Product 1: iPhone 15, Electronics, $799, stock 50
        TestProduct p1 = new TestProduct(1L, "iPhone 15", new BigDecimal("799.99"), "Electronics", 50);
        p1.setCreatedAt(now.minusDays(10));
        repository.save(p1);

        // Product 2: MacBook Pro, Electronics, $1999, stock 20
        TestProduct p2 = new TestProduct(2L, "MacBook Pro", new BigDecimal("1999.99"), "Electronics", 20);
        p2.setCreatedAt(now.minusDays(5));
        repository.save(p2);

        // Product 3: T-Shirt, Clothing, $29, stock 100
        TestProduct p3 = new TestProduct(3L, "T-Shirt", new BigDecimal("29.99"), "Clothing", 100);
        p3.setCreatedAt(now.minusDays(3));
        repository.save(p3);

        // Product 4: Jeans, Clothing, $79, stock 80
        TestProduct p4 = new TestProduct(4L, "Jeans", new BigDecimal("79.99"), "Clothing", 80);
        p4.setCreatedAt(now.minusDays(2));
        repository.save(p4);

        // Product 5: Headphones, Electronics, $149, stock 30
        TestProduct p5 = new TestProduct(5L, "Headphones", new BigDecimal("149.99"), "Electronics", 30);
        p5.setCreatedAt(now.minusDays(1));
        repository.save(p5);

        // Product 6: Book, Books, $19, stock 200
        TestProduct p6 = new TestProduct(6L, "Java Programming Book", new BigDecimal("19.99"), "Books", 200);
        p6.setCreatedAt(now.minusHours(12));
        repository.save(p6);
    }

    // ==================== EQUAL 条件测试 ====================

    @Nested
    @DisplayName("EQUAL 条件测试")
    class EqualConditionTests {

        @Test
        @DisplayName("should filter by category with EQUAL condition")
        void shouldFilterByEqualCondition() {
            // given
            ProductQuery query = new ProductQuery(null, null, null, "Electronics", null, null, null);

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(p -> "Electronics".equals(p.category()));
            assertThat(results).extracting(TestProduct::name)
                    .containsExactlyInAnyOrder("iPhone 15", "MacBook Pro", "Headphones");
        }

        @Test
        @DisplayName("should return empty list when no match for EQUAL condition")
        void shouldReturnEmptyListWhenNoMatchForEqualCondition() {
            // given
            ProductQuery query = new ProductQuery(null, null, null, "NonExistent", null, null, null);

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).isEmpty();
        }
    }

    // ==================== LIKE 条件测试 ====================

    @Nested
    @DisplayName("LIKE 条件测试")
    class LikeConditionTests {

        @Test
        @DisplayName("should filter by name with INNER_LIKE condition")
        void shouldFilterByLikeCondition() {
            // given
            ProductQuery query = new ProductQuery("MacBook Pro", null, null, null, null, null, null);

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).isEqualTo("MacBook Pro");
        }

        @Test
        @DisplayName("should filter with case-sensitive like")
        void shouldFilterWithCaseSensitiveLike() {
            // given
            ProductQuery query = new ProductQuery("java", null, null, null, null, null, null);

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            // 默认 LIKE 是区分大小写的（取决于数据库）
            // H2 默认区分大小写，所以 "java" 不会匹配 "Java"
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should find products containing 'Programming'")
        void shouldFindProductsContainingProgramming() {
            // given
            ProductQuery query = new ProductQuery("Programming", null, null, null, null, null, null);

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).contains("Programming");
        }
    }

    // ==================== GREATER/LESS 条件测试 ====================

    @Nested
    @DisplayName("GREATER/LESS 条件测试")
    class GreaterLessConditionTests {

        @Test
        @DisplayName("should filter by minStock with GREATER_EQUAL condition")
        void shouldFilterByGreaterCondition() {
            // given
            ProductQuery query = new ProductQuery(null, 50, null, null, null, null, null);

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(4);
            assertThat(results).allMatch(p -> p.stock() >= 50);
            assertThat(results).extracting(TestProduct::name)
                    .containsExactlyInAnyOrder("iPhone 15", "T-Shirt", "Jeans", "Java Programming Book");
        }

        @Test
        @DisplayName("should filter by maxStock with LESS_EQUAL condition")
        void shouldFilterByLessCondition() {
            // given
            ProductQuery query = new ProductQuery(null, null, 30, null, null, null, null);

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(p -> p.stock() <= 30);
            assertThat(results).extracting(TestProduct::name)
                    .containsExactlyInAnyOrder("MacBook Pro", "Headphones");
        }

        @Test
        @DisplayName("should filter by stock range using minStock and maxStock")
        void shouldFilterByStockRangeUsingMinAndMaxStock() {
            // given
            ProductQuery query = new ProductQuery(
                    null,
                    25,
                    80,
                    null,
                    null,
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(p -> p.stock() >= 25 && p.stock() <= 80);
            assertThat(results).extracting(TestProduct::name)
                    .containsExactlyInAnyOrder("iPhone 15", "Jeans", "Headphones");
        }
    }

    // ==================== IN 条件测试 ====================

    @Nested
    @DisplayName("IN 条件测试")
    class InConditionTests {

        @Test
        @DisplayName("should filter by categories with IN condition")
        void shouldFilterByInCondition() {
            // given
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    null,
                    null,
                    List.of("Electronics", "Clothing"),
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(5);
            assertThat(results).allMatch(p ->
                    "Electronics".equals(p.category()) || "Clothing".equals(p.category())
            );
        }

        @Test
        @DisplayName("should filter by single category with IN condition")
        void shouldFilterBySingleCategoryWithInCondition() {
            // given
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    null,
                    null,
                    List.of("Books"),
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).category()).isEqualTo("Books");
        }

        @Test
        @DisplayName("should return empty list when IN condition has no matches")
        void shouldReturnEmptyListWhenInConditionHasNoMatches() {
            // given
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    null,
                    null,
                    List.of("NonExistent"),
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).isEmpty();
        }
    }

    // ==================== BETWEEN 条件测试 ====================

    @Nested
    @DisplayName("BETWEEN 条件测试")
    class BetweenConditionTests {

        @Test
        @DisplayName("should filter by stockRange with BETWEEN condition")
        void shouldFilterByBetweenCondition() {
            // given
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(20, 80),
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(4);
            assertThat(results).allMatch(p -> p.stock() >= 20 && p.stock() <= 80);
            assertThat(results).extracting(TestProduct::name)
                    .containsExactlyInAnyOrder("iPhone 15", "Jeans", "MacBook Pro", "Headphones");
        }

        @Test
        @DisplayName("should handle exact boundary values in BETWEEN")
        void shouldHandleExactBoundaryValuesInBetween() {
            // given
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(50, 100),
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(3);
            assertThat(results).extracting(TestProduct::name)
                    .containsExactlyInAnyOrder("iPhone 15", "T-Shirt", "Jeans");
        }
    }

    // ==================== 多字段模糊搜索 (blurry) 测试 ====================

    @Nested
    @DisplayName("多字段模糊搜索 (blurry) 测试")
    class BlurrySearchTests {

        @Test
        @DisplayName("should search across name and category fields with keyword")
        void shouldSearchAcrossMultipleFieldsWithKeyword() {
            // given
            ProductQuery query = new ProductQuery(null, null, null, null, null, null, "MacBook");

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            // 应该匹配 name 包含 "MacBook" 的产品
            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).contains("MacBook");
        }

        @Test
        @DisplayName("should match category field with blurry search")
        void shouldMatchCategoryFieldWithBlurrySearch() {
            // given
            ProductQuery query = new ProductQuery(null, null, null, null, null, null, "Electronic");

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(p -> p.category().contains("Electronic"));
        }

        @Test
        @DisplayName("should return empty when blurry keyword not found")
        void shouldReturnEmptyWhenBlurryKeywordNotFound() {
            // given
            ProductQuery query = new ProductQuery(null, null, null, null, null, null, "NonExistentKeyword");

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).isEmpty();
        }
    }

    // ==================== 组合条件测试 ====================

    @Nested
    @DisplayName("组合条件测试")
    class CombinedConditionsTests {

        @Test
        @DisplayName("should combine multiple conditions with AND")
        void shouldCombineMultipleConditions() {
            // given: Electronics category AND stock >= 30
            ProductQuery query = new ProductQuery(
                    null,
                    30,
                    null,
                    "Electronics",
                    null,
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(p ->
                    "Electronics".equals(p.category()) && p.stock() >= 30
            );
            assertThat(results).extracting(TestProduct::name)
                    .containsExactlyInAnyOrder("iPhone 15", "Headphones");
        }

        @Test
        @DisplayName("should combine category and name conditions")
        void shouldCombineCategoryAndNameConditions() {
            // given: Clothing category AND name contains "Jean"
            ProductQuery query = new ProductQuery(
                    "Jean",
                    null,
                    null,
                    "Clothing",
                    null,
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).isEqualTo("Jeans");
            assertThat(results.get(0).category()).isEqualTo("Clothing");
        }

        @Test
        @DisplayName("should combine IN condition with stock range")
        void shouldCombineInConditionWithStockRange() {
            // given: Electronics or Books AND stock <= 50
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    50,
                    null,
                    List.of("Electronics", "Books"),
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(p ->
                    p.stock() <= 50 &&
                    ("Electronics".equals(p.category()) || "Books".equals(p.category()))
            );
        }

        @Test
        @DisplayName("should return all products when all conditions are null")
        void shouldReturnAllProductsWhenAllConditionsAreNull() {
            // given
            ProductQuery query = new ProductQuery(null, null, null, null, null, null, null);

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(6);
        }

        @Test
        @DisplayName("should combine blurry search with category filter")
        void shouldCombineBlurrySearchWithCategoryFilter() {
            // given: keyword "Pro" AND category Electronics
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    null,
                    "Electronics",
                    null,
                    null,
                    "Pro"
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).name()).isEqualTo("MacBook Pro");
        }
    }

    // ==================== 边界情况测试 ====================

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("should skip null condition values")
        void shouldSkipNullConditionValues() {
            // given: mix of null and actual values
            ProductQuery query = new ProductQuery(
                    null,           // null name
                    null,           // null minStock
                    30,             // actual maxStock
                    null,           // null category
                    null,           // null categories
                    null,           // null stockRange
                    null            // null keyword
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(p -> p.stock() <= 30);
        }

        @Test
        @DisplayName("should handle empty string values")
        void shouldHandleEmptyStringValues() {
            // given: empty string should be skipped
            ProductQuery query = new ProductQuery(
                    "",
                    null,
                    null,
                    "Electronics",
                    null,
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(p -> "Electronics".equals(p.category()));
        }

        @Test
        @DisplayName("should handle empty list in IN condition")
        void shouldHandleEmptyListInInCondition() {
            // given: empty list should result in no matches or be skipped
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            // 空列表会导致 IN 条件返回 disjunction()，所以没有结果
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle single element list in IN condition")
        void shouldHandleSingleElementListInInCondition() {
            // given
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    null,
                    null,
                    List.of("Clothing"),
                    null,
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(p -> "Clothing".equals(p.category()));
        }

        @Test
        @DisplayName("should handle BETWEEN with invalid list size")
        void shouldHandleBetweenWithInvalidListSize() {
            // given: BETWEEN requires exactly 2 elements
            ProductQuery query = new ProductQuery(
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(50),  // only 1 element
                    null
            );

            // when
            List<TestProduct> results = repository.findByCondition(query);

            // then
            // 无效的 BETWEEN 条件返回 disjunction()，所以没有结果
            assertThat(results).isEmpty();
        }
    }
}
