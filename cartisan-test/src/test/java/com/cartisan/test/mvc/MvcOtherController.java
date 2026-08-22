package com.cartisan.test.mvc;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 未被切片指定的 Controller，用于验证 controller 过滤语义不被破坏。
 */
@RestController
public class MvcOtherController {

    @GetMapping("/test/other")
    public String other() {
        return "other";
    }
}
