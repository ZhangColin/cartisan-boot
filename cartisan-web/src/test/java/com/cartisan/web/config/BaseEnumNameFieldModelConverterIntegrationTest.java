package com.cartisan.web.config;

import com.cartisan.web.TestApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BaseEnum 展示名 schema 同步的集成测试（opt-in 开启）。
 *
 * <p>起全上下文并开启 {@code cartisan.web.enum-name-fields.enabled}，拉
 * {@code /v3/api-docs} 验证 schema 与运行时 JSON 两跳同步：BaseEnum 属性
 * 获得 {@code xxxName} string 属性且紧跟 code 属性之后；普通枚举与集合不同步。</p>
 */
@SpringBootTest(classes = TestApplication.class,
        properties = "cartisan.web.enum-name-fields.enabled=true")
@AutoConfigureMockMvc
// springdoc 把 ModelConverter 注册进 JVM 级静态单例且上下文缓存不主动关闭——
// 本类结束后立刻关闭上下文（触发 converter 的 destroy 注销），
// 避免 enabled=true 的 converter 泄漏到同套件默认关闭的集成测试
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BaseEnumNameFieldModelConverterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_render_name_field_immediately_after_code_field() throws Exception {
        JsonNode properties = orderResponseProperties();

        JsonNode statusName = properties.path("statusName");
        assertThat(statusName.isMissingNode()).as("api-docs 应含 statusName 属性").isFalse();
        assertThat(statusName.path("type").asText()).isEqualTo("string");

        List<String> fieldOrder = new ArrayList<>();
        properties.fieldNames().forEachRemaining(fieldOrder::add);
        assertThat(fieldOrder).containsSubsequence("status", "statusName");
    }

    @Test
    void should_not_render_name_field_for_plain_enum_or_collection() throws Exception {
        JsonNode properties = orderResponseProperties();

        assertThat(properties.has("categoryName")).isFalse();
        assertThat(properties.has("historyName")).isFalse();
    }

    // ---------- 支撑 ----------

    private JsonNode orderResponseProperties() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode properties = objectMapper.readTree(body)
                .path("components").path("schemas")
                .path("SpringDocOrderResponse").path("properties");
        assertThat(properties.isMissingNode())
                .as("api-docs 应含 SpringDocOrderResponse schema").isFalse();
        return properties;
    }
}
