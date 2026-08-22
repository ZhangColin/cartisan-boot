package com.cartisan.test.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpringDocEnumContractTestBase 端到端验证（消费服务视角）。
 *
 * <p>最小消费服务形态（cartisan-web + springdoc）继承基类：
 * 契约主体自动执行，另抽查代表性字段防「无枚举出现」的空转通过。</p>
 */
class SpringDocEnumContractTestBaseTest extends SpringDocEnumContractTestBase {

    @Override
    protected String basePackage() {
        return "com.cartisan.test.contract";
    }

    @Test
    void should_render_representative_base_enum_field_as_integer() throws Exception {
        JsonNode doc = fetchApiDocs("/v3/api-docs");

        assertThat(enumFieldType(doc, "ContractOrderResponse", "status")).isEqualTo("integer");
    }
}
