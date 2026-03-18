package com.cartisan.security.config;

import cn.dev33.satoken.stp.StpUtil;
import com.cartisan.security.config.properties.CartisanSecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * cartisan-security 自动配置主类。
 * <p>
 * 零配置引入 cartisan-security 模块的入口类。
 * </p>
 *
 * <h3>条件装配</h3>
 * <ul>
 *   <li>Web 应用（@ConditionalOnWebApplication）</li>
 *   <li>classpath 存在 Sa-Token（@ConditionalOnClass(StpUtil.class)）</li>
 * </ul>
 *
 * <h3>配置项</h3>
 * <pre>
 * cartisan:
 *   security:
 *     interceptor:
 *       path-patterns: ["/**"]
 *       exclude-path-patterns: ["/error", "/actuator/**"]
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(StpUtil.class)
@EnableConfigurationProperties(CartisanSecurityProperties.class)
@Import({
    SecurityInterceptorConfig.class,
    CurrentUserArgumentResolverConfig.class
})
public class CartisanSecurityAutoConfiguration {
    // 主类只负责模块级条件和导入，具体配置由 SecurityInterceptorConfig 处理
}
