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
}
