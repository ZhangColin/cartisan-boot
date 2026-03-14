package com.cartisan.security.authentication;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * 基于 Sa-Token 的认证服务实现。
 * <p>
 * 封装 Sa-Token 的会话管理能力，提供 login/logout/getTokenInfo 方法。
 * </p>
 *
 * @since 0.3.0
 */
@Service
public class SaTokenAuthenticationService implements AuthenticationService {

    @Override
    public TokenInfo login(Long loginId) {
        Objects.requireNonNull(loginId, "loginId");

        // 创建会话
        StpUtil.login(loginId);

        // 获取 Token 信息
        String token = StpUtil.getTokenValue();
        long timeoutSeconds = StpUtil.getTokenTimeout();
        Instant expireTime = Instant.now().plusSeconds(timeoutSeconds);

        return new TokenInfo(token, loginId, expireTime);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public TokenInfo getTokenInfo() {
        if (!StpUtil.isLogin()) {
            return null;
        }

        String token = StpUtil.getTokenValue();
        Long loginId = StpUtil.getLoginIdAsLong();
        long timeoutSeconds = StpUtil.getTokenTimeout();
        Instant expireTime = Instant.now().plusSeconds(timeoutSeconds);

        return new TokenInfo(token, loginId, expireTime);
    }
}
