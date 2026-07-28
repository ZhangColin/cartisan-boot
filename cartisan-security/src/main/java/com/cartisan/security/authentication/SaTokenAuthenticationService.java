package com.cartisan.security.authentication;

import cn.dev33.satoken.stp.StpUtil;

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
public class SaTokenAuthenticationService implements AuthenticationService {

    @Override
    public TokenInfo login(Long loginId, String userName) {
        Objects.requireNonNull(loginId, "loginId");

        // 创建会话
        StpUtil.login(loginId);
        recordUserName(userName);

        // 获取 Token 信息
        String token = StpUtil.getTokenValue();
        long timeoutSeconds = StpUtil.getTokenTimeout();
        Instant expireTime = Instant.now().plusSeconds(timeoutSeconds);

        return new TokenInfo(token, loginId, expireTime);
    }

    @Override
    public TokenInfo login(Long loginId, long timeoutSeconds, String userName) {
        Objects.requireNonNull(loginId, "loginId");

        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be positive: " + timeoutSeconds);
        }

        StpUtil.login(loginId, timeoutSeconds);
        recordUserName(userName);

        String token = StpUtil.getTokenValue();
        Instant expireTime = Instant.now().plusSeconds(timeoutSeconds);

        return new TokenInfo(token, loginId, expireTime);
    }

    /**
     * 将 userName 写入 Sa-Token Session 的 "userName" key（SecurityFilter 从此 key 读取）。
     * <p>
     * userName 为 null 时不写入，等价于不设置，为机器账号等无显示名场景留口子。
     * </p>
     */
    private void recordUserName(String userName) {
        if (userName != null) {
            StpUtil.getSession().set("userName", userName);
        }
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

    @Override
    public void kickout(Long loginId) {
        Objects.requireNonNull(loginId, "loginId");
        StpUtil.kickout(loginId);
    }
}
