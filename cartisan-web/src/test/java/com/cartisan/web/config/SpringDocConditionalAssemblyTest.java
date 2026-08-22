package com.cartisan.web.config;

import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * springdoc 集成的条件装配测试（#21）。
 *
 * <p>验证 {@code BaseEnumModelConverter} 仅在 classpath 存在 springdoc 时注册；
 * 无 springdoc 的服务上下文正常启动、零影响。</p>
 */
class SpringDocConditionalAssemblyTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    RedisAutoConfiguration.class, CartisanWebAutoConfiguration.class));

    @Test
    void should_register_baseEnumModelConverter_when_springdoc_present() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(BaseEnumModelConverter.class));
    }

    @Test
    void should_start_normally_and_skip_converter_when_springdoc_absent() {
        runner.withClassLoader(new FilteredClassLoader(SpringDocConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(BaseEnumModelConverter.class);
                });
    }
}
