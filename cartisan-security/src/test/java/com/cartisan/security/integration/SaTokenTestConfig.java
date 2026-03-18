package com.cartisan.security.integration;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Sa-Token 测试配置。
 * <p>
 * 使用 {@link SaTokenContextForSpringInJakartaServlet} 作为 Sa-Token 上下文实现，
 * 通过 Spring 的 {@code RequestContextHolder} 读取当前请求，与 MockMvc 兼容。
 * </p>
 */
@TestConfiguration
public class SaTokenTestConfig {

    /**
     * 初始化 Sa-Token 上下文的辅助方法。
     * <p>
     * 在测试开始前调用，确保 Sa-Token 上下文可用。
     * 使用 {@code SaTokenContextForSpringInJakartaServlet} 以便 MockMvc 测试中
     * {@code StpUtil.isLogin()} 能通过 {@code RequestContextHolder} 读取到请求。
     * </p>
     */
    public static void initSaTokenContext() {
        if (SaManager.getSaTokenContext() == null) {
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        }
    }
}
