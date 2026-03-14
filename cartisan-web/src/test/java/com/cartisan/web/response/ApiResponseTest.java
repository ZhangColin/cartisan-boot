package com.cartisan.web.response;

import com.cartisan.core.exception.BaseCodeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse 单元测试")
class ApiResponseTest {

    @Test
    @DisplayName("应该构造成功响应 - 带数据")
    void should_construct_success_response_with_data() {
        ApiResponse<String> response = ApiResponse.ok("test data");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isEqualTo("test data");
        assertThat(response.requestId()).isNull();
    }

    @Test
    @DisplayName("应该构造成功响应 - 无数据")
    void should_construct_success_response_without_data() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isNull();
        assertThat(response.requestId()).isNull();
    }

    @Test
    @DisplayName("应该构造错误响应 - 使用 CodeMessage")
    void should_construct_error_response_with_codeMessage() {
        ApiResponse<Void> response = ApiResponse.error(BaseCodeMessage.NOT_FOUND);

        assertThat(response.code()).isEqualTo(404);
        assertThat(response.message()).isEqualTo("Resource not found");
        assertThat(response.data()).isNull();
        assertThat(response.requestId()).isNull();
    }

    @Test
    @DisplayName("应该构造错误响应 - 参数化消息")
    void should_construct_error_response_with_parameterized_message() {
        ApiResponse<Void> response = ApiResponse.error(BaseCodeMessage.INVALID_PARAMETER, "email");

        assertThat(response.code()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("Invalid parameter: email");
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("应该构造错误响应 - 空参数使用原消息")
    void should_construct_error_response_with_empty_args() {
        ApiResponse<Void> response = ApiResponse.error(BaseCodeMessage.INVALID_PARAMETER);

        assertThat(response.code()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("Invalid parameter: {0}");
    }

    @Test
    @DisplayName("应该构造错误响应 - 自定义码和消息")
    void should_construct_error_response_with_custom_code_and_message() {
        ApiResponse<Void> response = ApiResponse.error(500, "Third party error");

        assertThat(response.code()).isEqualTo(500);
        assertThat(response.message()).isEqualTo("Third party error");
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("应该支持泛型类型推导")
    void should_support_generic_type_inference() {
        ApiResponse<String> stringResponse = ApiResponse.ok("text");
        ApiResponse<Integer> intResponse = ApiResponse.ok(42);

        assertThat(stringResponse.data()).isInstanceOf(String.class);
        assertThat(intResponse.data()).isInstanceOf(Integer.class);
    }
}
