package com.cartisan.data.jpa.repository.impl;

import com.cartisan.event.DomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * DomainEventPublisherHolder 单元测试。
 */
@DisplayName("DomainEventPublisherHolder 测试")
class DomainEventPublisherHolderTest {

    @Test
    @DisplayName("should_setPublisher_and_getPublisher_returns_same_publisher")
    void should_setPublisher_and_getPublisher_returns_same_publisher() {
        // Given
        DomainEventPublisher mockPublisher = mock(DomainEventPublisher.class);

        // When
        DomainEventPublisherHolder.setPublisher(mockPublisher);
        DomainEventPublisher result = DomainEventPublisherHolder.getPublisher();

        // Then
        assertThat(result).isSameAs(mockPublisher);
    }

    @Test
    @DisplayName("should_setPublisher_throw_NullPointerException_when_null")
    void should_setPublisher_throw_NullPointerException_when_null() {
        // When & Then
        assertThatThrownBy(() -> DomainEventPublisherHolder.setPublisher(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("publisher cannot be null");
    }

    @Test
    @DisplayName("should_getPublisher_return_null_initially")
    void should_getPublisher_return_null_initially() throws Exception {
        // Given: Reset the static field to null using reflection
        Field publisherField = DomainEventPublisherHolder.class.getDeclaredField("publisher");
        publisherField.setAccessible(true);
        publisherField.set(null, null);

        // When
        DomainEventPublisher result = DomainEventPublisherHolder.getPublisher();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should_private_constructor_throw_UnsupportedOperationException")
    void should_private_constructor_throw_UnsupportedOperationException() throws Exception {
        // Given
        var constructor = DomainEventPublisherHolder.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then
        assertThatThrownBy(() -> constructor.newInstance())
                .hasCauseExactlyInstanceOf(UnsupportedOperationException.class)
                .cause()
                .hasMessageContaining("Utility class");
    }
}
