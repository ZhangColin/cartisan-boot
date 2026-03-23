package com.cartisan.web.response;

import com.cartisan.web.TestApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AutoResponseAdvice 集成测试。
 * <p>
 * 使用 MockMvc 验证自动响应包装功能。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestApplication.class,
        properties = "cartisan.web.auto-response.enabled=true"
)
@AutoConfigureMockMvc
@DisplayName("AutoResponseAdvice 集成测试")
class AutoResponseAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("字符串响应包装")
    class StringResponseWrappingTest {

        @Test
        @DisplayName("应该包装字符串响应为 ApiResponse")
        void shouldWrapStringResponse() throws Exception {
            mockMvc.perform(get("/test/string-response"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Success"))
                    .andExpect(jsonPath("$.data").value("test string"));
        }
    }

    @Nested
    @DisplayName("对象响应包装")
    class ObjectResponseWrappingTest {

        @Test
        @DisplayName("应该包装对象响应为 ApiResponse")
        void shouldWrapObjectResponse() throws Exception {
            mockMvc.perform(get("/test/object-response"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Success"))
                    .andExpect(jsonPath("$.data.name").value("test"))
                    .andExpect(jsonPath("$.data.value").value(123));
        }
    }

    @Nested
    @DisplayName("ApiResponse 不重复包装")
    class ApiResponseNoDoubleWrappingTest {

        @Test
        @DisplayName("不应该重复包装 ApiResponse 类型")
        void shouldNotWrapApiResponse() throws Exception {
            mockMvc.perform(get("/test/api-response"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Already wrapped"))
                    .andExpect(jsonPath("$.data").value("original data"));
        }
    }

    @Nested
    @DisplayName("空值处理")
    class NullResponseHandlingTest {

        @Test
        @DisplayName("应该处理 null 响应")
        void shouldHandleNullResponse() throws Exception {
            mockMvc.perform(get("/test/null-response"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Success"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
}
