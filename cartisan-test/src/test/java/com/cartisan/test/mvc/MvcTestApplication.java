package com.cartisan.test.mvc;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {@code @CartisanMvcTest} 验证用 Spring Boot 应用。
 *
 * <p>切片测试经包结构向上查找 {@code @SpringBootConfiguration}；
 * controller 过滤语义（只装配指定 controller）仅作用于规范路径下的组件扫描。</p>
 */
@SpringBootApplication
public class MvcTestApplication {
}
