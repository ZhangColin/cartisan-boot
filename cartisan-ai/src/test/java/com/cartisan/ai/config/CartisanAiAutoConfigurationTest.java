package com.cartisan.ai.config;

import com.cartisan.ai.provider.anthropic.AnthropicProvider;
import com.cartisan.ai.provider.deepseek.DeepSeekProvider;
import com.cartisan.ai.provider.openai.OpenAiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CartisanAiAutoConfiguration.class)
class CartisanAiAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }
}
