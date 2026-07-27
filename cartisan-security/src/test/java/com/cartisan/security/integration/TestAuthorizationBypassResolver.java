package com.cartisan.security.integration;

import com.cartisan.security.authorization.AuthorizationBypassResolver;
import org.springframework.stereotype.Component;

/**
 * 测试用授权 bypass 解析器。
 *
 * <p>对固定超管 userId（{@link #SUPER_ADMIN_ID}）返回 {@code true}，其余返回 {@code false}。
 * 仅供集成测试使用，模拟消费应用声明"某个 loginId 可 bypass 授权检查"。</p>
 */
@Component
public class TestAuthorizationBypassResolver implements AuthorizationBypassResolver {

    /**
     * 测试用超管 userId。登录此 id 的用户将 bypass 所有授权检查。
     */
    public static final Long SUPER_ADMIN_ID = 999L;

    @Override
    public boolean shouldBypass(Long loginId) {
        return SUPER_ADMIN_ID.equals(loginId);
    }
}
