package com.cartisan.web.support;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScanPackageResolver 单元测试。
 */
class ScanPackageResolverTest {

    @Test
    void shouldReturnConfiguredPackages_whenExplicitlyConfigured() {
        String[] resolved = ScanPackageResolver.resolve(new String[]{"com.acme"}, null);

        assertThat(resolved).containsExactly("com.acme");
    }

    @Test
    void shouldIgnoreBlankEntries_whenConfigured() {
        String[] resolved = ScanPackageResolver.resolve(new String[]{"com.acme", " ", ""}, null);

        assertThat(resolved).containsExactly("com.acme");
    }

    @Test
    void shouldUseApplicationBasePackage_whenNotConfigured() {
        GenericApplicationContext context = new GenericApplicationContext();
        AutoConfigurationPackages.register(context, "com.aieducenter.aiplatform");
        context.refresh();
        try {
            String[] resolved = ScanPackageResolver.resolve(new String[0], context);

            assertThat(resolved).containsExactly("com.aieducenter.aiplatform");
        } finally {
            context.close();
        }
    }

    @Test
    void shouldFallbackToCartisanPackage_whenNoConfigurationAndNoApplicationPackage() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.refresh();
        try {
            String[] resolved = ScanPackageResolver.resolve(new String[0], context);

            assertThat(resolved).containsExactly("com.cartisan");
        } finally {
            context.close();
        }
    }

    @Test
    void shouldFallbackToCartisanPackage_whenAllConfiguredEntriesBlank() {
        String[] resolved = ScanPackageResolver.resolve(new String[]{" ", ""}, null);

        assertThat(resolved).containsExactly("com.cartisan");
    }
}
