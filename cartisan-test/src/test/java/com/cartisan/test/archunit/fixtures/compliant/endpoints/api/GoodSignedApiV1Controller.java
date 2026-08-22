package com.cartisan.test.archunit.fixtures.compliant.endpoints.api;

import com.cartisan.openapi.annotation.RequireSignature;
import com.cartisan.web.doc.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合规的机机接口 Controller - @RequireSignature + @Tag + @Operation(summary) + @ErrorCodes
 */
@RequireSignature
@Tag(name = "库存同步 API")
@RestController
public class GoodSignedApiV1Controller {

    @Operation(summary = "推送库存变更")
    @ErrorCodes({"STK_001", "STK_002"})
    @GetMapping("/stock-changes")
    public String push() {
        return "OK";
    }
}
