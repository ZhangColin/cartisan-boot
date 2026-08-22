package com.cartisan.web.doc;

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
 * @ErrorCodes 渲染的端到端契约测试（#24）。
 *
 * <p>起全上下文拉 {@code /v3/api-docs}，验证 {@link ErrorCodeOperationCustomizer}
 * 自动生效：声明的错误码渲染进端点 description，已有手写描述保留在前，
 * 未声明的端点不受影响。启动成功本身即证明 {@link ErrorCodesValidator}
 * 校验通过（fixture 的 code 全部可解析）。</p>
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class ErrorCodeDocIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void should_append_error_code_block_after_hand_written_description() throws Exception {
        String description = operationDescription(apiDocs(), "/errorcode-test/doc");

        assertThat(description).startsWith("手写描述保留在前");
        assertThat(description).contains("""
                错误码：
                - 404 DOC_001 — 文档不存在
                - 409 DOC_002 — 文档被锁定""");
    }

    @Test
    void should_render_error_code_block_as_sole_description_when_absent() throws Exception {
        String description = operationDescription(apiDocs(), "/errorcode-test/bare");

        assertThat(description).isEqualTo("""
                错误码：
                - 404 DOC_001 — 文档不存在""");
    }

    @Test
    void should_not_touch_endpoints_without_error_codes() throws Exception {
        JsonNode operation = apiDocs()
                .path("paths").path("/errorcode-test/plain").path("get");

        assertThat(operation.path("summary").asText()).isEqualTo("无错误码声明的对照端点");
        assertThat(operation.has("description")).isFalse();
    }

    // ---------- 支撑 ----------

    private JsonNode apiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static String operationDescription(JsonNode doc, String path) {
        JsonNode description = doc.path("paths").path(path).path("get").path("description");
        assertThat(description.isMissingNode())
                .as("api-docs 端点 %s 应有 description（缺失视为契约漂移）", path)
                .isFalse();
        return description.asText();
    }
}
