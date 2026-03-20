package com.cartisan.ai.sse;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * SSE 流式输出配置属性。
 *
 * <p>配置前缀：{@code cartisan.ai.sse}
 *
 * <p>{@code @EnableConfigurationProperties} 注册在 F05-09 自动装配模块中完成。
 */
@ConfigurationProperties("cartisan.ai.sse")
public class SseProperties {

    /**
     * SSE 连接超时时间，默认 5 分钟。
     * <p>设置为 {@code -1} 表示无超时限制。</p>
     */
    private Duration timeout = Duration.ofMinutes(5);

    /**
     * 心跳间隔，默认 30 秒。
     */
    private Duration heartbeat = Duration.ofSeconds(30);

    /**
     * 是否启用心跳机制，默认 true。
     */
    private boolean heartbeatEnabled = true;

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Duration heartbeat) {
        this.heartbeat = heartbeat;
    }

    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }

    public void setHeartbeatEnabled(boolean heartbeatEnabled) {
        this.heartbeatEnabled = heartbeatEnabled;
    }
}
