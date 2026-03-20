# F05-09 自动配置实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 为 cartisan-ai 模块创建 Spring Boot 自动配置，实现零配置引入

**架构：** 使用 `@ConfigurationPropertiesScan` 扫描配置属性类，`@ConditionalOnProperty` 按需创建 Provider Bean，`@ConditionalOnBean` 确保 Registry 至少有一个 Provider 才创建

**技术栈：** Spring Boot 3.4.x AutoConfiguration、JUnit 5、AssertJ

---

## 文件结构

```
cartisan-ai/src/main/java/com/cartisan/ai/config/
├── CartisanAiAutoConfiguration.java    # 主配置类
└── package-info.java                    # 包文档

cartisan-ai/src/test/java/com/cartisan/ai/config/
└── CartisanAiAutoConfigurationTest.java # 集成测试

cartisan-ai/src/main/resources/
└── META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## Task 1: 验证 config 包

**Files:**
- Verify: `cartisan-ai/src/main/java/com/cartisan/ai/config/package-info.java`（已存在）

- [ ] **Step 1: 验证 package-info.java 已存在**

Run: `cat cartisan-ai/src/main/java/com/cartisan/ai/config/package-info.java`
Expected: 文件存在且内容为包文档

- [ ] **Step 2: 跳过此任务**

`config` 包已在 F05-01 模块骨架中创建，无需额外操作。

---

## Task 2: 创建 CartisanAiAutoConfiguration 类骨架

**Files:**
- Create: `cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java`

- [ ] **Step 1: 创建空配置类**

```java
package com.cartisan.ai.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan("com.cartisan.ai")
public class CartisanAiAutoConfiguration {
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :cartisan-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java
git commit -m "feat(cartisan-ai): add CartisanAiAutoConfiguration skeleton"
```

---

## Task 3: 创建 AutoConfiguration.imports 注册文件

**Files:**
- Create: `cartisan-ai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: 创建目录结构**

Run: `mkdir -p cartisan-ai/src/main/resources/META-INF/spring`

- [ ] **Step 2: 创建 imports 文件**

文件内容：
```
com.cartisan.ai.config.CartisanAiAutoConfiguration
```

- [ ] **Step 3: 验证文件内容**

Run: `cat cartisan-ai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
Expected: 显示 `com.cartisan.ai.config.CartisanAiAutoConfiguration`

- [ ] **Step 4: 提交**

```bash
git add cartisan-ai/src/main/resources/
git commit -m "feat(cartisan-ai): register AutoConfiguration imports"
```

---

## Task 4: 编写集成测试框架

**Files:**
- Create: `cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java`
- Create: `cartisan-ai/src/test/resources/application-test.yml`（目录不存在，需创建）

- [ ] **Step 1: 创建测试资源目录**

Run: `mkdir -p cartisan-ai/src/test/resources`

- [ ] **Step 2: 创建测试配置文件**

Create: `cartisan-ai/src/test/resources/application-test.yml`

```yaml
# 测试配置文件，具体测试会覆盖属性
cartisan:
  ai:
    openai:
      api-key: ""
    deepseek:
      api-key: ""
    anthropic:
      api-key: ""
```

- [ ] **Step 3: 创建测试类骨架**

```java
package com.cartisan.ai.config;

import com.cartisan.ai.provider.ModelProvider;
import com.cartisan.ai.provider.ModelProviderRegistry;
import com.cartisan.ai.provider.anthropic.AnthropicProvider;
import com.cartisan.ai.provider.deepseek.DeepSeekProvider;
import com.cartisan.ai.provider.openai.OpenAiProvider;
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
}
```

- [ ] **Step 4: 运行测试验证框架**

Run: `./gradlew :cartisan-ai:test --tests CartisanAiAutoConfigurationTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java
git add cartisan-ai/src/test/resources/application-test.yml
git commit -m "test(cartisan-ai): add AutoConfiguration test framework"
```

---

## Task 5: 添加 OpenAiProvider Bean

**Files:**
- Modify: `cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java`
- Modify: `cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java`

- [ ] **Step 1: 在配置类中添加 OpenAiProvider Bean**

在 `CartisanAiAutoConfiguration` 类中添加：

```java
import com.cartisan.ai.provider.openai.OpenAiProperties;
import com.cartisan.ai.provider.openai.OpenAiProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

// 在类中添加方法
@Bean
@ConditionalOnProperty(prefix = "cartisan.ai.openai", name = "apiKey")
OpenAiProvider openAiProvider(OpenAiProperties properties) {
    return new OpenAiProvider(properties);
}
```

- [ ] **Step 2: 编写测试 - 仅配置 OpenAI 时创建 Bean**

在 `CartisanAiAutoConfigurationTest` 中添加内部类：

```java
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

        @Test
        void shouldCreateModelProviderRegistry() {
            ModelProviderRegistry registry = context.getBean(ModelProviderRegistry.class);
            assertThat(registry).isNotNull();
        }
    }
```

- [ ] **Step 3: 运行测试**

Run: `./gradlew :cartisan-ai:test --tests CartisanAiAutoConfigurationTest`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java
git add cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java
git commit -m "feat(cartisan-ai): add OpenAiProvider conditional bean"
```

---

## Task 6: 添加 DeepSeekProvider Bean

**Files:**
- Modify: `cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java`
- Modify: `cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java`

- [ ] **Step 1: 在配置类中添加 DeepSeekProvider Bean**

