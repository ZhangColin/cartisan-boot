/**
 * Provider SPI 及各实现
 *
 * <p>包括：</p>
 * <ul>
 *   <li>{@code ModelProvider} — 统一调用接口（chat / chatStream）</li>
 *   <li>{@code ModelProviderRegistry} — 按 providerId / modelName 查找 Provider</li>
 *   <li>{@code OpenAiProvider}、{@code DeepSeekProvider}、{@code AnthropicProvider} — 各 Provider 实现</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.ai.provider;
