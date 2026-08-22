package com.cartisan.test.archunit.fixtures.violation.endpoints.api;

import com.cartisan.openapi.annotation.RequireSignature;
import com.cartisan.web.doc.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：类级 @RequireSignature 的机机接口 handler 缺 @Operation(summary)
 * 违反规则：requireSignatureEndpointsShouldBeDocumented
 */
@RequireSignature
@Tag(name = "订单回调 API")
@RestController
public class BadSignedApiWithoutSummary {

    @ErrorCodes("ORD_001")
    @PostMapping("/callback")
    public String callback() {
        return "OK";
    }
}