在 `CartisanAiAutoConfiguration` 类中添加：

```java
import com.cartisan.ai.provider.deepseek.DeepSeekProperties;
import com.cartisan.ai.provider.deepseek.DeepSeekProvider;

@Bean
@ConditionalOnProperty(prefix = "cartisan.ai.deepseek", name = "apiKey")
DeepSeekProvider deepSeekProvider(DeepSeekProperties properties) {
    return new DeepSeekProvider(properties);
}
```

- [ ] **Step 2: 编写测试 - 仅配置 DeepSeek 时创建 Bean**

在 `CartisanAiAutoConfigurationTest` 中添加内部类：

```java
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
```

- [ ] **Step 3: 运行测试**

Run: `./gradlew :cartisan-ai:test --tests CartisanAiAutoConfigurationTest`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java
git add cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java
git commit -m "feat(cartisan-ai): add DeepSeekProvider conditional bean"
```

---

## Task 7: 添加 AnthropicProvider Bean

**Files:**
- Modify: `cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java`
- Modify: `cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java`

- [ ] **Step 1: 在配置类中添加 AnthropicProvider Bean**

在 `CartisanAiAutoConfiguration` 类中添加：

```java
import com.cartisan.ai.provider.anthropic.AnthropicProperties;
import com.cartisan.ai.provider.anthropic.AnthropicProvider;

@Bean
@ConditionalOnProperty(prefix = "cartisan.ai.anthropic", name = "apiKey")
AnthropicProvider anthropicProvider(AnthropicProperties properties) {
    return new AnthropicProvider(properties);
}
```

- [ ] **Step 2: 编写测试 - 仅配置 Anthropic 时创建 Bean**

在 `CartisanAiAutoConfigurationTest` 中添加内部类：

```java
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
```

- [ ] **Step 3: 运行测试**

Run: `./gradlew :cartisan-ai:test --tests CartisanAiAutoConfigurationTest`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java
git add cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java
git commit -m "feat(cartisan-ai): add AnthropicProvider conditional bean"
```

---

## Task 8: 添加 ModelProviderRegistry Bean

**Files:**
- Modify: `cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java`
- Modify: `cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java`

- [ ] **Step 1: 在配置类中添加 ModelProviderRegistry Bean**

在 `CartisanAiAutoConfiguration` 类中添加：

```java
import com.cartisan.ai.provider.ModelProvider;
import com.cartisan.ai.provider.ModelProviderRegistry;
import com.cartisan.ai.provider.ModelUsageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

@Bean
@ConditionalOnBean(ModelProvider.class)
ModelProviderRegistry modelProviderRegistry(
        List<ModelProvider> providers,
        List<ModelUsageListener> listeners) {
    return new ModelProviderRegistry(providers, listeners);
}
```

- [ ] **Step 2: 编写测试 - 多 Provider 场景**

在 `CartisanAiAutoConfigurationTest` 中添加内部类：

```java
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
```

- [ ] **Step 3: 运行测试**

Run: `./gradlew :cartisan-ai:test --tests CartisanAiAutoConfigurationTest`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java
git add cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java
git commit -m "feat(cartisan-ai): add ModelProviderRegistry conditional bean"
```

---

## Task 9: 测试无配置场景

**Files:**
- Modify: `cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java`

- [ ] **Step 1: 编写测试 - 无任何 api-key 配置**

在 `CartisanAiAutoConfigurationTest` 中添加内部类：

```java
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
```

- [ ] **Step 2: 运行测试**

Run: `./gradlew :cartisan-ai:test --tests CartisanAiAutoConfigurationTest`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add cartisan-ai/src/test/java/com/cartisan/ai/config/CartisanAiAutoConfigurationTest.java
git commit -m "test(cartisan-ai): add test for no provider configured scenario"
```

---

## Task 10: 全量测试与验证

- [ ] **Step 1: 运行完整测试套件**

Run: `./gradlew :cartisan-ai:test`
Expected: 所有测试通过

- [ ] **Step 2: 验证编译**

Run: `./gradlew :cartisan-ai:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 检查最终代码结构**

Run: `find cartisan-ai/src/main/java/com/cartisan/ai/config -type f -name "*.java"`
Expected输出：
```
cartisan-ai/src/main/java/com/cartisan/ai/config/CartisanAiAutoConfiguration.java
cartisan-ai/src/main/java/com/cartisan/ai/config/package-info.java
```

Run: `cat cartisan-ai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
Expected输出：
```
com.cartisan.ai.config.CartisanAiAutoConfiguration
```

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "feat(cartisan-ai): complete F05-09 auto-configuration implementation"
```

---

## 验收标准

1. ✅ `CartisanAiAutoConfiguration` 类存在且编译通过
2. ✅ `AutoConfiguration.imports` 文件正确注册
3. ✅ 集成测试覆盖所有条件装配场景（OpenAI/DeepSeek/Anthropic/多 Provider/无配置）
4. ✅ `@ConfigurationPropertiesScan` 覆盖 `com.cartisan.ai` 包，确保所有 Properties 类可用
5. ✅ 引入依赖后零配置自动生效

---

## 参考文档

- 设计文档：[docs/superpowers/plans/2026-03-21-f05-09-auto-configuration.md](docs/superpowers/plans/2026-03-21-f05-09-auto-configuration.md)
- Epic Backlog：[docs/specs/epic-05-ai/00_epic_backlog.md](docs/specs/epic-05-ai/00_epic_backlog.md)
