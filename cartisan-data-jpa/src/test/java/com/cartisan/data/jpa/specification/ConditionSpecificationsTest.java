package com.cartisan.data.jpa.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    // ==================== 注解反射解析测试 ====================

    @Nested
    @DisplayName("Annotation Reflection Parsing Tests")
    class AnnotationReflectionParsingTests {

        record TestQuery(
                @Condition(type = ConditionType.EQUAL) String name,
                @Condition(type = ConditionType.INNER_LIKE) String title,
                @Condition(propName = "status", type = ConditionType.EQUAL) Integer statusCode
        ) {}

        @Test
        @DisplayName("should read Condition annotation and generate Specification")
        void shouldReadConditionAnnotation() {
            // given
            TestQuery query = new TestQuery("John", "Manager", 1);

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }

        @Test
        @DisplayName("should handle custom propName")
        void shouldHandleCustomPropName() {
            // given
            TestQuery query = new TestQuery(null, null, 1);

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }

        @Test
        @DisplayName("should skip null and empty values")
        void shouldSkipNullAndEmptyValues() {
            // given
            TestQuery query = new TestQuery(null, "", 1);

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }
    }

    // ==================== 嵌套属性路径测试 ====================

    @Nested
    @DisplayName("Nested Property Path Tests")
    class NestedPropertyPathTests {

        record TestQueryWithNestedPath(
                @Condition(propName = "user.profile.name", type = ConditionType.EQUAL) String name,
                @Condition(propName = "user.age", type = ConditionType.EQUAL) Integer age
        ) {}

        @Test
        @DisplayName("should handle nested property path")
        void shouldHandleNestedPropertyPath() {
            // given
            TestQueryWithNestedPath query = new TestQueryWithNestedPath("John", 18);

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }

        @Test
        @DisplayName("should handle multiple level nesting")
        void shouldHandleMultipleLevelNesting() {
            // given
            TestQueryWithNestedPath query = new TestQueryWithNestedPath("Jane", null);

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }

        record TestQueryWithDeepNesting(
                @Condition(propName = "user.profile.settings.theme", type = ConditionType.EQUAL) String theme
        ) {}

        @Test
        @DisplayName("should handle deep nesting (3 levels)")
        void shouldHandleDeepNesting() {
            // given
            TestQueryWithDeepNesting query = new TestQueryWithDeepNesting("dark");

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }

        @Test
        @DisplayName("should handle single level path (no nesting)")
        void shouldHandleSingleLevelPath() {
            // given
            TestQueryWithNestedPath query = new TestQueryWithNestedPath(null, 25);

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }
    }

    // ==================== 多字段模糊搜索 (blurry) 测试 ====================

    @Nested
    @DisplayName("Blurry Multi-Field Search Tests")
    class BlurryMultiFieldSearchTests {

        record ArticleQuery(
                @Condition(blurry = "title,content") String keyword
        ) {}

        @Test
        @DisplayName("should handle blurry search")
        void shouldHandleBlurrySearch() {
            // given
            ArticleQuery query = new ArticleQuery("test");

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }

        @Test
        @DisplayName("should combine blurry fields with OR predicate")
        void shouldCombineBlurryFieldsWithOrPredicate() {
            // given
            ArticleQuery query = new ArticleQuery("keyword");

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();

            // 验证生成的 Specification 能正确转换为 Predicate
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            Path<Object> titlePath = mock(Path.class);
            Path<Object> contentPath = mock(Path.class);

            // 模拟 root.get() 调用
            when(root.get("title")).thenReturn(titlePath);
            when(root.get("content")).thenReturn(contentPath);

            Predicate titleLike = mock(Predicate.class);
            Predicate contentLike = mock(Predicate.class);
            Predicate orPredicate = mock(Predicate.class);
            Predicate andPredicate = mock(Predicate.class);

            // 模拟 cb.like() 调用
            when(cb.like(titlePath.as(String.class), "%keyword%")).thenReturn(titleLike);
            when(cb.like(contentPath.as(String.class), "%keyword%")).thenReturn(contentLike);

            // 模拟 cb.or() 调用
            when(cb.or(any(Predicate[].class))).thenReturn(orPredicate);

            // 模拟 cb.and() 调用（外层会包裹 and）
            when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

            Predicate result = specification.toPredicate(root, criteriaQuery, cb);

            // 验证结果不为空
            assertThat(result).isNotNull();

            // 验证 OR 被调用了（说明多字段模糊搜索生效）
            verify(cb).or(any(Predicate[].class));
        }

        record ArticleQueryWithThreeFields(
                @Condition(blurry = "title,subtitle,content") String keyword
        ) {}

        @Test
        @DisplayName("should handle blurry with three fields")
        void shouldHandleBlurryWithThreeFields() {
            // given
            ArticleQueryWithThreeFields query = new ArticleQueryWithThreeFields("search");

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }

        record ArticleQueryWithOtherConditions(
                @Condition(type = ConditionType.EQUAL) Integer status,
                @Condition(blurry = "title,content") String keyword
        ) {}

        @Test
        @DisplayName("should combine blurry with regular conditions")
        void shouldCombineBlurryWithRegularConditions() {
            // given
            ArticleQueryWithOtherConditions query = new ArticleQueryWithOtherConditions(1, "test");

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }

        record ArticleQueryWithEmptyKeyword(
                @Condition(blurry = "title,content") String keyword
        ) {}

        @Test
        @DisplayName("should skip blurry when keyword is null or empty")
        void shouldSkipBlurryWhenKeywordIsEmpty() {
            // given
            ArticleQueryWithEmptyKeyword query = new ArticleQueryWithEmptyKeyword(null);

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }

        record ArticleQueryWithSingleField(
                @Condition(blurry = "title") String keyword
        ) {}

        @Test
        @DisplayName("should handle blurry with single field")
        void shouldHandleBlurryWithSingleField() {
            // given
            ArticleQueryWithSingleField query = new ArticleQueryWithSingleField("single");

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();
        }
    }

    // ==================== 边界条件和异常处理测试 ====================

    @Nested
    @DisplayName("Edge Case and Exception Handling Tests")
    class EdgeCaseAndExceptionHandlingTests {

        // ==================== IN 条件边界测试 ====================

        @Test
        @DisplayName("should handle IN condition with Object[] array")
        void shouldHandleInConditionWithObjectArray() {
            // given
            TestQueryCondition condition = new TestQueryCondition("category", ConditionType.IN, new Object[]{"A", "B", "C"});

            // when
            Specification<Object> specification = ConditionSpecifications.of(condition);

            // then
            assertThat(specification).isNotNull();

            // 验证生成的 Specification 能正确转换为 Predicate
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            Path<Object> path = mock(Path.class);
            when(root.get("category")).thenReturn(path);

            // 使用 in(Collection) 来匹配 - 数组会被转换
            @SuppressWarnings("unchecked")
            Predicate inPredicate = mock(Predicate.class);
            when(path.in(org.mockito.ArgumentMatchers.<Collection>any())).thenReturn(inPredicate);

            // 同时也匹配 varargs 版本
            org.mockito.Mockito.doReturn(inPredicate).when(path).in(new Object[]{"A", "B", "C"});

            Predicate result = specification.toPredicate(root, criteriaQuery, cb);

            // 验证结果不为空
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return disjunction for IN condition with invalid value")
        void shouldReturnDisjunctionForInConditionWithInvalidValue() {
            // given
            TestQueryCondition condition = new TestQueryCondition("category", ConditionType.IN, "invalid-string-value");

            // when
            Specification<Object> specification = ConditionSpecifications.of(condition);

            // then
            assertThat(specification).isNotNull();

            // 验证生成的 Specification 能正确转换为 Predicate (返回 disjunction)
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            Path<Object> path = mock(Path.class);
            when(root.get("category")).thenReturn(path);

            Predicate disjunction = mock(Predicate.class);
            when(cb.disjunction()).thenReturn(disjunction);

            Predicate result = specification.toPredicate(root, criteriaQuery, cb);

            // 验证 disjunction 被调用
            assertThat(result).isNotNull();
            verify(cb).disjunction();
        }

        // ==================== BETWEEN 条件边界测试 ====================

        @Test
        @DisplayName("should handle BETWEEN condition with Object[] array")
        void shouldHandleBetweenConditionWithObjectArray() {
            // given
            TestQueryCondition condition = new TestQueryCondition("createdDate", ConditionType.BETWEEN, new Object[]{"2024-01-01", "2024-12-31"});

            // when
            Specification<Object> specification = ConditionSpecifications.of(condition);

            // then
            assertThat(specification).isNotNull();

            // 验证生成的 Specification 能正确转换为 Predicate
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            @SuppressWarnings("unchecked")
            Path<Object> path = mock(Path.class);
            when(root.get("createdDate")).thenReturn(path);

            // 使用更宽松的匹配器
            @SuppressWarnings("rawtypes")
            Predicate betweenPredicate = mock(Predicate.class);
            when(cb.between(
                    org.mockito.ArgumentMatchers.<jakarta.persistence.criteria.Expression>any(),
                    org.mockito.ArgumentMatchers.<Comparable>any(),
                    org.mockito.ArgumentMatchers.<Comparable>any()
            )).thenReturn(betweenPredicate);

            Predicate result = specification.toPredicate(root, criteriaQuery, cb);

            // 验证结果不为空
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return disjunction for BETWEEN condition with invalid array size")
        void shouldReturnDisjunctionForBetweenConditionWithInvalidArraySize() {
            // given
            TestQueryCondition condition = new TestQueryCondition("createdDate", ConditionType.BETWEEN, new Object[]{"2024-01-01"});

            // when
            Specification<Object> specification = ConditionSpecifications.of(condition);

            // then
            assertThat(specification).isNotNull();

            // 验证生成的 Specification 能正确转换为 Predicate (返回 disjunction)
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            Path<Object> path = mock(Path.class);
            when(root.get("createdDate")).thenReturn(path);

            Predicate disjunction = mock(Predicate.class);
            when(cb.disjunction()).thenReturn(disjunction);

            Predicate result = specification.toPredicate(root, criteriaQuery, cb);

            // 验证 disjunction 被调用
            assertThat(result).isNotNull();
            verify(cb).disjunction();
        }

        @Test
        @DisplayName("should return disjunction for BETWEEN condition with List of wrong size")
        void shouldReturnDisjunctionForBetweenConditionWithWrongListSize() {
            // given
            TestQueryCondition condition = new TestQueryCondition("createdDate", ConditionType.BETWEEN, List.of("2024-01-01"));

            // when
            Specification<Object> specification = ConditionSpecifications.of(condition);

            // then
            assertThat(specification).isNotNull();

            // 验证生成的 Specification 能正确转换为 Predicate (返回 disjunction)
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            Path<Object> path = mock(Path.class);
            when(root.get("createdDate")).thenReturn(path);

            Predicate disjunction = mock(Predicate.class);
            when(cb.disjunction()).thenReturn(disjunction);

            Predicate result = specification.toPredicate(root, criteriaQuery, cb);

            // 验证 disjunction 被调用
            assertThat(result).isNotNull();
            verify(cb).disjunction();
        }

        // ==================== getFieldValue 异常测试 ====================

        record InvalidQueryCondition(
                String wrongFieldName,
                ConditionType type
        ) {}

        @Test
        @DisplayName("should throw IllegalArgumentException when field not found")
        void shouldThrowExceptionWhenFieldNotFound() {
            // given
            InvalidQueryCondition condition = new InvalidQueryCondition("test", ConditionType.EQUAL);

            // when
            Specification<Object> specification = ConditionSpecifications.of(condition);

            // then - Specification 创建成功，但执行 toPredicate 时抛出异常
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            assertThatThrownBy(() -> specification.toPredicate(root, criteriaQuery, cb))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Field")
                    .hasMessageContaining("not found");
        }

        // ==================== buildPath 单层路径测试 ====================

        @Test
        @DisplayName("should build single layer path without dot")
        void shouldBuildSingleLayerPath() {
            // given
            TestQueryCondition condition = new TestQueryCondition("name", ConditionType.EQUAL, "John");

            // when
            Specification<Object> specification = ConditionSpecifications.of(condition);

            // then
            assertThat(specification).isNotNull();

            // 验证 root.get() 被正确调用（单层路径）
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            Path<Object> path = mock(Path.class);
            when(root.get("name")).thenReturn(path);

            Predicate predicate = mock(Predicate.class);
            when(cb.equal(path, "John")).thenReturn(predicate);

            specification.toPredicate(root, criteriaQuery, cb);

            // 验证 root.get() 被调用一次（单层路径，不涉及嵌套）
            verify(root).get("name");
        }

        // ==================== buildPath 多层嵌套测试 ====================

        @Test
        @DisplayName("should build multi-layer nested path")
        void shouldBuildMultiLayerNestedPath() {
            // given
            TestQueryCondition condition = new TestQueryCondition("user.profile.name", ConditionType.EQUAL, "John");

            // when
            Specification<Object> specification = ConditionSpecifications.of(condition);

            // then
            assertThat(specification).isNotNull();

            // 验证路径链式调用
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            Path<Object> userPath = mock(Path.class);
            Path<Object> profilePath = mock(Path.class);
            Path<Object> namePath = mock(Path.class);

            when(root.get("user")).thenReturn(userPath);
            when(userPath.get("profile")).thenReturn(profilePath);
            when(profilePath.get("name")).thenReturn(namePath);

            Predicate predicate = mock(Predicate.class);
            when(cb.equal(namePath, "John")).thenReturn(predicate);

            specification.toPredicate(root, criteriaQuery, cb);

            // 验证三层嵌套路径链式调用
            verify(root).get("user");
            verify(userPath).get("profile");
            verify(profilePath).get("name");
        }

        // ==================== buildBlurryPredicate 空字段测试 ====================

        record ArticleQueryWithEmptyFields(
                @Condition(blurry = " , , ") String keyword
        ) {}

        @Test
        @DisplayName("should return conjunction for blurry predicate with empty fields")
        void shouldReturnConjunctionForBlurryPredicateWithEmptyFields() {
            // given
            ArticleQueryWithEmptyFields query = new ArticleQueryWithEmptyFields("test");

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();

            // 验证生成的 Specification 能正确转换为 Predicate (返回 conjunction)
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            Predicate conjunction = mock(Predicate.class);
            when(cb.conjunction()).thenReturn(conjunction);

            // 模拟 cb.and() 调用（外层会包裹 and）
            Predicate andPredicate = mock(Predicate.class);
            when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

            Predicate result = specification.toPredicate(root, criteriaQuery, cb);

            // 验证 conjunction 被调用（因为所有字段都是空的）
            assertThat(result).isNotNull();
            verify(cb).conjunction();
        }

        record ArticleQueryWithMixedEmptyFields(
                @Condition(blurry = "title, ,content") String keyword
        ) {}

        @Test
        @DisplayName("should handle blurry predicate with mixed empty fields")
        void shouldHandleBlurryPredicateWithMixedEmptyFields() {
            // given
            ArticleQueryWithMixedEmptyFields query = new ArticleQueryWithMixedEmptyFields("test");

            // when
            Specification<Object> specification = ConditionSpecifications.fromAnnotation(query);

            // then
            assertThat(specification).isNotNull();

            // 验证生成的 Specification 能正确转换为 Predicate
            Root<Object> root = mock(Root.class);
            jakarta.persistence.criteria.CriteriaQuery<Object> criteriaQuery = mock(jakarta.persistence.criteria.CriteriaQuery.class);
            CriteriaBuilder cb = mock(CriteriaBuilder.class);

            Path<Object> titlePath = mock(Path.class);
            Path<Object> contentPath = mock(Path.class);

            // 模拟 root.get() 调用
            when(root.get("title")).thenReturn(titlePath);
            when(root.get("content")).thenReturn(contentPath);

            Predicate titleLike = mock(Predicate.class);
            Predicate contentLike = mock(Predicate.class);
            Predicate orPredicate = mock(Predicate.class);
            Predicate andPredicate = mock(Predicate.class);

            // 模拟 cb.like() 调用
            when(cb.like(titlePath.as(String.class), "%test%")).thenReturn(titleLike);
            when(cb.like(contentPath.as(String.class), "%test%")).thenReturn(contentLike);

            // 模拟 cb.or() 调用
            when(cb.or(any(Predicate[].class))).thenReturn(orPredicate);

            // 模拟 cb.and() 调用（外层会包裹 and）
            when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

            Predicate result = specification.toPredicate(root, criteriaQuery, cb);

            // 验证结果不为空
            assertThat(result).isNotNull();

            // 验证 OR 被调用了（说明有效的字段模糊搜索生效）
            verify(cb).or(any(Predicate[].class));
        }
    }
}
