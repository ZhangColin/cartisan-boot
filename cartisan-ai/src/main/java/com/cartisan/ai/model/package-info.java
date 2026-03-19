/**
 * 统一对话模型
 *
 * <p>与具体 Provider 无关的数据类型，包括：</p>
 * <ul>
 *   <li>{@code ChatMessage} — 单条消息（role + content）</li>
 *   <li>{@code ChatRequest} — 对话请求</li>
 *   <li>{@code ChatResponse} — 同步响应</li>
 *   <li>{@code ChatStreamEvent} — 流式事件（delta + finished + usage）</li>
 *   <li>{@code TokenUsage} — Token 用量统计</li>
 * </ul>
 *
 * @since 0.1.0
 */
package com.cartisan.ai.model;
