package com.cartisan.test.mvc;

import com.cartisan.core.context.RequestContext;
import com.cartisan.web.request.Pagination;
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
 * <p>覆盖三种枚举入口（query / path / body）、RequestContext 叠加场景与
 * Pagination record 绑定（验证切片与完整 MVC 在分页 wire 契约上无差异）。</p>
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

    @GetMapping("/pagination")
    public PaginationView pagination(Pagination pagination) {
        return new PaginationView(pagination.page(), pagination.size());
    }

    /**
     * RequestContext 可见性验证用视图。
     *
     * @param userId 当前用户 ID（字符串化，规避 Long → String 序列化干扰断言）
     */
    public record ContextView(String userId) {
    }

    /**
     * 分页绑定验证用视图（回显归一化后的值）。
     */
    public record PaginationView(Integer page, Integer size) {
    }
}
