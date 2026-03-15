package com.cartisan.security.context;

/**
 * 租户上下文测试工具类。
 *
 * <p>提供在集成测试中设置租户上下文的便捷方法。此类仅用于测试，
 * 业务代码不应依赖。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * import static com.cartisan.security.context.TenantTestSupport.runWithTenant;
 *
 * // 设置租户并执行操作
 * runWithTenant(100L, () -> {
 *     // 在此代码块内，TenantContext.getCurrentTenantId() 返回 100L
 *     service.doSomething();
 * });
 * // 执行结束后，租户上下文自动清除
 * }</pre>
 *
 * <p><strong>注意：</strong>此类与 {@link TenantContext} 位于同一包，
 * 以访问 package-private 的 {@code runWithTenant} 方法。</p>
 *
 * @since 0.3.0
 */
public final class TenantTestSupport {

    /**
     * 防止实例化。
     *
     * @throws UnsupportedOperationException 始终抛出，表示工具类不可实例化
     */
    private TenantTestSupport() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 在指定租户上下文中执行任务。
     *
     * <p>执行 {@code runnable} 时，{@link TenantContext#getCurrentTenantId()}
     * 返回指定的 {@code tenantId}。执行结束后，租户上下文自动清除。</p>
     *
     * @param tenantId 租户 ID，可为 null
     * @param runnable 要执行的任务
     * @throws NullPointerException 若 {@code runnable} 为 null
     */
    public static void runWithTenant(Long tenantId, Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable cannot be null");
        }
        // 委托给 TenantContext 的 package-private 方法
        TenantContext.runWithTenant(tenantId, runnable);
    }
}
