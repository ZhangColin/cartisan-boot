package com.cartisan.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ValueObject 接口测试。
 *
 * <p>验证 ValueObject 接口 sameValueAs 方法的默认行为。</p>
 */
class ValueObjectTest {

    /**
     * 测试用值对象 - 使用 Record 实现。
     */
    private record TestValueObject(String value, int number) implements ValueObject<TestValueObject> {
    }

    /**
     * 另一个测试用值对象 - 相同结构。
     */
    private record AnotherValueObject(String value, int number) implements ValueObject<AnotherValueObject> {
    }

    /**
     * 测试用值对象 - 自定义 sameValueAs 逻辑。
     */
    private static class CustomValueObject implements ValueObject<CustomValueObject> {
        private final String value;
        private final int number;

        CustomValueObject(String value, int number) {
            this.value = value;
            this.number = number;
        }

        @Override
        public boolean sameValueAs(CustomValueObject other) {
            // 自定义逻辑：忽略大小写比较 value
            if (other == null) {
                return false;
            }
            return value.equalsIgnoreCase(other.value) && number == other.number;
        }

        @Override
        public boolean equals(Object obj) {
            // 标准 equals 实现（区分大小写）
            if (this == obj) return true;
            if (!(obj instanceof CustomValueObject)) return false;
            CustomValueObject other = (CustomValueObject) obj;
            return number == other.number && value.equals(other.value);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(value, number);
        }
    }

    @Test
    void shouldReturnTrue_whenSameValueAsWithSameValue() {
        // Given
        TestValueObject vo1 = new TestValueObject("test", 123);
        TestValueObject vo2 = new TestValueObject("test", 123);

        // When
        boolean result = vo1.sameValueAs(vo2);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenSameValueAsWithDifferentValue() {
        // Given
        TestValueObject vo1 = new TestValueObject("test", 123);
        TestValueObject vo2 = new TestValueObject("other", 456);

        // When
        boolean result = vo1.sameValueAs(vo2);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalse_whenSameValueAsWithNull() {
        // Given
        TestValueObject vo = new TestValueObject("test", 123);

        // When
        boolean result = vo.sameValueAs(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrue_whenSameValueAsWithSelf() {
        // Given
        TestValueObject vo = new TestValueObject("test", 123);

        // When
        boolean result = vo.sameValueAs(vo);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldBeEquivalentToEquals_whenSameValueAsForRecord() {
        // Given
        TestValueObject vo1 = new TestValueObject("test", 123);
        TestValueObject vo2 = new TestValueObject("test", 123);
        TestValueObject vo3 = new TestValueObject("other", 456);

        // When & Then
        assertThat(vo1.sameValueAs(vo2)).isEqualTo(vo1.equals(vo2));
        assertThat(vo1.sameValueAs(vo3)).isEqualTo(vo1.equals(vo3));
    }

    @Test
    void shouldUseCustomLogic_whenSameValueAsForCustomValueObject() {
        // Given
        CustomValueObject vo1 = new CustomValueObject("TEST", 123);
        CustomValueObject vo2 = new CustomValueObject("test", 123);

        // When
        boolean sameValueAsResult = vo1.sameValueAs(vo2);
        boolean equalsResult = vo1.equals(vo2);

        // Then - sameValueAs 忽略大小写，equals 区分大小写
        assertThat(sameValueAsResult).isTrue();
        assertThat(equalsResult).isFalse();
    }

    @Test
    void shouldReturnFalse_whenSameValueAsWithNullForCustomValueObject() {
        // Given
        CustomValueObject vo = new CustomValueObject("test", 123);

        // When
        boolean result = vo.sameValueAs(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldHaveCompileTimeTypeSafety_whenComparingDifferentTypes() {
        // Given
        TestValueObject vo1 = new TestValueObject("test", 123);
        AnotherValueObject vo2 = new AnotherValueObject("test", 123);

        // When - 编译器会拒绝不同类型的比较
        // vo1.sameValueAs(vo2); // 编译错误

        // Then - 验证编译期类型安全
        assertThat(vo1.value()).isEqualTo(vo2.value());
        assertThat(vo1.number()).isEqualTo(vo2.number());
    }
}
