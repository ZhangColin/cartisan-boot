package com.cartisan.web.enums;

import com.cartisan.web.config.TestUserStatus;
import com.cartisan.web.response.EnumOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumRegistryTest {

    private EnumRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EnumRegistry();
    }

    @Test
    void shouldRegisterAndGetEnum() {
        registry.register("TestUserStatus", TestUserStatus.class);

        Class<? extends com.cartisan.core.domain.BaseEnum<?>> enumClass =
            registry.getEnumClass("TestUserStatus");
        assertThat(enumClass).isEqualTo(TestUserStatus.class);
    }

    @Test
    void shouldGetEnumOptions() {
        registry.register("TestUserStatus", TestUserStatus.class);

        List<EnumOption> options = registry.getEnumOptions("TestUserStatus");

        assertThat(options).hasSize(3);
        assertThat(options.get(0).code()).isEqualTo(1);
    }

    @Test
    void shouldThrowExceptionWhenEnumNotFound() {
        assertThatThrownBy(() -> registry.getEnumClass("NotExist"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Enum not found: NotExist");
    }

    @Test
    void shouldThrowExceptionWhenRegisteringNonEnum() {
        class NotAnEnum {
            @Override public String toString() { return "NotAnEnum"; }
        }

        assertThatThrownBy(() -> registry.register("NotAnEnum", (Class<? extends com.cartisan.core.domain.BaseEnum<?>>) (Class<?>) NotAnEnum.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Class must be an enum");
    }

    @Test
    void shouldListRegisteredEnums() {
        registry.register("TestUserStatus", TestUserStatus.class);
        registry.register("AnotherEnum", TestUserStatus.class);

        List<String> enums = registry.listRegisteredEnums();

        assertThat(enums).hasSize(2);
        assertThat(enums).containsExactlyInAnyOrder("AnotherEnum", "TestUserStatus");
    }
}