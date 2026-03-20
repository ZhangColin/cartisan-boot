/**
 * Anthropic Claude API Provider 实现。
 *
 * <p>独立协议实现，与 OpenAI 不兼容：
 * <ul>
 *   <li>Header 使用 {@code x-api-key} 而非 {@code Authorization: Bearer}</li>
 *   <li>响应格式不同：{@code content} 是数组而非单一字符串</li>
 *   <li>流式事件格式不同：使用 {@code content_block_delta} 事件类型</li>
 * </ul>
 *
 * @since 0.5.0
 */
package com.cartisan.ai.provider.anthropic;