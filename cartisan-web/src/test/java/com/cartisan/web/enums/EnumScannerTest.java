package com.cartisan.web.enums;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnumScannerTest {

    private EnumRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EnumRegistry();
    }

    @Test
    void shouldScanAndRegisterEnums() {
        EnumScanner scanner = new EnumScanner(registry);
        // 扫描 com.cartisan.web.config 包，其中包含 TestUserStatus 枚举
        scanner.scanBaseEnums("com.cartisan.web.config");

        List<String> enums = registry.listRegisteredEnums();
        assertThat(enums).contains("TestUserStatus");
    }

    @Test
    void shouldNotRegisterNonBaseEnumClasses() {
        EnumScanner scanner = new EnumScanner(registry);
        scanner.scanBaseEnums("java.lang");

        // String 不是 BaseEnum，不应该被注册
        assertThat(registry.listRegisteredEnums()).isEmpty();
    }
}