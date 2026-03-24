package com.cartisan.security.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * cartisan-security 拦截器配置属性。
 * <p>
 * 支持配置拦截器生效的路径模式和排除路径模式。
 * </p>
 *
 * @see com.cartisan.security.config.CartisanSecurityAutoConfiguration
 */
@ConfigurationProperties(prefix = "cartisan.security.interceptor")
public class CartisanSecurityProperties {

    /**
     * 拦截器生效的路径模式（Ant 风格）。
     * <p>
     * 默认拦截所有路径 {@code /**}，由拦截器内部根据注解决定是否鉴权。
     * </p>
     */
    private List<String> pathPatterns = new ArrayList<>(List.of("/**"));

    /**
     * 排除的路径模式（Ant 风格）。
     * <p>
     * 默认排除错误页和 Actuator 端点，避免对系统路径做无意义拦截。
     * </p>
     */
    private List<String> excludePathPatterns = new ArrayList<>(List.of(
        "/error",
        "/actuator/**"
    ));

    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    public void setPathPatterns(List<String> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    public List<String> getExcludePathPatterns() {
        return excludePathPatterns;
    }

    public void setExcludePathPatterns(List<String> excludePathPatterns) {
        this.excludePathPatterns = excludePathPatterns;
    }
}
