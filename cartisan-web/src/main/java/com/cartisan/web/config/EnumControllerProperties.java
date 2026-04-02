package com.cartisan.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 枚举 Controller 配置属性。
 *
 * @since 0.9.0
 */
@Configuration
@ConfigurationProperties(prefix = "cartisan.web.enum-controller")
public class EnumControllerProperties {
    private boolean enabled = true;
    private String path = "/api/enums";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}