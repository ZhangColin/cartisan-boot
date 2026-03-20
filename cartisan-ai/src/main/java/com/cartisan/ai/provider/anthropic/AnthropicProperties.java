package com.cartisan.ai.provider.anthropic;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic Provider 配置属性。
 *
 * <p>配置前缀：{@code cartisan.ai.anthropic}
 *
 * <p>{@code @EnableConfigurationProperties} 注册在 F05-09 自动装配模块中完成。
 */
@ConfigurationProperties("cartisan.ai.anthropic")
public class AnthropicProperties {

    /**
     * Anthropic API 密钥，作为 x-api-key header 发送，必填。
     */
    private String apiKey;

    /**
     * API 基础 URL，默认指向 Anthropic 官方 API。
     * <p>可配置为代理地址或 AWS Bedrock 端点。
     */
    private String baseUrl = "https://api.anthropic.com";

    /**
     * 支持的模型名称列表。
     * <p>默认包含 Claude 常用模型（2025 年版本）。
     */
    private List<String> models = new ArrayList<>(List.of(
            "claude-sonnet-4-20250514",
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229"
    ));

    /**
     * 默认 max_tokens，当 ChatRequest.maxTokens 为 null 时使用。
     * <p>Anthropic 要求 max_tokens 必填，OpenAI 则可选。
     */
    private int defaultMaxTokens = 4096;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public int getDefaultMaxTokens() {
        return defaultMaxTokens;
    }

    public void setDefaultMaxTokens(int defaultMaxTokens) {
        this.defaultMaxTokens = defaultMaxTokens;
    }
}