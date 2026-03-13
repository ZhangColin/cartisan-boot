package com.cartisan.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Identity 接口测试。
 *
 * <p>验证 Identity 接口可被 Record 实现，且 value() 方法返回正确值。</p>
 */
class IdentityTest {

    /**
     * 测试用 ID 类型 - 使用 Record 实现 Identity。
     */
    private record TestUserId(String value) implements Identity<String> {
    }

    /**
     * 另一个测试用 ID 类型 - 验证类型安全。
     */
    private record TestOrderId(Long value) implements Identity<Long> {
    }

    @Test
    void shouldReturnCorrectValue_whenRecordImplementation() {
        // Given
        String expectedId = "user-123";
        TestUserId userId = new TestUserId(expectedId);

        // When
        String actualValue = userId.value();

        // Then
        assertThat(actualValue).isEqualTo(expectedId);
    }

    @Test
    void shouldReturnCorrectLong_whenLongIdentity() {
        // Given
        Long expectedId = 999L;
        TestOrderId orderId = new TestOrderId(expectedId);

        // When
        Long actualValue = orderId.value();

        // Then
        assertThat(actualValue).isEqualTo(expectedId);
    }

    @Test
    void shouldHaveDifferentTypes_whenComparingDifferentRecordTypes() {
        // Given
        TestUserId userId = new TestUserId("123");

        // 编译期验证：TestUserId 和 TestOrderId 是不同类型
        // 以下代码编译器会拒绝：
        // TestOrderId orderId = userId; // 编译错误：类型不兼容
        // Identity<String> stringId = new TestOrderId(123L); // 编译错误：类型不兼容

        // Then - 运行时验证
        assertThat(userId.value()).isEqualTo("123");
        assertThat(userId.getClass().getSimpleName()).isEqualTo("TestUserId");
    }

    @Test
    void shouldReturnNull_whenValueIsNull() {
        // Given - Identity 接口不限制 value 是否为 null
        // 这允许具体的 ID 类型自行决定 null 语义
        TestUserId userId = new TestUserId(null);

        // When
        String actualValue = userId.value();

        // Then
        assertThat(actualValue).isNull();
    }
}
