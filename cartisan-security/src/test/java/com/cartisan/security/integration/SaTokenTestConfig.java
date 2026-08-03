package com.cartisan.security.integration;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Sa-Token 测试配置。
 * <p>
 * 使用 {@link SaTokenContextForSpringInJakartaServlet} 作为 Sa-Token 上下文实现，
 * 通过 Spring 的 {@code RequestContextHolder} 读取当前请求，与 MockMvc 兼容。
 * </p>
 * <p>
 * 提供内存版 {@link SaTokenDao}，覆盖 classpath 上 sa-token-redis-jackson 的 Redis 实现，
 * 确保测试无需 Redis 即可运行。
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

    /**
     * 内存版 SaTokenDao，通过 @Primary 覆盖 sa-token-redis-jackson 自动配置的 Redis DAO。
     * <p>
     * {@link SaTokenDaoDefaultImpl} 是 Sa-Token 内置的默认实现（基于内存 Map），
     * 测试环境无需持久化 token，使用内存版即可。
     * </p>
     */
    @Bean
    @Primary
    public SaTokenDao saTokenDao() {
        return new SaTokenDaoDefaultImpl();
    }
}
