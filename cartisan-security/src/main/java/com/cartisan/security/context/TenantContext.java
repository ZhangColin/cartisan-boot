package com.cartisan.security.context;

/**
 * 多租户上下文工具类。
 * <p>
 * 提供当前请求租户 ID 的存取访问，支持从 Header 或 Session 解析租户信息。
 * 使用 ScopedValue 实现，兼容 Virtual Threads。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 判断是否有租户上下文
 * if (TenantContext.hasTenant()) {
 *     Long tenantId = TenantContext.getCurrentTenantId();
 *     // 使用 tenantId...
 * }
 *
 * // 必须有租户的场景
 * Long tenantId = TenantContext.requireTenant();
 * // 使用 tenantId...
 *
 * // 可选租户的场景
 * Long tenantId = TenantContext.getCurrentTenantId();
 * if (tenantId != null) {
 *     // 使用 tenantId...
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
public final class TenantContext {

    /**
     * 租户 ID 的 ScopedValue 键。
     * <p>
     * 使用 ScopedValue 而非 ThreadLocal，确保在 Virtual Threads 环境下
     * 租户上下文能正确传递给子任务。
     * </p>
     */
    static final ScopedValue<Long> TENANT_ID = ScopedValue.newInstance();

    /**
     * 防止实例化。
     */
    private TenantContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取当前租户 ID。
     * <p>
     * 若当前请求未绑定租户上下文，返回 {@code null}。
     * </p>
     *
     * @return 租户 ID，未设置时返回 {@code null}
     */
    public static Long getCurrentTenantId() {
        // ScopedValue.get() 在未绑定时抛 NoSuchElementException
        // 使用 isBound() 检查后再 get()
        if (!TENANT_ID.isBound()) {
            return null;
        }
        return TENANT_ID.get();
    }

    /**
     * 判断当前请求是否有租户上下文。
     *
     * @return 有租户返回 {@code true}，否则返回 {@code false}
     */
    public static boolean hasTenant() {
        return getCurrentTenantId() != null;
    }

    /**
     * 获取当前租户 ID，若不存在则抛出异常。
     * <p>
     * 用于"必须有租户"的业务场景。
     * </p>
     *
     * @return 租户 ID
     * @throws IllegalStateException 当前无租户上下文
     */
    public static Long requireTenant() {
        Long tenantId = getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available");
        }
        return tenantId;
    }

    /**
     * 在指定租户上下文中执行任务。
     * <p>
     * 该方法为 package-private，仅供 TenantContextFilter 调用。
     * </p>
     *
     * @param tenantId 租户 ID，可为 null
     * @param runnable 要执行的任务
     */
    static void runWithTenant(Long tenantId, Runnable runnable) {
        if (tenantId != null) {
            ScopedValue.where(TENANT_ID, tenantId).run(runnable);
        } else {
            runnable.run();
        }
    }
}
