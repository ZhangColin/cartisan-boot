package com.cartisan.core.context;

import java.util.concurrent.Callable;

/**
 * 统一请求上下文，基于 ScopedValue 存储当前请求的所有上下文信息。
 *
 * <p>兼容 Virtual Threads，所有字段不可变，只能通过 Filter 写入。</p>
 *
 * <p>字段：</p>
 * <ul>
 *   <li>requestId — 请求追踪 ID</li>
 *   <li>clientIp — 客户端 IP</li>
 *   <li>callerAppId / callerAppName — 调用方应用（服务间通信）</li>
 *   <li>userId / userName — 当前用户</li>
 *   <li>tenantId / tenantName — 当前租户</li>
 * </ul>
 */
public record RequestContext(
        String requestId,
        String clientIp,
        String callerAppId,
        String callerAppName,
        Long userId,
        String userName,
        Long tenantId,
        String tenantName
) {

    /**
     * ScopedValue 键，持有当前线程的上下文实例。
     */
    public static final ScopedValue<RequestContext> CONTEXT = ScopedValue.newInstance();

    // ---- 静态 getter 方法 ----

    public static String getRequestId() {
        RequestContext ctx = CONTEXT.orElse(null);
        return ctx == null ? null : ctx.requestId();
    }

    public static String getClientIp() {
        RequestContext ctx = CONTEXT.orElse(null);
        return ctx == null ? null : ctx.clientIp();
    }

    public static String getCallerAppId() {
        RequestContext ctx = CONTEXT.orElse(null);
        return ctx == null ? null : ctx.callerAppId();
    }

    public static String getCallerAppName() {
        RequestContext ctx = CONTEXT.orElse(null);
        return ctx == null ? null : ctx.callerAppName();
    }

    public static Long getUserId() {
        RequestContext ctx = CONTEXT.orElse(null);
        return ctx == null ? null : ctx.userId();
    }

    public static String getUserName() {
        RequestContext ctx = CONTEXT.orElse(null);
        return ctx == null ? null : ctx.userName();
    }

    public static Long getTenantId() {
        RequestContext ctx = CONTEXT.orElse(null);
        return ctx == null ? null : ctx.tenantId();
    }

    public static String getTenantName() {
        RequestContext ctx = CONTEXT.orElse(null);
        return ctx == null ? null : ctx.tenantName();
    }

    // ---- withXxx 方法，返回新实例 ----

    /**
     * 返回新的 RequestContext，设置调用方信息，其余字段不变。
     */
    public RequestContext withCaller(String appId, String appName) {
        return new RequestContext(requestId, clientIp, appId, appName, userId, userName, tenantId, tenantName);
    }

    /**
     * 返回新的 RequestContext，设置用户信息，其余字段不变。
     */
    public RequestContext withUser(Long newUserId, String newUserName) {
        return new RequestContext(requestId, clientIp, callerAppId, callerAppName, newUserId, newUserName, tenantId, tenantName);
    }

    /**
     * 返回新的 RequestContext，设置租户信息，其余字段不变。
     */
    public RequestContext withTenant(Long newTenantId, String newTenantName) {
        return new RequestContext(requestId, clientIp, callerAppId, callerAppName, userId, userName, newTenantId, newTenantName);
    }

    // ---- ScopedValue 操作 ----

    /**
     * 在指定上下文中执行 Runnable，执行完毕后自动清理。
     */
    public static void run(RequestContext context, Runnable runnable) {
        ScopedValue.where(CONTEXT, context).run(runnable);
    }

    /**
     * 在指定上下文中执行 Callable，执行完毕后自动清理，返回结果。
     */
    public static <T> T runFor(RequestContext context, Callable<T> callable) throws Exception {
        return ScopedValue.where(CONTEXT, context).call(callable);
    }
}
