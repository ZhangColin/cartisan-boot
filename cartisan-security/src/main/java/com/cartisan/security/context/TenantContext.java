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
}
