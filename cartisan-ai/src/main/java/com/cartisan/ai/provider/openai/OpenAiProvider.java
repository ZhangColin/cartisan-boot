package com.cartisan.ai.provider.openai;

import com.cartisan.ai.provider.openaicompat.OpenAiCompatibleClient;
import com.cartisan.ai.provider.openaicompat.OpenAiCompatibleProvider;

import java.util.List;

public class OpenAiProvider extends OpenAiCompatibleProvider {

    private final List<String> supportedModels;

    public OpenAiProvider(OpenAiProperties properties) {
        super(new OpenAiCompatibleClient(properties.getBaseUrl(), properties.getApiKey()));
        this.supportedModels = List.copyOf(properties.getModels());
    }

    OpenAiProvider(OpenAiCompatibleClient client, List<String> models) {
        super(client);
        this.supportedModels = List.copyOf(models);
    }

    @Override
    public String id() {
        return "openai";
    }

    @Override
    public List<String> supportedModels() {
        return supportedModels;
    }
}
