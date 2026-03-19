/**
 * Cartisan AI — 大模型调用统一抽象模块
 *
 * <p>本模块提供多 Provider 的统一对话接口（同步 + 流式 SSE），包括：</p>
 * <ul>
 *   <li>{@code model} — 统一对话模型（ChatMessage、ChatRequest、ChatResponse 等）</li>
 *   <li>{@code provider} — Provider SPI 及 OpenAI/Anthropic/DeepSeek 实现</li>
 *   <li>{@code sse} — SSE 流式工具（Flux → SseEmitter）</li>
 *   <li>{@code config} — Spring Boot 自动配置</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.ai;
