package com.cartisan.security.integration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * 集成测试专用启动类。
 * <p>
 * 仅扫描测试包，模拟真实业务项目使用方式（不扫描 com.cartisan.security.*）。
 * 所有安全组件由 CartisanSecurityAutoConfiguration 自动配置声明。
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.cartisan.security.integration")
@Import(SaTokenTestConfig.class)
public class IntegrationTestApplication {
    // 无需额外配置，依赖 Spring Boot 自动装配
}
