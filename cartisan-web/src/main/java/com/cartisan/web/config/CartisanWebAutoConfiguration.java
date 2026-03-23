package com.cartisan.web.config;

import com.cartisan.web.context.RequestContextFilter;
import com.cartisan.web.exception.GlobalExceptionHandler;
import com.cartisan.web.filter.RequestLogFilter;
import com.cartisan.web.response.AutoResponseConfiguration;
import com.cartisan.web.resubmit.PreventResubmit;
import com.cartisan.web.resubmit.ResubmitAspect;
import com.cartisan.web.resubmit.ResubmitLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * cartisan-web 模块的 Spring Boot 自动配置。
 *
 * <p>注册 Web 层核心组件：
 * <ul>
 *   <li>{@link RequestContextFilter} — 请求上下文初始化</li>
 *   <li>{@link RequestLogFilter} — 请求日志记录</li>
 *   <li>{@link GlobalExceptionHandler} — 全局异常处理</li>
 *   <li>{@link com.cartisan.web.response.AutoResponseAdvice} — 自动响应包装（可选）</li>
 *   <li>{@link ResubmitAspect} — 防重复提交切面（当 Redis 可用时）</li>
 * </ul>
 *
 * <p><strong>引入即用</strong>：添加 cartisan-web 依赖后，无需 {@code @ComponentScan}，
 * 这些组件会自动注册。</p>
 *
 * <h3>条件装配</h3>
 * <p>仅在 Web 应用环境中生效（非 Web 应用如批处理不需要这些组件）。</p>
 *
 * <h3>用户覆盖</h3>
 * <p>核心组件强制注册，不使用 {@code @ConditionalOnMissingBean}。
 * 用户需要替换时，通过排除 AutoConfiguration 或显式注册自定义 Bean 处理。</p>
 *
 * @since 0.2.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
@Import(AutoResponseConfiguration.class)
public class CartisanWebAutoConfiguration {

    /**
     * 注册请求上下文 Filter。
     *
     * <p>Bean 名称使用 {@code cartisanRequestContextFilter}，与之前 {@code @Component} 注解时的名称一致，
     * 保持向后兼容。</p>
     *
     * @return RequestContextFilter 实例
     */
    @Bean("cartisanRequestContextFilter")
    public RequestContextFilter requestContextFilter() {
        return new RequestContextFilter();
    }

    /**
     * 注册请求日志记录 Filter。
     *
     * <p>依赖 {@link RequestContextFilter}，需要在请求上下文初始化之后执行。
     * 设置 Order 值为 {@code Ordered.HIGHEST_PRECEDENCE + 1}，确保在 RequestContextFilter 之后。</p>
     *
     * @return RequestLogFilter 实例
     */
    @Bean
    public RequestLogFilter requestLogFilter() {
        return new RequestLogFilter();
    }

    /**
     * 注册全局异常处理器。
     *
     * <p>{@link GlobalExceptionHandler} 类本身保留 {@code @ControllerAdvice} 注解，
     * 这是 Spring MVC 识别异常处理器的必要注解。</p>
     *
     * @return GlobalExceptionHandler 实例
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 注册防重复提交锁。
     *
     * <p>仅在 Redis 可用时注册。</p>
     *
     * @param redisTemplate Redis 模板
     * @return ResubmitLock 实例
     */
    @Bean
    @ConditionalOnClass(PreventResubmit.class)
    @ConditionalOnMissingBean
    public ResubmitLock resubmitLock(StringRedisTemplate redisTemplate) {
        return new ResubmitLock(redisTemplate);
    }

    /**
     * 注册防重复提交切面。
     *
     * <p>仅在 ResubmitLock 可用时注册。</p>
     *
     * @param resubmitLock 防重复提交锁
     * @return ResubmitAspect 实例
     */
    @Bean
    @ConditionalOnClass(PreventResubmit.class)
    @ConditionalOnMissingBean
    public ResubmitAspect resubmitAspect(ResubmitLock resubmitLock) {
        return new ResubmitAspect(resubmitLock);
    }
}
