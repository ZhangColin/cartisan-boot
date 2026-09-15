package com.cartisan.web.request;

import com.cartisan.web.TestApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link Pagination} 的 springdoc 自文档契约测试（#29）。
 *
 * <p>起全上下文拉 {@code /v3/api-docs}，验证 record 组件渲染为
 * page/size/sort 顶级 query 参数——前端联调无需翻代码。</p>
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@DisplayName("Pagination springdoc 自文档集成测试")
class PaginationSpringDocIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Pagination 组件渲染为 page/size/sort 顶级 query 参数")
    void shouldRenderComponentsAsTopLevelQueryParameters() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode parameters = objectMapper.readTree(body)
                .path("paths").path("/springdoc-test/pagination").path("get").path("parameters");

        assertThat(parameter(parameters, "page").path("in").asText()).isEqualTo("query");
        assertThat(parameter(parameters, "size").path("in").asText()).isEqualTo("query");
        assertThat(parameter(parameters, "sort").path("in").asText()).isEqualTo("query");
        // 组件 schema 自描述类型，不渲染为 request body
        assertThat(parameter(parameters, "page").path("schema").path("type").asText())
                .isEqualTo("integer");
        assertThat(parameter(parameters, "size").path("schema").path("type").asText())
                .isEqualTo("integer");
    }

    private static JsonNode parameter(JsonNode parameters, String name) {
        assertThat(parameters.isArray()).as("api-docs 应渲染 parameters 数组").isTrue();
        for (JsonNode parameter : parameters) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("query 参数 " + name + " 未渲染进 api-docs（契约漂移）: " + parameters);
    }
}
