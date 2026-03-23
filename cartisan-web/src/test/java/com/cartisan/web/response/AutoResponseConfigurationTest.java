package com.cartisan.web.response;

import com.cartisan.web.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AutoResponseConfiguration 测试。
 *
 * <p>验证自动响应包装功能的开关控制。</p>
 */
@DisplayName("AutoResponseConfiguration 测试")
class AutoResponseConfigurationTest {

    @Nested
    @DisplayName("默认禁用配置")
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestApplication.class
    )
    class DisabledByDefaultTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("默认情况下 - AutoResponseAdvice Bean 不应该注册")
        void shouldNotRegisterAutoResponseAdviceByDefault() {
            assertThat(context.containsBean("autoResponseAdvice")).isFalse();
        }
    }

    @Nested
    @DisplayName("启用配置")
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestApplication.class,
        properties = "cartisan.web.auto-response.enabled=true"
    )
    class EnabledWhenPropertySetTest {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("当属性设置为 true 时 - AutoResponseAdvice Bean 应该注册")
        void shouldRegisterAutoResponseAdviceWhenEnabled() {
            assertThat(context.containsBean("autoResponseAdvice")).isTrue();
            assertThat(context.getBean("autoResponseAdvice")).isInstanceOf(AutoResponseAdvice.class);
        }
    }
}
