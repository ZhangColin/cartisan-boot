package com.cartisan.web.config;

import com.cartisan.web.TestApplication;
import com.cartisan.web.context.RequestContextFilter;
import com.cartisan.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CartisanWebAutoConfiguration 集成测试。
 *
 * <p>验证：添加 cartisan-web 依赖后，组件通过 AutoConfiguration 自动注册。</p>
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class
)
class CartisanWebAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 验证 RequestContextFilter Bean 已注册。
     */
    @Test
    void should_register_requestContextFilter() {
        assertThat(applicationContext.getBean("cartisanRequestContextFilter"))
            .isNotNull();
        assertThat(applicationContext.getBean("cartisanRequestContextFilter"))
            .isInstanceOf(RequestContextFilter.class);
    }

    /**
     * 验证 GlobalExceptionHandler Bean 已注册。
     */
    @Test
    void should_register_globalExceptionHandler() {
        assertThat(applicationContext.getBean(GlobalExceptionHandler.class))
            .isNotNull();
    }
}
