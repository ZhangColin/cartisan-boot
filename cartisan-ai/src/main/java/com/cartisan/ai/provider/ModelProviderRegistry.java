package com.cartisan.ai.provider;

import com.cartisan.core.exception.BaseCodeMessage;
import com.cartisan.core.exception.DomainException;
import reactor.core.publisher.Flux;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cartisan.ai.model.*;

public class ModelProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelProviderRegistry.class);

    private final Map<String, ModelProvider> providerById;
    private final Map<String, ModelProvider> providerByModel;
    private final List<ModelUsageListener> listeners;

    public ModelProviderRegistry(List<ModelProvider> providers, List<ModelUsageListener> listeners) {
        this.listeners = List.copyOf(listeners);

        Map<String, ModelProvider> byId    = new HashMap<>();
        Map<String, ModelProvider> byModel = new HashMap<>();

        for (ModelProvider provider : providers) {
            byId.put(provider.id(), provider);
            for (String model : provider.supportedModels()) {
                if (byModel.containsKey(model)) {
                    throw new IllegalArgumentException(
                        "Duplicate model name '" + model + "' declared by providers '"
                        + byModel.get(model).id() + "' and '" + provider.id() + "'");
                }
                byModel.put(model, provider);
            }
        }

        this.providerById    = Map.copyOf(byId);
        this.providerByModel = Map.copyOf(byModel);
    }

    public List<ModelProvider> listProviders() {
        return List.copyOf(providerById.values());
    }

    public ModelProvider getProvider(String providerId) {
        ModelProvider provider = providerById.get(providerId);
        if (provider == null) {
            throw new DomainException(BaseCodeMessage.RESOURCE_NOT_FOUND, providerId);
        }
        return provider;
    }

    public ModelProvider getProviderByModel(String modelName) {
        ModelProvider provider = providerByModel.get(modelName);
        if (provider == null) {
            throw new DomainException(BaseCodeMessage.RESOURCE_NOT_FOUND, modelName);
        }
        return provider;
    }

    public ChatResponse chat(String providerId, ChatRequest request) {
        ModelProvider provider = getProvider(providerId);
        ChatResponse response = provider.chat(request);
        notifyListeners(provider.id(), response.model(), response.usage());
        return response;
    }

    public Flux<ChatStreamEvent> chatStream(String providerId, ChatRequest request) {
        // implemented in Task 3
        throw new UnsupportedOperationException("not yet implemented");
    }

    // called by chat() (Task 2) and chatStream() (Task 3)
    private void notifyListeners(String providerId, String model, TokenUsage usage) {
        for (ModelUsageListener listener : listeners) {
            try {
                listener.onUsage(providerId, model, usage);
            } catch (Exception e) {
                log.warn("ModelUsageListener threw an exception and was ignored", e);
            }
        }
    }
}
