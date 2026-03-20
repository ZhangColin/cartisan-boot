package com.cartisan.ai.config;

import com.cartisan.ai.provider.ModelProvider;
import com.cartisan.ai.provider.anthropic.AnthropicProperties;
import com.cartisan.ai.provider.anthropic.AnthropicProvider;
import com.cartisan.ai.provider.deepseek.DeepSeekProperties;
import com.cartisan.ai.provider.deepseek.DeepSeekProvider;
import com.cartisan.ai.provider.openai.OpenAiProperties;
import com.cartisan.ai.provider.openai.OpenAiProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationPropertiesScan("com.cartisan.ai")
@EnableConfigurationProperties({
    OpenAiProperties.class,
    DeepSeekProperties.class,
    AnthropicProperties.class
})
public class CartisanAiAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "cartisan.ai.openai", name = "api-key")
    OpenAiProvider openAiProvider(OpenAiProperties properties) {
        return new OpenAiProvider(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cartisan.ai.deepseek", name = "api-key")
    DeepSeekProvider deepSeekProvider(DeepSeekProperties properties) {
        return new DeepSeekProvider(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cartisan.ai.anthropic", name = "api-key")
    AnthropicProvider anthropicProvider(AnthropicProperties properties) {
        return new AnthropicProvider(properties);
    }
}
