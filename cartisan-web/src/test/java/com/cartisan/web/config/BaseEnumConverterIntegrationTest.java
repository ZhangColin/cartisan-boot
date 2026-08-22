package com.cartisan.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BaseEnumConverter 集成测试")
class BaseEnumConverterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("应该将 @RequestParam Integer code 转换为枚举")
    void shouldConvertRequestParam() throws Exception {
        mockMvc.perform(get("/test/enum/request-param")
                .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Status: ACTIVE (code=1)"));
    }

    @Test
    @DisplayName("应该将 @PathVariable Integer code 转换为枚举")
    void shouldConvertPathVariable() throws Exception {
        mockMvc.perform(get("/test/enum/path-variable/0"))
                .andExpect(status().isOk())
                .andExpect(content().string("Status: DISABLED (code=0)"));
    }

    @Test
    @DisplayName("应该处理可选参数为 null 的情况")
    void shouldHandleNullOptionalParam() throws Exception {
        mockMvc.perform(get("/test/enum/optional"))
                .andExpect(status().isOk())
                .andExpect(content().string("Status is null"));
    }

    @Test
    @DisplayName("当传入无效 code 时应该返回 400，message 含参数名、非法值与取值表")
    void shouldReturn400WithCodeTable_whenCodeIsInvalid() throws Exception {
        mockMvc.perform(get("/test/enum/request-param")
                .param("status", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        "status 取值 999 非法，合法取值：1=启用, 0=禁用, 2=待审核"));
    }

    @Test
    @DisplayName("当传入非数字字符串时应该返回 400，message 同样含取值表")
    void shouldReturn400WithCodeTable_whenInputIsNotNumber() throws Exception {
        mockMvc.perform(get("/test/enum/request-param")
                .param("status", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        "status 取值 invalid 非法，合法取值：1=启用, 0=禁用, 2=待审核"));
    }

    @Test
    @DisplayName("path variable 枚举非法取值同样返回 400 与取值表")
    void shouldReturn400WithCodeTable_whenPathVariableIsInvalid() throws Exception {
        mockMvc.perform(get("/test/enum/path-variable/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        "status 取值 999 非法，合法取值：1=启用, 0=禁用, 2=待审核"));
    }
}