package com.cartisan.test.fixture;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FixtureBuilder 单元测试。
 */
class FixtureBuilderTest {

    // 测试用的简单 POJO
    static class SimplePojo {
        public Long id;
        public String name;
        public int count;
        public boolean active;
    }

    // 测试用的 POJO（无无参构造）
    static class NoArgConstructorPojo {
        public String value;

        public NoArgConstructorPojo(String value) {
            this.value = value;
        }
    }

    // 测试用的 Record（Java 16+）
    record TestRecord(String name, int value) {
    }

    // 嵌套对象
    static class Order {
        public Long id;
        public String orderNo;
        public BigDecimal amount;
        public LocalDateTime createdAt;
        public Customer customer;
    }

    static class Customer {
        public Long id;
        public String name;
        public Order order; // 循环引用
    }

    @Test
    void shouldBuild_whenClassHasNoArgConstructor() {
        SimplePojo result = FixtureBuilder.of(SimplePojo.class).build();

        assertThat(result).isNotNull();
        assertThat(result.id).isNotNull();
        assertThat(result.name).isNotEmpty();
        assertThat(result.count).isNotNull();
        assertThat(result.active).isNotNull();
    }

    @Test
    void shouldThrowException_whenClassIsRecord() {
        assertThatThrownBy(() -> FixtureBuilder.of(TestRecord.class))
                .isInstanceOf(FixtureBuildException.class)
                .hasMessageContaining("Record types not supported yet");
    }

    @Test
    void shouldThrowException_whenClassHasNoNoArgConstructor() {
        assertThatThrownBy(() -> FixtureBuilder.of(NoArgConstructorPojo.class))
                .isInstanceOf(FixtureBuildException.class)
                .hasMessageContaining("has no no-arg constructor");
    }

    @Test
    void shouldOverrideField_whenWithCalled() {
        SimplePojo result = FixtureBuilder.of(SimplePojo.class)
                .with("name", "TEST_NAME")
                .with("count", 42)
                .build();

        assertThat(result.name).isEqualTo("TEST_NAME");
        assertThat(result.count).isEqualTo(42);
    }

    @Test
    void shouldThrowException_whenFieldNotExist() {
        assertThatThrownBy(() -> FixtureBuilder.of(SimplePojo.class)
                .with("nonExistent", "value")
                .build())
                .isInstanceOf(FixtureBuildException.class)
                .hasMessageContaining("Field 'nonExistent' not found");
    }

    @Test
    void shouldBuildNestedObject_whenDepthAllows() {
        Order result = FixtureBuilder.of(Order.class)
                .maxDepth(2)
                .build();

        assertThat(result.id).isNotNull();
        assertThat(result.customer).isNotNull(); // 深度 2 填充 Customer
    }

    @Test
    void shouldLimitDepth_whenMaxDepthIsOne() {
        Order result = FixtureBuilder.of(Order.class)
                .maxDepth(1)
                .build();

        assertThat(result.id).isNotNull();
        assertThat(result.customer).isNull(); // 超过深度 1
    }
}
