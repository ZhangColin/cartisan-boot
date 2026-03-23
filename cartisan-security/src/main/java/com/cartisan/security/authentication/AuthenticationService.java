package com.cartisan.security.authentication;

import java.util.Optional;

/**
 * 认证服务接口。
 * <p>
 * 提供登录会话管理抽象，包括创建会话、销毁会话、查询会话信息。
 * 业务代码通过此接口管理会话，不直接依赖 Sa-Token。
 * </p>
 *
 * <h3>职责边界</h3>
 * <ul>
 *   <li>框架实现：login()、logout()、getTokenInfo() — 会话管理</li>
 *   <li>业务层实现：authenticate() — 身份验证</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 业务层实现认证
 * @Service
 * public class AuthServiceImpl implements AuthenticationService {
 *     @Override
 *     public Long authenticate(String username, String password) {
 *         User user = userRepository.findByUsername(username);
 *         if (!passwordEncoder.matches(password, user.getPassword())) {
 *             throw new DomainException("密码错误");
 *         }
 *         return user.getId();
 *     }
 * }
 *
 * // 使用示例
 * Long loginId = authService.authenticate("admin", "123");
 * TokenInfo tokenInfo = authService.login(loginId);
 * }</pre>
 *
 * @since 0.3.0
 */
public interface AuthenticationService {

    /**
     * 认证用户身份。
     * <p>
     * 默认实现抛出 {@link UnsupportedOperationException}，提示业务层实现。
     * 业务层应覆盖此方法，实现具体的认证逻辑（如密码验证、LDAP、OAuth等）。
     * </p>
     *
     * @param username 用户名
     * @param password 密码
     * @return 认证成功返回用户标识（loginId）
     * @throws UnsupportedOperationException 使用默认实现时抛出
     * @throws RuntimeException 业务层实现时可能抛出的认证失败异常
     */
    default Long authenticate(String username, String password) {
        throw new UnsupportedOperationException(
            "Authentication not implemented. Override this method in your service.");
    }

    /**
     * 创建登录会话。
     * <p>
     * 调用此方法前，业务层应已完成身份验证并获得 loginId。
     * </p>
     *
     * @param loginId 用户标识（由业务层认证后提供）
     * @return Token 信息
     * @throws NullPointerException loginId 为 null
     */
    TokenInfo login(Long loginId);

    /**
     * 创建登录会话（自定义超时）。
     * <p>
     * 用于"记住我"等场景，如 7 天免登录。
     * </p>
     *
     * @param loginId        用户标识
     * @param timeoutSeconds 超时秒数（> 0）
     * @return Token 信息
     * @throws NullPointerException     loginId 为 null
     * @throws IllegalArgumentException timeoutSeconds <= 0
     */
    TokenInfo login(Long loginId, long timeoutSeconds);

    /**
     * 销毁当前登录会话。
     * <p>
     * 未登录时静默处理，不抛异常。
     * </p>
     */
    void logout();

    /**
     * 获取当前 Token 信息。
     *
     * @return Token 信息，未登录返回 {@code null}
     */
    TokenInfo getTokenInfo();

    /**
     * 获取当前用户 ID。
     *
     * @return 用户 ID，未登录返回 {@link Optional#empty()}
     */
    default Optional<Long> getCurrentUserId() {
        return Optional.ofNullable(getTokenInfo()).map(TokenInfo::loginId);
    }
}
