package com.cartisan.test.context;

import com.cartisan.core.context.RequestContext;

/**
 * 测试工具：ScopedValue bind 辅助。
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * RequestContext ctx = RequestContextTestSupport.builder()
 *     .requestId("test-req")
 *     .userId(1L)
 *     .userName("test")
 *     .build();
 * RequestContextTestSupport.runWith(ctx, () -> {
 *     assertThat(RequestContext.getRequestId()).isEqualTo("test-req");
 * });
 * }</pre>
 */
public final class RequestContextTestSupport {

    private RequestContextTestSupport() {
    }

    /**
     * 在指定 RequestContext 作用域内执行测试逻辑。
     */
    public static void runWith(RequestContext context, Runnable test) {
        RequestContext.run(context, test);
    }

    /**
     * 创建 Builder 快速构建测试 RequestContext。
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String requestId;
        private String clientIp;
        private String callerAppId;
        private String callerAppName;
        private Long userId;
        private String userName;
        private Long tenantId;
        private String tenantName;

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder callerAppId(String callerAppId) {
            this.callerAppId = callerAppId;
            return this;
        }

        public Builder callerAppName(String callerAppName) {
            this.callerAppName = callerAppName;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder tenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder tenantName(String tenantName) {
            this.tenantName = tenantName;
            return this;
        }

        public RequestContext build() {
            return new RequestContext(requestId, clientIp, callerAppId, callerAppName,
                    userId, userName, tenantId, tenantName);
        }
    }
}
