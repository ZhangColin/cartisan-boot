package com.cartisan.security.integration;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Sa-Token 测试配置。
 */
@TestConfiguration
public class SaTokenTestConfig {

    static {
        // 确保 Sa-Token 已初始化
        if (SaManager.getSaTokenContext() == null) {
            SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        }
    }

    /**
     * 初始化 Sa-Token 上下文的辅助方法。
     * <p>
     * 在测试开始前调用，确保 Sa-Token 上下文可用。
     * </p>
     */
    public static void initSaTokenContext() {
        SaTokenContext context = SaManager.getSaTokenContext();
        if (context == null) {
            SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        }
    }
}
