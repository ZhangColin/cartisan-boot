package com.cartisan.web;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.CartisanException;
import com.cartisan.core.exception.DomainException;
import com.cartisan.web.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;


/**
 * 测试用 Controller，用于触发各种异常以验证 GlobalExceptionHandler。
 */
@Validated
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/cartisan-exception")
    public void throwCartisanException() {
        throw new DomainException(BaseCodeMessage.BAD_REQUEST);
    }

    @GetMapping("/cartisan-exception-500")
    public void throwCartisanException500() {
        throw new DomainException(BaseCodeMessage.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/validate-request-body")
    public void validateRequestBody(@Valid @RequestBody TestRequest request) {
        // Spring 会先校验，校验失败抛出 MethodArgumentNotValidException
    }

    @GetMapping("/validate-request-param")
    public void validateRequestParam(@RequestParam @Email String email) {
        // Spring 会先校验，校验失败抛出 ConstraintViolationException
    }

    @GetMapping("/missing-parameter")
    public void missingParameter(@RequestParam String required) {
        // 缺少参数会抛出 MissingServletRequestParameterException
    }

    @GetMapping("/missing-header")
    public void missingHeader(@RequestHeader String required) {
        // 缺少请求头会抛出 MissingRequestHeaderException
    }

    @PostMapping("/malformed-json")
    public void malformedJson(@RequestBody Object body) {
        // JSON 解析失败会抛出 HttpMessageNotReadableException
    }

    @GetMapping("/method-not-allowed")
    public void methodNotAllowed() {
        // 只支持 GET，POST 会触发 HttpRequestMethodNotSupportedException
    }

    @PostMapping(path = "/media-type-not-supported", consumes = "application/xml")
    public void mediaTypeNotSupported() {
        // 只接受 XML，发送 JSON 会触发 HttpMediaTypeNotSupportedException
    }


    @GetMapping("/exception")
    public void throwException() {
        throw new RuntimeException("Unexpected error");
    }

    /**
     * 测试请求体。
     */
    record TestRequest(
            @Email String email,
            @Size(min = 8, max = 20) String password
    ) {}

    // ========== 防重复提交测试端点 ==========

    @PostMapping("/resubmit")
    @com.cartisan.web.resubmit.PreventResubmit(delaySeconds = 10, prefix = "createUser")
    public ApiResponse<String> resubmit(@RequestBody TestRequest request) {
        return ApiResponse.ok("Request processed");
    }

    @PostMapping("/resubmit-short-delay")
    @com.cartisan.web.resubmit.PreventResubmit(delaySeconds = 1, prefix = "shortDelay")
    public ApiResponse<String> resubmitWithShortDelay(@RequestBody TestRequest request) {
        return ApiResponse.ok("Request processed");
    }

    @PostMapping("/resubmit-no-prefix")
    @com.cartisan.web.resubmit.PreventResubmit(delaySeconds = 5)
    public ApiResponse<String> resubmitWithoutPrefix(@RequestBody TestRequest request) {
        return ApiResponse.ok("Request processed");
    }
}
