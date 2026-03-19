package com.cartisan.ai.provider.deepseek;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek Provider 配置属性。
 *
 * <p>配置前缀：{@code cartisan.ai.deepseek}
 *
 * <p>{@code @EnableConfigurationProperties} 注册在 F05-09 自动装配模块中完成。
 */
@ConfigurationProperties("cartisan.ai.deepseek")
public class DeepSeekProperties {

    /**
     * DeepSeek API 密钥，作为 Bearer token 发送，必填。
     */
    private String apiKey;

    private String baseUrl = "https://api.deepseek.com/v1";

    /**
     * 支持的模型名称列表。
     *
     * <p>默认包含 DeepSeek 常用模型；可覆盖此列表以匹配实际可用的模型名称。
     */
    private List<String> models = new ArrayList<>(List.of(
            "deepseek-chat",
            "deepseek-reasoner"
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
