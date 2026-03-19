package com.cartisan.ai.provider;

import com.cartisan.ai.model.TokenUsage;

@FunctionalInterface
public interface ModelUsageListener {

    /**
     * 每次 AI 调用完成后触发，用于 Token 用量统计与计费。
     *
     * @param providerId 提供商标识，来自 {@link ModelProvider#id()}
     * @param model      实际使用的模型名，来自 {@code ChatResponse.model()}（服务端确认值，
     *                   在代理/路由场景下可能与请求中的 model 不同）
     * @param usage      本次调用消耗的 Token 明细
     */
    void onUsage(String providerId, String model, TokenUsage usage);
}
