package com.cartisan.web.exception;

import org.junit.jupiter.api.DisplayName;
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
 * BaseEnum 非法取值的业务码覆盖点集成测试（#22）。
 *
 * <p>通过 {@code cartisan.web.enum-error.codes.<枚举类名>} 将默认 400 映射为业务码，
 * HTTP 状态保持 400。</p>
 */
@SpringBootTest(properties = "cartisan.web.enum-error.codes.TestUserStatus=1014")
@AutoConfigureMockMvc
@DisplayName("BaseEnum 非法取值业务码覆盖集成测试")
class InvalidEnumValueBusinessCodeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("query 路径配置映射命中时响应 code 为业务码，HTTP 仍 400")
    void shouldReturnBusinessCode_whenQueryMappingConfigured() throws Exception {
        mockMvc.perform(get("/test/enum/request-param")
                        .param("status", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1014))
                .andExpect(jsonPath("$.message").value(
                        "status 取值 999 非法，合法取值：1=启用, 0=禁用, 2=待审核"));
    }

    @Test
    @DisplayName("body 路径同样应用业务码覆盖")
    void shouldReturnBusinessCode_whenBodyMappingConfigured() throws Exception {
        mockMvc.perform(post("/test/enum/request-body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": 999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1014));
    }

    @Test
    @DisplayName("非法值不会命中映射时（其他参数场景）不影响正常请求")
    void shouldNotAffectValidRequests() throws Exception {
        mockMvc.perform(get("/test/enum/request-param")
                        .param("status", "1"))
                .andExpect(status().isOk());
    }
}
