package com.cartisan.web.exception;

import com.cartisan.web.TestController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler 集成测试。
 * <p>
 * 使用 MockMvc 验证各种异常映射到正确的 HTTP 状态码和响应格式。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GlobalExceptionHandler 集成测试")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // ========== AC1: CartisanException 映射 ==========

    @Test
    @DisplayName("给定 CartisanException(400) - 验证返回 400 和正确响应格式")
    void shouldReturn400WithCorrectFormat_whenCartisanException400() throws Exception {
        mockMvc.perform(get("/test/cartisan-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("给定 CartisanException(500) - 验证返回 500 和正确响应格式")
    void shouldReturn500WithCorrectFormat_whenCartisanException500() throws Exception {
        mockMvc.perform(get("/test/cartisan-exception-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").exists());
    }

    // ========== AC2: 参数校验失败返回字段级错误 ==========

    @Test
    @DisplayName("给定 MethodArgumentNotValidException - 验证返回 400 和 errors 数组")
    void shouldReturn400WithErrorsArray_whenMethodArgumentNotValid() throws Exception {
        String invalidJson = "{\"email\":\"invalid\",\"password\":\"123\"}";

        mockMvc.perform(post("/test/validate-request-body")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Parameter validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").exists())
                .andExpect(jsonPath("$.errors[0].message").exists())
                .andExpect(jsonPath("$.errors[0].errorCode").exists());
    }

    @Test
    @DisplayName("给定 ConstraintViolationException - 验证返回 400 和 errors 数组")
    void shouldReturn400WithErrorsArray_whenConstraintViolation() throws Exception {
        mockMvc.perform(get("/test/validate-request-param")
                        .param("email", "invalid-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Parameter validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    // ========== AC4: HttpRequestMethodNotSupportedException 返回 405 ==========

    @Test
    @DisplayName("给定 HttpRequestMethodNotSupportedException - 验证返回 405")
    void shouldReturn405_whenHttpMethodNotSupported() throws Exception {
        mockMvc.perform(post("/test/method-not-allowed"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    // ========== AC5: HttpMediaTypeNotSupportedException 返回 415 ==========

    @Test
    @DisplayName("给定 HttpMediaTypeNotSupportedException - 验证返回 415")
    void shouldReturn415_whenHttpMediaTypeNotSupported() throws Exception {
        mockMvc.perform(post("/test/media-type-not-supported")
                        .contentType("application/json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(415));
    }

    // ========== AC6: HttpMessageNotReadableException 返回 400 ==========

    @Test
    @DisplayName("给定 HttpMessageNotReadableException - 验证返回 400")
    void shouldReturn400_whenHttpMessageNotReadable() throws Exception {
        String malformedJson = "{\"email\": invalid";

        mockMvc.perform(post("/test/malformed-json")
                        .contentType("application/json")
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    // ========== AC7: MissingServletRequestParameterException 返回 400 ==========

    @Test
    @DisplayName("给定 MissingServletRequestParameterException - 验证返回 400 和参数名")
    void shouldReturn400WithParameterName_whenMissingParameter() throws Exception {
        mockMvc.perform(get("/test/missing-parameter"))  // 缺少 required 参数
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Missing required parameter: required"));
    }

    // ========== AC8: MissingRequestHeaderException 返回 400 ==========

    @Test
    @DisplayName("给定 MissingRequestHeaderException - 验证返回 400 和请求头名")
    void shouldReturn400WithHeaderName_whenMissingHeader() throws Exception {
        mockMvc.perform(get("/test/missing-header"))  // 缺少 required 请求头
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Missing required header: required"));
    }

    // ========== AC9: 兜底 Exception 返回 500 ==========

    @Test
    @DisplayName("给定未捕获 Exception - 验证返回 500")
    void shouldReturn500_whenUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    // ========== AC10: DuplicateKeyException 返回 409（非 500）==========

    @Test
    @DisplayName("DuplicateKeyException - 验证返回 409 和 Resource conflict")
    void shouldReturn409_whenDuplicateKeyException() throws Exception {
        mockMvc.perform(get("/test/duplicate-key"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Resource conflict"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ========== AC11: DataIntegrityViolationException 返回 400（非 500）==========

    @Test
    @DisplayName("DataIntegrityViolationException - 验证返回 400 和 Invalid request")
    void shouldReturn400_whenDataIntegrityViolationException() throws Exception {
        mockMvc.perform(get("/test/data-integrity-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
