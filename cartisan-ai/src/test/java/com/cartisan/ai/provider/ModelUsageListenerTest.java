package com.cartisan.ai.provider;

import com.cartisan.ai.model.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelUsageListenerTest {

    @Test
    void shouldBeAssignableAsLambda() {
        List<String> captured = new ArrayList<>();

        ModelUsageListener listener = (providerId, model, usage) ->
            captured.add(providerId + ":" + model + ":" + usage.totalTokens());

        listener.onUsage("openai", "gpt-4o", new TokenUsage(10, 20, 30));

        assertThat(captured).containsExactly("openai:gpt-4o:30");
    }
}
