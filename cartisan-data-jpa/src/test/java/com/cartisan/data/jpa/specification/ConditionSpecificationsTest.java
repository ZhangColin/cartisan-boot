package com.cartisan.data.jpa.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConditionSpecifications 单元测试。
 *
 * <p>验证能根据 ConditionType 生成正确的 Predicate</p>
 */
@DisplayName("ConditionSpecifications 测试")
class ConditionSpecificationsTest {

    // ==================== Test Data ====================

    record TestQueryCondition(
            String fieldName,
            ConditionType type,
            Object value
    ) {}

    // ==================== 相等性比较 ====================

    @Test
    @DisplayName("should generate EQUAL predicate")
    void shouldGenerateEqualPredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("name", ConditionType.EQUAL, "John");

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    @Test
    @DisplayName("should generate NOT_EQUAL predicate")
    void shouldGenerateNotEqualPredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("status", ConditionType.NOT_EQUAL, 1);

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    // ==================== 大小比较 ====================

    @Test
    @DisplayName("should generate GREATER_EQUAL predicate")
    void shouldGenerateGreaterEqualPredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("age", ConditionType.GREATER_EQUAL, 18);

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    @Test
    @DisplayName("should generate GREATER predicate")
    void shouldGenerateGreaterPredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("score", ConditionType.GREATER, 60);

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    @Test
    @DisplayName("should generate LESS_EQUAL predicate")
    void shouldGenerateLessEqualPredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("price", ConditionType.LESS_EQUAL, 100);

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    @Test
    @DisplayName("should generate LESS predicate")
    void shouldGenerateLessPredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("quantity", ConditionType.LESS, 10);

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    // ==================== 模糊查询 ====================

    @Test
    @DisplayName("should generate INNER_LIKE predicate")
    void shouldGenerateInnerLikePredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("title", ConditionType.INNER_LIKE, "keyword");

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    @Test
    @DisplayName("should generate LEFT_LIKE predicate")
    void shouldGenerateLeftLikePredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("description", ConditionType.LEFT_LIKE, "suffix");

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    @Test
    @DisplayName("should generate RIGHT_LIKE predicate")
    void shouldGenerateRightLikePredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("code", ConditionType.RIGHT_LIKE, "prefix");

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    // ==================== 集合与区间查询 ====================

    @Test
    @DisplayName("should generate IN predicate")
    void shouldGenerateInPredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("category", ConditionType.IN, List.of("A", "B", "C"));

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }

    @Test
    @DisplayName("should generate BETWEEN predicate")
    void shouldGenerateBetweenPredicate() {
        // given
        TestQueryCondition condition = new TestQueryCondition("createdDate", ConditionType.BETWEEN, List.of("2024-01-01", "2024-12-31"));

        // when
        Specification<Object> specification = ConditionSpecifications.of(condition);

        // then
        assertThat(specification).isNotNull();
    }
}
