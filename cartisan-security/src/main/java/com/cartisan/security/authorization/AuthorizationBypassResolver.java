package com.cartisan.security.authorization;

/**
 * 授权 bypass 解析器。
 *
 * <p>消费应用实现此接口，声明哪些 loginId 应跳过 {@code @RequireRole} / {@code @RequirePermission}
 * 授权检查（但仍须通过 {@code @RequireAuth} 登录认证）。框架的 {@code SecurityInterceptor} 在确认
 * 用户登录之后、检查角色/权限之前，询问此 resolver；命中则跳过授权检查。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Bean
 * public AuthorizationBypassResolver superAdminBypassResolver(
 *         AdminUserPermissionAppService appService) {
 *     return loginId -> appService.isSuperAdmin(loginId);
 * }
 * }</pre>
 *
 * <p>不提供此 bean 的应用，行为完全不变（向后兼容）。bypass 判定标准（"谁是超管"等）完全由应用决定，
 * 框架不特化任何业务角色概念。</p>
 *
 * <p>注意：此 resolver 在每个鉴权 HTTP 请求都会被调用一次，框架不缓存结果。若判定逻辑涉及数据库查询，
 * 应用应自行缓存（对齐 Sa-Token {@code getPermissionList} 的 session 缓存模式）。</p>
 *
 * @since 0.1.0
 */
public interface AuthorizationBypassResolver {

    /**
     * 判断给定 loginId 是否跳过授权检查。
     *
     * @param loginId 当前登录用户 ID
     * @return {@code true} 表示跳过 {@code @RequireRole} / {@code @RequirePermission} 检查
     */
    boolean shouldBypass(Long loginId);
}
