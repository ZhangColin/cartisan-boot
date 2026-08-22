package com.cartisan.web.config;

import com.cartisan.web.TestApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BaseEnum springdoc 契约集成测试（#21）。
 *
 * <p>起全上下文拉 {@code /v3/api-docs}，验证 springdoc 在 classpath 时
 * {@link BaseEnumModelConverter} 自动生效：BaseEnum 字段 {@code type=integer} +
 * code→名称对照，与运行时 Jackson 契约一致；普通枚举不受影响。</p>
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class BaseEnumModelConverterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_render_base_enum_field_as_integer_with_code_table() throws Exception {
        JsonNode status = schemaProperty(apiDocs(), "SpringDocOrderResponse", "status");

        assertThat(status.path("type").asText()).isEqualTo("integer");
        assertThat(status.has("enum")).isFalse();
        assertThat(status.path("description").asText())
                .contains("1=启用")
                .contains("0=禁用")
                .contains("2=待审核");
    }

    @Test
    void should_render_base_enum_inside_generic_wrapper_as_integer() throws Exception {
        JsonNode data = schemaProperty(apiDocs(), "ApiResponseSpringDocOrderResponse", "data");

        // 统一响应体经泛型解析后内嵌 $ref，目标记录的 BaseEnum 字段必为 integer
        assertThat(data.path("$ref").asText())
                .isEqualTo("#/components/schemas/SpringDocOrderResponse");
        assertThat(schemaProperty(apiDocs(), "SpringDocOrderResponse", "status")
                .path("type").asText()).isEqualTo("integer");
    }

    @Test
    void should_render_base_enum_collection_items_as_integer() throws Exception {
        JsonNode items = schemaProperty(apiDocs(), "SpringDocOrderResponse", "history")
                .path("items");

        assertThat(items.path("type").asText()).isEqualTo("integer");
        assertThat(items.path("description").asText()).contains("1=启用");
    }

    @Test
    void should_render_base_enum_request_param_as_integer() throws Exception {
        JsonNode parameters = apiDocs()
                .path("paths").path("/springdoc-test/status").path("get").path("parameters");

        JsonNode statusParam = findParameter(parameters, "status");
        assertThat(statusParam).as("应存在名为 status 的请求参数").isNotNull();
        assertThat(statusParam.path("schema").path("type").asText()).isEqualTo("integer");
        assertThat(statusParam.path("schema").path("description").asText()).contains("1=启用");
    }

    @Test
    void should_keep_plain_enum_schema_unchanged() throws Exception {
        JsonNode category = schemaProperty(apiDocs(), "SpringDocOrderResponse", "category");

        assertThat(category.path("type").asText()).isEqualTo("string");
        assertThat(category.path("enum")).extracting(JsonNode::asText)
                .containsExactly("PHYSICAL", "DIGITAL");
    }

    @Test
    void should_not_render_name_field_by_default() throws Exception {
        // enum-name-fields 默认关闭：schema 不合成 xxxName 属性
        JsonNode properties = apiDocs().path("components").path("schemas")
                .path("SpringDocOrderResponse").path("properties");

        assertThat(properties.has("statusName")).isFalse();
    }

    // ---------- 支撑 ----------

    private JsonNode apiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    /** components.schemas.{schema}.properties.{field}（缺失即为契约漂移，断言失败）。 */
    private static JsonNode schemaProperty(JsonNode doc, String schema, String field) {
        JsonNode property = doc.path("components").path("schemas").path(schema)
                .path("properties").path(field);
        assertThat(property.isMissingNode())
                .as("api-docs 应含 %s.%s（缺失视为契约漂移）", schema, field)
                .isFalse();
        return property;
    }

    private static JsonNode findParameter(JsonNode parameters, String name) {
        for (JsonNode parameter : parameters) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        return null;
    }
}
