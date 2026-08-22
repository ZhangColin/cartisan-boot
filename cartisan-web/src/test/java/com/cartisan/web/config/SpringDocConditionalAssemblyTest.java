package com.cartisan.web.config;

import com.cartisan.web.doc.CodeMessageRegistry;
import com.cartisan.web.doc.ErrorCodeOperationCustomizer;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * springdoc 集成的条件装配测试（#21、#24）。
 *
 * <p>验证 {@code BaseEnumModelConverter} 与 @ErrorCodes 渲染组件
 * （{@code CodeMessageRegistry}、{@code ErrorCodeOperationCustomizer}）
 * 仅在 classpath 存在 springdoc 时注册；无 springdoc 的服务上下文正常启动、零影响。
 * 本 runner 未引入 WebMvc，{@code ErrorCodesValidator}（@ConditionalOnBean
 * RequestMappingHandlerMapping）不注册属预期，其装配由集成测试覆盖。</p>
 */
class SpringDocConditionalAssemblyTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    RedisAutoConfiguration.class, CartisanWebAutoConfiguration.class));

    @Test
    void should_register_springdoc_components_when_springdoc_present() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(BaseEnumModelConverter.class);
            assertThat(context).hasSingleBean(CodeMessageRegistry.class);
            assertThat(context).hasSingleBean(ErrorCodeOperationCustomizer.class);
        });
    }

    @Test
    void should_start_normally_and_skip_components_when_springdoc_absent() {
        runner.withClassLoader(new FilteredClassLoader(SpringDocConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(BaseEnumModelConverter.class);
                    assertThat(context).doesNotHaveBean(CodeMessageRegistry.class);
                    assertThat(context).doesNotHaveBean(ErrorCodeOperationCustomizer.class);
                });
    }
}
