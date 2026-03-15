package com.cartisan.security.integration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * 集成测试专用启动类。
 * <p>
 * 扫描 cartisan-security 模块的所有组件 + 测试用 Controller，
 * 确保拦截器、Filter、异常处理等完整加载。
 * </p>
 */
@SpringBootApplication(scanBasePackages = {
    "com.cartisan.security",              // 模块内所有组件
    "com.cartisan.security.integration"   // 测试用 Controller
})
@Import(SaTokenTestConfig.class)
public class IntegrationTestApplication {
    // 无需额外配置，依赖 Spring Boot 自动装配
}
