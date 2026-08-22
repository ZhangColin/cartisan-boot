package com.cartisan.test.contract;

import com.cartisan.web.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 契约测试基类验证用 Controller：BaseEnum 字段 + 普通枚举字段，泛型包装返回。
 */
@RestController
@RequestMapping("/contract-orders")
public class ContractOrderController {

    /** 未实现 BaseEnum 的普通枚举（schema 必须保持 string+name）。 */
    public enum ContractChannel {
        ONLINE, OFFLINE
    }

    /**
     * 测试响应体。
     */
    public record ContractOrderResponse(
            Long id,
            ContractOrderStatus status,
            ContractChannel channel) {
    }

    @GetMapping("/{id}")
    public ApiResponse<ContractOrderResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(new ContractOrderResponse(id, ContractOrderStatus.PAID, ContractChannel.ONLINE));
    }
}
