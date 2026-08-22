package com.cartisan.test.mvc;

import com.cartisan.web.config.BaseEnumConverter;
import com.cartisan.web.config.EnumErrorProperties;
import com.cartisan.web.config.JacksonConfiguration;
import com.cartisan.web.exception.GlobalExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link CartisanMvcTest} 切片装配。
 *
 * <p>{@code @WebMvcTest} 切片默认排除 cartisan-web 的自动配置，本类把切片验证
 * 所需的三件套补齐，与生产行为对齐：</p>
 * <ul>
 *   <li>{@link BaseEnumConverter} — 枚举 @RequestParam/@PathVariable 按 Integer code 绑定</li>
 *   <li>{@link JacksonConfiguration} — BaseEnum 响应序列化 / 请求反序列化为 Integer code</li>
 *   <li>{@link GlobalExceptionHandler} — 非法 code → 400 信封（含取值域表）</li>
 * </ul>
 *
 * <p>不引入生产自动配置中的其他组件（Filter、EnumController、ResubmitAspect 等）：
 * 切片只测指定 controller，且 ResubmitLock 依赖 StringRedisTemplate，
 * 整体拉回会破坏切片环境。</p>
 */
@Configuration(proxyBeanMethods = false)
@Import(JacksonConfiguration.class)
@EnableConfigurationProperties(EnumErrorProperties.class)
public class CartisanMvcTestConfiguration implements WebMvcConfigurer {

    /**
     * 注册 BaseEnum Converter Factory，与生产
     * {@code CartisanWebAutoConfiguration#addFormatters} 一致。
     *
     * @param registry Formatter 注册表
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new BaseEnumConverter());
    }

    /**
     * 注册全局异常处理器。
     *
     * @param enumErrorProperties 枚举错误业务码配置（支持切片内
     *        {@code cartisan.web.enum-error.codes.*} 覆盖）
     * @return GlobalExceptionHandler 实例
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler(EnumErrorProperties enumErrorProperties) {
        return new GlobalExceptionHandler(enumErrorProperties);
    }
}
