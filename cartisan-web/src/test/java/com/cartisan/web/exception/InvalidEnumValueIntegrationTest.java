package com.cartisan.web.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BaseEnum 非法取值错误信封集成测试（#22）。
 *
 * <p>覆盖 query/path 与 JSON body 两条绑定路径的自描述 400 信封、
 * 非枚举类型不匹配的原行为回归、业务码覆盖点。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BaseEnum 非法取值错误信封集成测试")
class InvalidEnumValueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("JSON body 路径")
    class BodyPath {

        @Test
        @DisplayName("body 内 BaseEnum 字段非法 code → 400，message 含字段名与取值表")
        void shouldReturn400WithCodeTable_whenBodyCodeIsInvalid() throws Exception {
            mockMvc.perform(post("/test/enum/request-body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": 999}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(
                            "status 取值 999 非法，合法取值：1=启用, 0=禁用, 2=待审核"));
        }

        @Test
        @DisplayName("body 内 BaseEnum 字段非数字字符串 → 400，message 含字段名与取值表")
        void shouldReturn400WithCodeTable_whenBodyValueIsNotNumber() throws Exception {
            mockMvc.perform(post("/test/enum/request-body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"ACTIVE\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value(
                            "status 取值 ACTIVE 非法，合法取值：1=启用, 0=禁用, 2=待审核"));
        }

        @Test
        @DisplayName("body 内合法 code 正常绑定")
        void shouldBindValidBodyCode() throws Exception {
            mockMvc.perform(post("/test/enum/request-body")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": 1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value("Status: ACTIVE (code=1)"));
        }
    }

    @Nested
    @DisplayName("非枚举类型不匹配（回归）")
    class NonEnumTypeMismatch {

        @Test
        @DisplayName("path 变量应为 Long 传了 abc → 仍返回 404")
        void shouldReturn404_whenLongPathVariableIsNotNumber() throws Exception {
            mockMvc.perform(get("/test/enum/long-path/abc"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.message").value("Resource not found"));
        }
    }
}
