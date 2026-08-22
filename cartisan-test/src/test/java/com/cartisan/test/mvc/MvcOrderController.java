package com.cartisan.test.mvc;

import com.cartisan.core.context.RequestContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code @CartisanMvcTest} 验证用 Controller。
 *
 * <p>覆盖三种枚举入口（query / path / body）与 RequestContext 叠加场景。</p>
 */
@RestController
@RequestMapping("/test/orders")
public class MvcOrderController {

    @GetMapping("/query")
    public MvcOrderResponse query(@RequestParam MvcOrderStatus status) {
        return new MvcOrderResponse("ORD-001", status);
    }

    @GetMapping("/{status}")
    public MvcOrderResponse path(@PathVariable MvcOrderStatus status) {
        return new MvcOrderResponse("ORD-002", status);
    }

    @PostMapping
    public MvcOrderResponse create(@RequestBody MvcOrderRequest request) {
        return new MvcOrderResponse("ORD-003", request.status());
    }

    @GetMapping("/context")
    public ContextView context() {
        Long userId = RequestContext.getUserId();
        return new ContextView(userId == null ? null : userId.toString());
    }

    /**
     * RequestContext 可见性验证用视图。
     *
     * @param userId 当前用户 ID（字符串化，规避 Long → String 序列化干扰断言）
     */
    public record ContextView(String userId) {
    }
}
