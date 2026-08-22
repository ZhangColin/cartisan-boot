package com.cartisan.test.archunit.fixtures.compliant.controller;

import com.cartisan.test.archunit.fixtures.compliant.application.GoodAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

/**
 * 合规的控制器示例
 * - 使用 @RestController 注解
 * - 以 Controller 结尾
 * - 构造函数注入（无 @Autowired 字段）
 * - 只依赖应用服务
 * - 有 @Tag 与非空 @Operation(summary)
 */
@Tag(name = "商品")
@RestController
public class GoodController {

    private final GoodAppService appService;

    public GoodController(GoodAppService appService) {
        this.appService = appService;
    }

    @Operation(summary = "创建商品")
    @PostMapping
    public String create(BigDecimal price) {
        appService.create(price);
        return "OK";
    }
}
