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
 * String userName = user.getNickname();
 * TokenInfo tokenInfo = authService.login(loginId, userName);
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
     * 调用此方法前，业务层应已完成身份验证并获得 loginId；登录时本就持有用户名，随之一并传入即可，
     * 无需（也不应）直接依赖 Sa-Token。
     * </p>
     * <p>
     * 框架在建立会话后，将 {@code userName} 写入 Sa-Token Session 的 {@code "userName"} key，
     * {@code SecurityFilter} 在后续请求中自动读取并写入 {@code RequestContext}。
     * {@code userName} 为 {@code null} 时不写入（等价于不设置），为机器账号等无显示名的边缘场景留口子；
     * 非空值使后续请求的 {@code RequestContext.userName} 自动为所登录用户名。
     * {@code userName} 是不透明字符串，框架不解析其字段来源（nickname / realName / 账号均可）。
     * </p>
     *
     * @param loginId  用户标识（由业务层认证后提供）
     * @param userName 用户名（写入会话供 SecurityFilter 读取；为 null 则不写入）
     * @return Token 信息
     * @throws NullPointerException loginId 为 null
     */
    TokenInfo login(Long loginId, String userName);

    /**
     * 创建登录会话（自定义超时）。
     * <p>
     * 用于"记住我"等场景，如 7 天免登录。{@code userName} 的处理与
     * {@link #login(Long, String)} 一致。
     * </p>
     *
     * @param loginId        用户标识
     * @param timeoutSeconds 超时秒数（> 0）
     * @param userName       用户名（写入会话供 SecurityFilter 读取；为 null 则不写入）
     * @return Token 信息
     * @throws NullPointerException     loginId 为 null
     * @throws IllegalArgumentException timeoutSeconds <= 0
     */
    TokenInfo login(Long loginId, long timeoutSeconds, String userName);

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

    /**
     * 踢出指定用户（强制下线）。
     * <p>
     * 踢出后，用户的 Token 将失效，需要重新登录。
     * </p>
     *
     * @param loginId 用户标识
     * @since 0.3.0
     */
    default void kickout(Long loginId) {
        // 框架实现由 SaTokenAuthenticationService 提供
        throw new UnsupportedOperationException("Kickout not implemented");
    }

    /**
     * 根据用户名踢出用户。
     * <p>
     * 默认实现抛出 {@link UnsupportedOperationException}。
     * 业务层需要覆盖此方法，提供 username → loginId 映射。
     * </p>
     *
     * @param username 用户名
     * @since 0.3.0
     */
    default void kickoutByUsername(String username) {
        throw new UnsupportedOperationException(
            "Kickout by username not implemented. Override this method in your service.");
    }
}
