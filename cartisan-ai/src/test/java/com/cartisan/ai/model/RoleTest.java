package com.cartisan.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void shouldHaveThreeRoles() {
        assertThat(Role.values()).containsExactly(Role.SYSTEM, Role.USER, Role.ASSISTANT);
    }
}
