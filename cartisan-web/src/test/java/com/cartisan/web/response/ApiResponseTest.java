package com.cartisan.web.response;

import com.cartisan.core.exception.BaseCodeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse 单元测试")
class ApiResponseTest {

    @Test
    @DisplayName("给定数据 - 调用 ok - 返回成功响应带数据")
    void given_data_when_ok_then_return_success_response_with_data() {
        ApiResponse<String> response = ApiResponse.ok("test data");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isEqualTo("test data");
        assertThat(response.requestId()).isNull();
    }

    @Test
    @DisplayName("给定无数据 - 调用 ok - 返回成功响应无数据")
    void given_noData_when_ok_then_return_success_response_without_data() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("Success");
        assertThat(response.data()).isNull();
        assertThat(response.requestId()).isNull();
    }

    @Test
    @DisplayName("给定 CodeMessage - 调用 error - 返回错误响应")
    void given_codeMessage_when_error_then_return_error_response() {
        ApiResponse<Void> response = ApiResponse.error(BaseCodeMessage.NOT_FOUND);

        assertThat(response.code()).isEqualTo(404);
        assertThat(response.message()).isEqualTo("Resource not found");
        assertThat(response.data()).isNull();
        assertThat(response.requestId()).isNull();
    }

    @Test
    @DisplayName("给定 CodeMessage 和参数 - 调用 error - 返回参数化错误响应")
    void given_codeMessage_and_args_when_error_then_return_parameterized_error_response() {
        ApiResponse<Void> response = ApiResponse.error(BaseCodeMessage.INVALID_PARAMETER, "email");

        assertThat(response.code()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("Invalid parameter: email");
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("给定 CodeMessage 和空参数 - 调用 error - 返回原始消息错误响应")
    void given_codeMessage_and_emptyArgs_when_error_then_return_error_response_with_original_message() {
        ApiResponse<Void> response = ApiResponse.error(BaseCodeMessage.INVALID_PARAMETER);

        assertThat(response.code()).isEqualTo(400);
        assertThat(response.message()).isEqualTo("Invalid parameter: {0}");
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("给定自定义码和消息 - 调用 error - 返回自定义错误响应")
    void given_customCode_and_message_when_error_then_return_custom_error_response() {
        ApiResponse<Void> response = ApiResponse.error(500, "Third party error");

        assertThat(response.code()).isEqualTo(500);
        assertThat(response.message()).isEqualTo("Third party error");
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("给定不同类型数据 - 调用 ok - 支持泛型类型推导")
    void given_differentTypeData_when_ok_then_support_generic_type_inference() {
        ApiResponse<String> stringResponse = ApiResponse.ok("text");
        ApiResponse<Integer> intResponse = ApiResponse.ok(42);

        assertThat(stringResponse.data()).isInstanceOf(String.class);
        assertThat(intResponse.data()).isInstanceOf(Integer.class);
    }
}
