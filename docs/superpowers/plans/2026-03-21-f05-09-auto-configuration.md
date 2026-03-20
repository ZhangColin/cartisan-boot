# F05-09: 自动配置设计文档

> **Feature ID**: F05-09
> **日期**: 2026-03-21
> **状态**: 设计完成

## 目标

为 `cartisan-ai` 模块创建 Spring Boot 自动配置，实现零配置引入。

## 背景

F05-01 至 F05-08 已完成：
- 统一对话模型（ChatRequest/ChatResponse/ChatStreamEvent）
- Provider SPI（ModelProvider 接口）
- 三大 Provider 实现（OpenAI/DeepSeek/Anthropic）及对应 Properties 类
- ModelProviderRegistry
- SseHelper

各 Provider 类已有 `@ConfigurationProperties` 注解的 Properties 配置类，但尚未启用自动装配。

## 设计

### 1. 文件结构

```
cartisan-ai/src/main/java/com/cartisan/ai/config/
├── CartisanAiAutoConfiguration.java
└── package-info.java

cartisan-ai/src/main/resources/
└── META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 2. CartisanAiAutoConfiguration 类设计

```java
package com.cartisan.ai.config;

import com.cartisan.ai.provider.anthropic.AnthropicProperties;
import com.cartisan.ai.provider.anthropic.AnthropicProvider;
import com.cartisan.ai.provider.deepseek.DeepSeekProperties;
import com.cartisan.ai.provider.deepseek.DeepSeekProvider;
import com.cartisan.ai.provider.openai.OpenAiProperties;
import com.cartisan.ai.provider.openai.OpenAiProvider;
import com.cartisan.ai.provider.ModelProvider;
import com.cartisan.ai.provider.ModelProviderRegistry;
import com.cartisan.ai.provider.ModelUsageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationPropertiesScan("com.cartisan.ai.provider")
public class CartisanAiAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "cartisan.ai.openai", name = "apiKey")
    OpenAiProvider openAiProvider(OpenAiProperties properties) {
        return new OpenAiProvider(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cartisan.ai.deepseek", name = "apiKey")
    DeepSeekProvider deepSeekProvider(DeepSeekProperties properties) {
        return new DeepSeekProvider(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cartisan.ai.anthropic", name = "apiKey")
    AnthropicProvider anthropicProvider(AnthropicProperties properties) {
        return new AnthropicProvider(properties);
    }

    @Bean
    @ConditionalOnBean(ModelProvider.class)
    ModelProviderRegistry modelProviderRegistry(
            List<ModelProvider> providers,
            List<ModelUsageListener> listeners) {
        return new ModelProviderRegistry(providers, listeners);
    }
}
```

**关键设计决策：**

1. **`@ConfigurationPropertiesScan("com.cartisan.ai.provider")`**
   - 自动扫描 `provider` 包下所有 `@ConfigurationProperties` 类
   - 支持未来扩展（如 Gemini、Ollama），无需修改配置类

2. **`@ConditionalOnProperty` 匹配 `apiKey`**
   - 用户配置 api-key 即创建对应 Provider Bean
   - 语义清晰：无 api-key 时 Provider 无法工作
   - 符合"约定优于配置"原则

3. **`ModelUsageListener` 可选依赖**
   - Spring 自动注入空列表（无 Bean 时）
   - 用户可选择性定义自己的 Listener Bean

4. **`@ConditionalOnBean(ModelProvider.class)`**
   - 至少有一个 Provider 才创建 Registry
   - 避免无意义空 Registry

### 3. AutoConfiguration.imports 注册

创建 `cartisan-ai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```
com.cartisan.ai.config.CartisanAiAutoConfiguration
```

Spring Boot 启动时自动加载此类。

### 4. 测试策略

**测试类：** `CartisanAiAutoConfigurationTest`

使用 `@SpringBootTest` + `@Import(CartisanAiAutoConfiguration.class)` 进行集成测试：

| 测试场景 | 配置 | 预期 Bean |
|---------|------|----------|
| 仅 OpenAI | `cartisan.ai.openai.api-key=sk-xxx` | `OpenAiProvider`, `Registry` |
| 仅 DeepSeek | `cartisan.ai.deepseek.api-key=sk-xxx` | `DeepSeekProvider`, `Registry` |
| 仅 Anthropic | `cartisan.ai.anthropic.api-key=sk-xxx` | `AnthropicProvider`, `Registry` |
| 多 Provider | 配置多个 api-key | 对应 Provider, `Registry` |
| 无配置 | 空 | 无任何 Bean |
| 空字符串 api-key | `api-key: ""` | 对应 Provider 不创建 |

**验证方法：** `ApplicationContext.getBeanProvider().getIfAvailable()`

### 5. 用户使用方式

**配置（application.yml）：**

```yaml
cartisan:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1  # 可选
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      default-max-tokens: 4096  # 可选
```

**依赖引入（业务项目）：**

```gradle
dependencies {
    implementation("com.cartisan:cartisan-ai:1.0.0")
}
```

**使用：**

```java
@Service
public class ChatService {
    private final ModelProviderRegistry registry;

    public ChatService(ModelProviderRegistry registry) {
        this.registry = registry;
    }

    public String chat(String message) {
        return registry.chat("openai", new ChatRequest("gpt-4o", List.of(...))).content();
    }
}
```

## 依赖关系

```
F05-01 ~ F05-08 ──→ F05-09
```

F05-09 依赖所有前置 Feature 已完成。

## 验收标准

1. `CartisanAiAutoConfiguration` 类编译通过
2. `AutoConfiguration.imports` 文件正确创建
3. 集成测试覆盖所有条件装配场景
4. 引入依赖后零配置自动生效

## 后续工作

本 Feature 完成后，Epic-05 所有 Feature 即交付完毕。可进行归档：
- 提取 API 文档到 `docs/guide/`
- 清理过程文档
