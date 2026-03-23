package com.cartisan.data.jpa.specification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConditionType 单元测试。
 *
 * <p>验证 11 种查询类型枚举值存在</p>
 */
@DisplayName("ConditionType 测试")
class ConditionTypeTest {

    // ==================== 相等性比较 ====================

    @Test
    @DisplayName("should have EQUAL enum constant")
    void shouldHaveEqualEnumConstant() {
        assertThat(ConditionType.valueOf("EQUAL")).isNotNull();
    }

    @Test
    @DisplayName("should have NOT_EQUAL enum constant")
    void shouldHaveNotEqualEnumConstant() {
        assertThat(ConditionType.valueOf("NOT_EQUAL")).isNotNull();
    }

    // ==================== 大小比较 ====================

    @Test
    @DisplayName("should have GREATER_EQUAL enum constant")
    void shouldHaveGreaterEqualEnumConstant() {
        assertThat(ConditionType.valueOf("GREATER_EQUAL")).isNotNull();
    }

    @Test
    @DisplayName("should have GREATER enum constant")
    void shouldHaveGreaterEnumConstant() {
        assertThat(ConditionType.valueOf("GREATER")).isNotNull();
    }

    @Test
    @DisplayName("should have LESS_EQUAL enum constant")
    void shouldHaveLessEqualEnumConstant() {
        assertThat(ConditionType.valueOf("LESS_EQUAL")).isNotNull();
    }

    @Test
    @DisplayName("should have LESS enum constant")
    void shouldHaveLessEnumConstant() {
        assertThat(ConditionType.valueOf("LESS")).isNotNull();
    }

    // ==================== 模糊查询 ====================

    @Test
    @DisplayName("should have INNER_LIKE enum constant")
    void shouldHaveInnerLikeEnumConstant() {
        assertThat(ConditionType.valueOf("INNER_LIKE")).isNotNull();
    }

    @Test
    @DisplayName("should have LEFT_LIKE enum constant")
    void shouldHaveLeftLikeEnumConstant() {
        assertThat(ConditionType.valueOf("LEFT_LIKE")).isNotNull();
    }

    @Test
    @DisplayName("should have RIGHT_LIKE enum constant")
    void shouldHaveRightLikeEnumConstant() {
        assertThat(ConditionType.valueOf("RIGHT_LIKE")).isNotNull();
    }

    // ==================== 集合与区间查询 ====================

    @Test
    @DisplayName("should have IN enum constant")
    void shouldHaveInEnumConstant() {
        assertThat(ConditionType.valueOf("IN")).isNotNull();
    }

    @Test
    @DisplayName("should have BETWEEN enum constant")
    void shouldHaveBetweenEnumConstant() {
        assertThat(ConditionType.valueOf("BETWEEN")).isNotNull();
    }

    // ==================== 枚举完整性验证 ====================

    @Test
    @DisplayName("should have exactly 11 enum constants")
    void shouldHaveExactly11EnumConstants() {
        ConditionType[] values = ConditionType.values();
        assertThat(values).hasSize(11);
    }

    @Test
    @DisplayName("should have all expected enum values")
    void shouldHaveAllExpectedEnumValues() {
        List<String> names = Arrays.stream(ConditionType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder(
                "EQUAL", "NOT_EQUAL",
                "GREATER_EQUAL", "GREATER", "LESS_EQUAL", "LESS",
                "INNER_LIKE", "LEFT_LIKE", "RIGHT_LIKE",
                "IN", "BETWEEN"
        );
    }
}
