package com.cartisan.web.controller;

import com.cartisan.web.enums.EnumRegistry;
import com.cartisan.web.response.ApiResponse;
import com.cartisan.web.response.EnumOption;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 枚举选项 Controller（默认实现）。
 *
 * @since 0.9.0
 */
@RestController
@RequestMapping("${cartisan.web.enum-controller.path:/api/enums}")
public class EnumController extends EnumControllerBase {

    public EnumController(EnumRegistry enumRegistry) {
        super(enumRegistry);
    }

    @GetMapping("/{enumName}")
    public ApiResponse<List<EnumOption>> getEnum(@PathVariable String enumName) {
        return ApiResponse.ok(enumRegistry.getEnumOptions(enumName));
    }

    @PostMapping("/batch")
    public ApiResponse<Map<String, List<EnumOption>>> batchEnums(
            @RequestBody @Valid EnumBatchRequest request) {
        Map<String, List<EnumOption>> result = super.batchEnums(request.enums());
        return ApiResponse.ok(result);
    }
}