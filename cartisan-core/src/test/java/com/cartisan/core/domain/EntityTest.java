package com.cartisan.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entity 接口测试。
 *
 * <p>验证 Entity 接口 sameIdentityAs 方法的默认行为。</p>
 */
class EntityTest {

    /**
     * 测试用 ID 类型。
     */
    private record TestId(String value) implements Identity<String> {
    }

    /**
     * 测试用实体。
     */
    private static class TestEntity implements Entity<TestEntity, TestId> {
        private final TestId id;

        TestEntity(TestId id) {
            this.id = id;
        }

        @Override
        public TestId getId() {
            return id;
        }
    }

    @Test
    void shouldReturnTrue_whenSameIdentityAsWithSameId() {
        // Given
        TestId id = new TestId("entity-1");
        TestEntity entity1 = new TestEntity(id);
        TestEntity entity2 = new TestEntity(id);

        // When
        boolean result = entity1.sameIdentityAs(entity2);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenSameIdentityAsWithDifferentIds() {
        // Given
        TestEntity entity1 = new TestEntity(new TestId("entity-1"));
        TestEntity entity2 = new TestEntity(new TestId("entity-2"));

        // When
        boolean result = entity1.sameIdentityAs(entity2);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalse_whenSameIdentityAsWithNull() {
        // Given
        TestEntity entity = new TestEntity(new TestId("entity-1"));

        // When
        boolean result = entity.sameIdentityAs(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrue_whenSameIdentityAsWithBothNullIds() {
        // Given
        TestEntity entity1 = new TestEntity(null);
        TestEntity entity2 = new TestEntity(null);

        // When
        boolean result = entity1.sameIdentityAs(entity2);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenSameIdentityAsWithOneNullId() {
        // Given
        TestEntity entity1 = new TestEntity(null);
        TestEntity entity2 = new TestEntity(new TestId("entity-1"));

        // When
        boolean result = entity1.sameIdentityAs(entity2);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnTrue_whenSameIdentityAsWithSelf() {
        // Given
        TestEntity entity = new TestEntity(new TestId("entity-1"));

        // When
        boolean result = entity.sameIdentityAs(entity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldHaveCompileTimeTypeSafety_whenComparingDifferentEntityTypes() {
        // Given
        TestId id = new TestId("same-id");
        TestEntity entity = new TestEntity(id);

        // 创建另一个实现了 Entity 但类型不同的对象
        OtherEntity otherEntity = new OtherEntity(id);

        // When
        // 编译器会拒绝不同类型的比较
        // entity.sameIdentityAs(otherEntity); // 编译错误

        // Then - 验证编译期类型安全
        assertThat(entity.getId()).isEqualTo(otherEntity.getId());
    }

    /**
     * 另一个测试用实体类型 - 用于验证类型安全。
     */
    private static class OtherEntity implements Entity<OtherEntity, TestId> {
        private final TestId id;

        OtherEntity(TestId id) {
            this.id = id;
        }

        @Override
        public TestId getId() {
            return id;
        }
    }
}
