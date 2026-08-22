package com.cartisan.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BaseEnum 非法取值错误的业务码覆盖配置。
 *
 * <p>默认响应 code 为 400。消费服务可将特定枚举的取值错误映射为业务码
 * （HTTP 状态保持 400，仅覆盖响应体 code）：</p>
 *
 * <pre>{@code
 * cartisan:
 *   web:
 *     enum-error:
 *       codes:
 *         com.aiplatform.project.domain.ProjectStatus: 1014   # FQN
 *         OrderStatus: 1024                                   # 或简单类名
 * }</pre>
 *
 * <p>查找顺序：先 FQN（{@code cartisan.web.enum-error.codes.[com.foo.Bar]}），
 * 再简单类名；均未命中时回退默认 400。</p>
 *
 * @since 0.10.0
 */
@ConfigurationProperties(prefix = "cartisan.web.enum-error")
public class EnumErrorProperties {

    /**
     * 枚举类（FQN 或简单类名）→ 业务码。
     */
    private Map<String, Integer> codes = new LinkedHashMap<>();

    public Map<String, Integer> getCodes() {
        return codes;
    }

    public void setCodes(Map<String, Integer> codes) {
        this.codes = codes;
    }
}
