package com.cartisan.web.doc;

import com.cartisan.core.exception.CodeMessage;
import com.cartisan.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ErrorCodes 渲染的 api-docs 集成测试用 Controller。
 *
 * <p>两个端点：带手写 description 的（验证块追加其后）与不带的（验证块独立成文）；
 * 附一个无 @ErrorCodes 的对照端点。错误码枚举用独立 DOC_ 前缀，避免与模块内
 * 其他测试枚举的 code 冲突（CodeMessageRegistry 启动扫描会全量登记）。</p>
 */
@RestController
@RequestMapping("/errorcode-test")
public class ErrorCodeTestController {

    /** 集成测试用业务错误码枚举。 */
    public enum DocMessage implements CodeMessage {
        DOC_NOT_FOUND(404, "DOC_001", "文档不存在"),
        DOC_LOCKED(409, "DOC_002", "文档被锁定");

        private final int httpStatus;
        private final String code;
        private final String message;

        DocMessage(int httpStatus, String code, String message) {
            this.httpStatus = httpStatus;
            this.code = code;
            this.message = message;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public String message() {
            return message;
        }

        @Override
        public int httpStatus() {
            return httpStatus;
        }
    }

    @Operation(summary = "查文档", description = "手写描述保留在前")
    @ErrorCodes({"DOC_001", "DOC_002"})
    @GetMapping("/doc")
    public ApiResponse<Void> doc() {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "无手写描述的端点")
    @ErrorCodes("DOC_001")
    @GetMapping("/bare")
    public ApiResponse<Void> bare() {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "无错误码声明的对照端点")
    @GetMapping("/plain")
    public ApiResponse<Void> plain() {
        return ApiResponse.ok(null);
    }
}
