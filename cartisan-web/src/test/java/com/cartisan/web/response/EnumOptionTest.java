package com.cartisan.web.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumOptionTest {

    @Test
    void shouldCreateEnumOption() {
        EnumOption option = new EnumOption(1, "激活");

        assertThat(option.code()).isEqualTo(1);
        assertThat(option.name()).isEqualTo("激活");
    }

    @Test
    void shouldImplementSerializable() {
        EnumOption option = new EnumOption(1, "激活");

        assertThat(option).isInstanceOf(java.io.Serializable.class);
    }
}