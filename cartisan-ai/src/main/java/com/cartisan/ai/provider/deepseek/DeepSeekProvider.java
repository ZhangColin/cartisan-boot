package com.cartisan.ai.provider.deepseek;

import com.cartisan.ai.provider.openaicompat.OpenAiCompatibleClient;
import com.cartisan.ai.provider.openaicompat.OpenAiCompatibleProvider;

import java.util.List;

public class DeepSeekProvider extends OpenAiCompatibleProvider {

    private final List<String> supportedModels;

    public DeepSeekProvider(DeepSeekProperties properties) {
        super(new OpenAiCompatibleClient(properties.getBaseUrl(), properties.getApiKey()));
        this.supportedModels = List.copyOf(properties.getModels());
    }

    DeepSeekProvider(OpenAiCompatibleClient client, List<String> models) {
        super(client);
        this.supportedModels = List.copyOf(models);
    }

    @Override
    public String id() {
        return "deepseek";
    }

    @Override
    public List<String> supportedModels() {
        return supportedModels;
    }
}
