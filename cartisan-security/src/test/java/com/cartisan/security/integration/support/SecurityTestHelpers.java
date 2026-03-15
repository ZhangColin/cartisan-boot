package com.cartisan.security.integration.support;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 集成测试工具类。
 * <p>
 * 提供登录、带租户 Header 请求等辅助方法。
 * </p>
 */
public final class SecurityTestHelpers {

    private SecurityTestHelpers() {
        // 工具类，禁止实例化
    }

    /**
     * 执行已登录用户的 MockMvc 请求。
     * <p>
     * 请求执行后自动登出，确保测试间隔离。
     * </p>
     *
     * @param mvc    MockMvc 实例
     * @param userId 登录用户 ID
     * @param requestBuilder 请求构建器
     * @return ResultActions
     * @throws Exception MockMvc 执行异常
     */
    public static ResultActions performAsUser(MockMvc mvc, Long userId,
            RequestBuilder requestBuilder) throws Exception {
        StpUtil.login(userId);
        try {
            return mvc.perform(requestBuilder);
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 为请求添加租户 Header。
     * <p>
     * 如果 tenantId 为 null，则不添加 Header。
     * </p>
     *
     * @param builder  请求构建器
     * @param tenantId 租户 ID
     * @return 带 X-Tenant-Id Header 的新请求构建器
     */
    public static MockHttpServletRequestBuilder withTenantHeader(
            MockHttpServletRequestBuilder builder, Long tenantId) {
        if (tenantId != null) {
            return builder.header("X-Tenant-Id", tenantId.toString());
        }
        return builder;
    }

    /**
     * 登录并在 Session 中设置租户 ID。
     * <p>
     * 用于测试 TenantContext 从 Session 读取租户的场景。
     * </p>
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     */
    public static void loginWithTenant(Long userId, Long tenantId) {
        StpUtil.login(userId);
        if (tenantId != null) {
            StpUtil.getSession().set("tenantId", tenantId.toString());
        }
    }

    /**
     * 清理登录状态。
     * <p>
     * 用于测试前置条件清理，确保每个测试从干净状态开始。
     * 如果 Sa-Token 上下文未初始化，则跳过清理。
     * </p>
     */
    public static void cleanup() {
        try {
            if (StpUtil.isLogin()) {
                StpUtil.logout();
            }
        } catch (cn.dev33.satoken.exception.SaTokenContextException e) {
            // 上下文未初始化，跳过清理
        }
    }
}
