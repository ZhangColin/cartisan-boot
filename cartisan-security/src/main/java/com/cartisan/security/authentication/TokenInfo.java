package com.cartisan.security.authentication;

import java.time.Instant;
import java.util.Objects;

/**
 * Token 信息。
 * <p>
 * 封装登录会话的 Token 信息，包括 token 值、用户标识和过期时间。
 * </p>
 *
 * @param token      Token 值
 * @param loginId    用户标识
 * @param expireTime 过期时间
 * @since 0.3.0
 */
public record TokenInfo(
    String token,
    Long loginId,
    Instant expireTime
) {
    /**
     * Compact constructor，校验参数非空。
     */
    public TokenInfo {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(loginId, "loginId");
        Objects.requireNonNull(expireTime, "expireTime");
    }
}
