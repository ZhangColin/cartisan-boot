package com.cartisan.ai.provider.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Provider 配置属性。
 *
 * <p>配置前缀：{@code cartisan.ai.openai}
 *
 * <p>{@code @EnableConfigurationProperties} 注册在 F05-09 自动装配模块中完成。
 */
@ConfigurationProperties("cartisan.ai.openai")
public class OpenAiProperties {

    /**
     * OpenAI API 密钥，作为 Bearer token 发送，必填。
     */
    private String apiKey;

    private String baseUrl = "https://api.openai.com/v1";

    /**
     * 支持的模型名称列表。
     *
     * <p>默认包含常用 OpenAI 模型；在使用代理服务或自定义端点时可覆盖此列表以匹配实际可用的模型名称。
     */
    private List<String> models = new ArrayList<>(List.of(
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4-turbo",
            "gpt-3.5-turbo",
            "o1",
            "o1-mini",
            "o3-mini"
    ));

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
}
