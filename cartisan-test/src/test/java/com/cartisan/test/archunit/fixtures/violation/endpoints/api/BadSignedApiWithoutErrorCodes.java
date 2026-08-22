package com.cartisan.test.archunit.fixtures.violation.endpoints.api;

import com.cartisan.openapi.annotation.RequireSignature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：方法级 @RequireSignature 的机机接口缺 @ErrorCodes
 * 违反规则：requireSignatureEndpointsShouldBeDocumented
 */
@Tag(name = "对账 API")
@RestController
public class BadSignedApiWithoutErrorCodes {

    @RequireSignature
    @Operation(summary = "拉取对账单")
    @GetMapping("/statements")
    public String statements() {
        return "OK";
    }
}
