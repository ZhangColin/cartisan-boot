package com.cartisan.ai.config;

import com.cartisan.ai.provider.ModelProviderRegistry;
import com.cartisan.ai.provider.anthropic.AnthropicProvider;
import com.cartisan.ai.provider.deepseek.DeepSeekProvider;
import com.cartisan.ai.provider.openai.OpenAiProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CartisanAiAutoConfiguration.class)
class CartisanAiAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Nested
    @TestPropertySource(properties = {
        "cartisan.ai.openai.api-key=sk-test-openai"
    })
    class OnlyOpenAiConfiguredTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void shouldCreateOpenAiProvider() {
            OpenAiProvider provider = context.getBean(OpenAiProvider.class);
            assertThat(provider).isNotNull();
            assertThat(provider.id()).isEqualTo("openai");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "cartisan.ai.deepseek.api-key=sk-test-deepseek"
    })
    class OnlyDeepSeekConfiguredTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void shouldCreateDeepSeekProvider() {
            DeepSeekProvider provider = context.getBean(DeepSeekProvider.class);
            assertThat(provider).isNotNull();
            assertThat(provider.id()).isEqualTo("deepseek");
        }

        @Test
        void shouldNotCreateOpenAiProvider() {
            assertThat(context.getBeanProvider(OpenAiProvider.class).getIfAvailable()).isNull();
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "cartisan.ai.anthropic.api-key=sk-test-anthropic"
    })
    class OnlyAnthropicConfiguredTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void shouldCreateAnthropicProvider() {
            AnthropicProvider provider = context.getBean(AnthropicProvider.class);
            assertThat(provider).isNotNull();
            assertThat(provider.id()).isEqualTo("anthropic");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "cartisan.ai.openai.api-key=sk-test-openai",
        "cartisan.ai.deepseek.api-key=sk-test-deepseek",
        "cartisan.ai.anthropic.api-key=sk-test-anthropic"
    })
    class AllProvidersConfiguredTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void shouldCreateAllProviders() {
            assertThat(context.getBean(OpenAiProvider.class)).isNotNull();
            assertThat(context.getBean(DeepSeekProvider.class)).isNotNull();
            assertThat(context.getBean(AnthropicProvider.class)).isNotNull();
        }

        @Test
        void shouldCreateRegistryWithAllProviders() {
            ModelProviderRegistry registry = context.getBean(ModelProviderRegistry.class);
            assertThat(registry.listProviders()).hasSize(3);
        }

        @Test
        void shouldFindProvidersById() {
            ModelProviderRegistry registry = context.getBean(ModelProviderRegistry.class);
            assertThat(registry.getProvider("openai")).isNotNull();
            assertThat(registry.getProvider("deepseek")).isNotNull();
            assertThat(registry.getProvider("anthropic")).isNotNull();
        }
    }

    @Nested
    class NoProviderConfiguredTest {

        @Autowired
        private ApplicationContext context;

        @Test
        void shouldNotCreateAnyProvider() {
            assertThat(context.getBeanProvider(OpenAiProvider.class).getIfAvailable()).isNull();
            assertThat(context.getBeanProvider(DeepSeekProvider.class).getIfAvailable()).isNull();
            assertThat(context.getBeanProvider(AnthropicProvider.class).getIfAvailable()).isNull();
        }

        @Test
        void shouldNotCreateRegistryWhenNoProviders() {
            assertThat(context.getBeanProvider(ModelProviderRegistry.class).getIfAvailable()).isNull();
        }
    }
}
