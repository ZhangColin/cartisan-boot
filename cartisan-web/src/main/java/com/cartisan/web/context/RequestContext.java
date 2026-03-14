package com.cartisan.web.context;

/**
 * 请求上下文，基于 ThreadLocal 存储当前请求的追踪信息。
 *
 * <p>设计特点：</p>
 * <ul>
 *   <li>不可变：只能通过 Filter 设置，业务代码只读</li>
 *   <li>线程隔离：ThreadLocal 确保并发安全</li>
 *   <li>容错设计：各字段可能为 null（初始化失败时）</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * String requestId = RequestContext.getRequestId();
 * if (requestId != null) {
 *     log.info("Processing request: {}", requestId);
 * }
 * }</pre>
 *
 * <p>前置条件：无（get 方法可安全调用）</p>
 * <p>后置条件：get 方法可能返回 null（调用方需检查）</p>
 */
public final class RequestContext {

    /**
     * ThreadLocal 持有当前线程的上下文实例。
     */
    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    /**
     * 请求追踪 ID，可能为 null。
     */
    private final String requestId;

    /**
     * 客户端 IP 地址，可能为 null。
     */
    private final String clientIp;

    /**
     * 私有构造函数，防止外部实例化。
     *
     * @param requestId 请求追踪 ID，可为 null
     * @param clientIp 客户端 IP，可为 null
     */
    private RequestContext(String requestId, String clientIp) {
        this.requestId = requestId;
        this.clientIp = clientIp;
    }

    /**
     * 获取当前请求的 requestId。
     *
     * <p>后置条件：</p>
     * <ul>
     *   <li>若 RequestContext 已初始化，返回初始化时的 requestId</li>
     *   <li>若 RequestContext 未初始化，返回 null</li>
     *   <li>若初始化时 requestId 为 null（失败场景），返回 null</li>
     * </ul>
     *
     * @return 当前请求的 requestId，可能为 null
     */
    public static String getRequestId() {
        RequestContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.requestId;
    }

    /**
     * 获取当前请求的 clientIp。
     *
     * <p>后置条件：</p>
     * <ul>
     *   <li>若 RequestContext 已初始化，返回初始化时的 clientIp</li>
     *   <li>若 RequestContext 未初始化，返回 null</li>
     *   <li>若初始化时 clientIp 为 null（失败场景），返回 null</li>
     * </ul>
     *
     * @return 当前请求的 clientIp，可能为 null
     */
    public static String getClientIp() {
        RequestContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.clientIp;
    }

    /**
     * 初始化上下文（包级可见，仅 Filter 调用）。
     *
     * <p>前置条件：无</p>
     * <p>后置条件：创建新的 RequestContext 实例并存入 ThreadLocal</p>
     *
     * @param requestId 请求追踪 ID，可为 null
     * @param clientIp 客户端 IP，可为 null
     */
    static void init(String requestId, String clientIp) {
        CONTEXT.set(new RequestContext(requestId, clientIp));
    }

    /**
     * 清理上下文（包级可见，仅 Filter 调用）。
     *
     * <p>前置条件：无</p>
     * <p>后置条件：从 ThreadLocal 移除 RequestContext 实例</p>
     */
    static void clear() {
        CONTEXT.remove();
    }
}
