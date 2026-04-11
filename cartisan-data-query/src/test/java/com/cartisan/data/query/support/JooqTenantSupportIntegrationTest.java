package com.cartisan.data.query.support;

import com.cartisan.core.context.RequestContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JooqTenantSupport 集成测试（有租户上下文）")
class JooqTenantSupportIntegrationTest {

    private static final Long TEST_TENANT_ID = 123L;

    @Test
    @DisplayName("给定有租户上下文 - 调用 eqTenantId - 返回租户等值条件")
    void shouldReturnTenantEqCondition_whenTenantContextExists() {
        var resultHolder = new Object() { Condition condition; };

        RequestContext ctx = new RequestContext(null, null, null, null, null, null, TEST_TENANT_ID, null);
        RequestContext.run(ctx, () -> {
            var tenantIdField = DSL.field("tenant_id", Long.class);
            resultHolder.condition = JooqTenantSupport.eqTenantId(tenantIdField);
        });

        assertThat(resultHolder.condition).isNotNull();
        assertThat(resultHolder.condition).isNotEqualTo(DSL.noCondition());
    }

    @Test
    @DisplayName("给定无租户上下文 - 调用 eqTenantId - 返回 noCondition")
    void shouldReturnNoCondition_whenNoTenantContext() {
        var resultHolder = new Object() { Condition condition; };

        // No RequestContext bound, should return noCondition
        var tenantIdField = DSL.field("tenant_id", Long.class);
        resultHolder.condition = JooqTenantSupport.eqTenantId(tenantIdField);

        assertThat(resultHolder.condition).isEqualTo(DSL.noCondition());
    }
}
