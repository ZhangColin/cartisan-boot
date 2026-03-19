package com.cartisan.ai.provider;

import com.cartisan.ai.model.ChatRequest;
import com.cartisan.ai.model.ChatResponse;
import com.cartisan.ai.model.ChatStreamEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ModelProvider {

    String id();

    List<String> supportedModels();

    ChatResponse chat(ChatRequest request);

    Flux<ChatStreamEvent> chatStream(ChatRequest request);
}
