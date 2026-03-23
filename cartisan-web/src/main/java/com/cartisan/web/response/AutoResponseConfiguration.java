package com.cartisan.web.response;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 自动响应包装配置。
 *
 * <p>控制是否自动将 Controller 返回值包装为 {@link ApiResponse}。</p>
 *
 * <h3>配置项</h3>
 * <ul>
 *   <li>{@code cartisan.web.auto-response.enabled} - 是否启用，默认 false</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // application.yml
 * cartisan:
 *   web:
 *     auto-response:
 *       enabled: true
 * </pre>
 *
 * @since 0.3.0
 */
@Configuration
@ConfigurationProperties("cartisan.web.auto-response")
public class AutoResponseConfiguration {

    /**
     * 是否启用自动响应包装。
     * <p>默认为 false，避免意外影响现有项目。</p>
     */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
