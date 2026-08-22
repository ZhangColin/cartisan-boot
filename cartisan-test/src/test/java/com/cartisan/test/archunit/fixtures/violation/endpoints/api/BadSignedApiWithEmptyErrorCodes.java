package com.cartisan.test.archunit.fixtures.violation.endpoints.api;

import com.cartisan.openapi.annotation.RequireSignature;
import com.cartisan.web.doc.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规：机机接口 @ErrorCodes 声明为空数组
 * 违反规则：requireSignatureEndpointsShouldBeDocumented
 */
@RequireSignature
@Tag(name = "汇率 API")
@RestController
public class BadSignedApiWithEmptyErrorCodes {

    @Operation(summary = "查汇率")
    @ErrorCodes({})
    @GetMapping("/rates")
    public String rates() {
        return "OK";
    }
}
