package com.cartisan.ai.provider.deepseek;

import com.cartisan.ai.provider.openaicompat.OpenAiCompatibleClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DeepSeekProviderTest {

    private OpenAiCompatibleClient mockClient;
    private DeepSeekProvider provider;

    @BeforeEach
    void setUp() {
        mockClient = mock(OpenAiCompatibleClient.class);
        provider = new DeepSeekProvider(mockClient, List.of("deepseek-chat", "deepseek-reasoner"));
    }

    @Test
    void shouldReturnDeepSeek_whenIdCalled() {
        assertThat(provider.id()).isEqualTo("deepseek");
    }

    @Test
    void shouldReturnConfiguredModels_whenSupportedModelsCalled() {
        assertThat(provider.supportedModels()).containsExactly("deepseek-chat", "deepseek-reasoner");
    }
}
