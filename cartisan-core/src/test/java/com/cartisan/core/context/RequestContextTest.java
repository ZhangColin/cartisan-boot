package com.cartisan.core.context;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextTest {

    @Nested
    class ShouldBindAndGetFields {
        @Test
        void shouldReturnAllFieldsWhenBound() {
            RequestContext ctx = new RequestContext(
                    "req-1", "10.0.0.1",
                    "app-1", "AppOne",
                    42L, "Alice",
                    100L, "TenantX");

            ScopedValue.where(RequestContext.CONTEXT, ctx).run(() -> {
                assertThat(RequestContext.getRequestId()).isEqualTo("req-1");
                assertThat(RequestContext.getClientIp()).isEqualTo("10.0.0.1");
                assertThat(RequestContext.getCallerAppId()).isEqualTo("app-1");
                assertThat(RequestContext.getCallerAppName()).isEqualTo("AppOne");
                assertThat(RequestContext.getUserId()).isEqualTo(42L);
                assertThat(RequestContext.getUserName()).isEqualTo("Alice");
                assertThat(RequestContext.getTenantId()).isEqualTo(100L);
                assertThat(RequestContext.getTenantName()).isEqualTo("TenantX");
            });
        }

        @Test
        void shouldReturnNullWhenNotBound() {
            assertThat(RequestContext.getRequestId()).isNull();
            assertThat(RequestContext.getClientIp()).isNull();
            assertThat(RequestContext.getCallerAppId()).isNull();
            assertThat(RequestContext.getCallerAppName()).isNull();
            assertThat(RequestContext.getUserId()).isNull();
            assertThat(RequestContext.getUserName()).isNull();
            assertThat(RequestContext.getTenantId()).isNull();
            assertThat(RequestContext.getTenantName()).isNull();
        }
    }

    @Nested
    class ShouldCreateWithDerivedContext {
        @Test
        void shouldReturnNewInstanceWithCaller() {
            RequestContext original = new RequestContext(
                    "req-1", "10.0.0.1",
                    null, null,
                    null, null,
                    null, null);

            RequestContext derived = original.withCaller("app-2", "AppTwo");

            assertThat(derived.requestId()).isEqualTo("req-1");
            assertThat(derived.clientIp()).isEqualTo("10.0.0.1");
            assertThat(derived.callerAppId()).isEqualTo("app-2");
            assertThat(derived.callerAppName()).isEqualTo("AppTwo");
            assertThat(derived.userId()).isNull();
            assertThat(derived.tenantId()).isNull();
            // original unchanged
            assertThat(original.callerAppId()).isNull();
        }

        @Test
        void shouldReturnNewInstanceWithUser() {
            RequestContext original = new RequestContext(
                    "req-1", "10.0.0.1",
                    "app-1", "AppOne",
                    null, null,
                    null, null);

            RequestContext derived = original.withUser(99L, "Bob");

            assertThat(derived.requestId()).isEqualTo("req-1");
            assertThat(derived.callerAppId()).isEqualTo("app-1");
            assertThat(derived.userId()).isEqualTo(99L);
            assertThat(derived.userName()).isEqualTo("Bob");
            assertThat(derived.tenantId()).isNull();
            assertThat(original.userId()).isNull();
        }

        @Test
        void shouldReturnNewInstanceWithTenant() {
            RequestContext original = new RequestContext(
                    "req-1", "10.0.0.1",
                    "app-1", "AppOne",
                    1L, "Alice",
                    null, null);

            RequestContext derived = original.withTenant(200L, "TenantY");

            assertThat(derived.requestId()).isEqualTo("req-1");
            assertThat(derived.userId()).isEqualTo(1L);
            assertThat(derived.tenantId()).isEqualTo(200L);
            assertThat(derived.tenantName()).isEqualTo("TenantY");
            assertThat(original.tenantId()).isNull();
        }
    }

    @Nested
    class ShouldRunInScope {
        @Test
        void shouldRunRunnableWithContext() {
            RequestContext ctx = new RequestContext(
                    "req-1", "10.0.0.1",
                    null, null, null, null, null, null);

            RequestContext.run(ctx, () -> {
                assertThat(RequestContext.getRequestId()).isEqualTo("req-1");
            });

            assertThat(RequestContext.getRequestId()).isNull();
        }
    }
}
