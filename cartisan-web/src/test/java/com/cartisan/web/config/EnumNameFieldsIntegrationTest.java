package com.cartisan.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code cartisan.web.enum-name-fields.enabled} 开关的装配集成测试。
 *
 * <p>验证默认关闭（JSON 无 {@code xxxName}）、显式开启后 ObjectMapper
 * 自动为 BaseEnum 属性追加展示名字段。</p>
 */
class EnumNameFieldsIntegrationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    RedisAutoConfiguration.class, CartisanWebAutoConfiguration.class));

    @Test
    void shouldNotEmitNameField_byDefault() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            String json = mapper.writeValueAsString(new UserResponse(TestUserStatus.ACTIVE));

            assertThat(json).contains("\"status\":1");
            assertThat(json).doesNotContain("statusName");
        });
    }

    @Test
    void shouldNotEmitNameField_whenExplicitlyDisabled() {
        runner.withPropertyValues("cartisan.web.enum-name-fields.enabled=false")
                .run(context -> {
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    String json = mapper.writeValueAsString(new UserResponse(TestUserStatus.ACTIVE));

                    assertThat(json).doesNotContain("statusName");
                });
    }

    @Test
    void shouldEmitNameField_whenEnabled() {
        runner.withPropertyValues("cartisan.web.enum-name-fields.enabled=true")
                .run(context -> {
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    String json = mapper.writeValueAsString(new UserResponse(TestUserStatus.ACTIVE));

                    assertThat(json).contains("\"status\":1");
                    assertThat(json).contains("\"statusName\":\"启用\"");
                });
    }

    record UserResponse(TestUserStatus status) {
    }
}
