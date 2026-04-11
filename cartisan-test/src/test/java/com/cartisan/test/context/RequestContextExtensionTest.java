package com.cartisan.test.context;

import com.cartisan.core.context.RequestContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextExtensionTest {

    @Nested
    @WithRequestContext(userId = 1L, userName = "test-user", tenantId = 100L, tenantName = "TestTenant")
    class ShouldBindAtClassLevel {

        @Test
        void shouldHaveRequestId() {
            assertThat(RequestContext.getRequestId()).isEqualTo("test-request-id");
        }

        @Test
        void shouldHaveClientIp() {
            assertThat(RequestContext.getClientIp()).isEqualTo("127.0.0.1");
        }

        @Test
        void shouldHaveUserId() {
            assertThat(RequestContext.getUserId()).isEqualTo(1L);
        }

        @Test
        void shouldHaveUserName() {
            assertThat(RequestContext.getUserName()).isEqualTo("test-user");
        }

        @Test
        void shouldHaveTenantId() {
            assertThat(RequestContext.getTenantId()).isEqualTo(100L);
        }

        @Test
        void shouldHaveTenantName() {
            assertThat(RequestContext.getTenantName()).isEqualTo("TestTenant");
        }
    }

    @Nested
    class ShouldBindAtMethodLevel {

        @Test
        @WithRequestContext(userId = 42L, userName = "method-user")
        void shouldUseMethodAnnotation() {
            assertThat(RequestContext.getUserId()).isEqualTo(42L);
            assertThat(RequestContext.getUserName()).isEqualTo("method-user");
        }
    }

    @Nested
    class ShouldSupportBuilder {

        @Test
        void shouldBuildWithContext() {
            RequestContext ctx = RequestContextTestSupport.builder()
                    .requestId("custom-req")
                    .userId(99L)
                    .userName("builder-user")
                    .tenantId(200L)
                    .build();

            RequestContextTestSupport.runWith(ctx, () -> {
                assertThat(RequestContext.getRequestId()).isEqualTo("custom-req");
                assertThat(RequestContext.getUserId()).isEqualTo(99L);
                assertThat(RequestContext.getUserName()).isEqualTo("builder-user");
                assertThat(RequestContext.getTenantId()).isEqualTo(200L);
            });

            // After runWith scope, context is gone
            assertThat(RequestContext.getRequestId()).isNull();
        }
    }
}
