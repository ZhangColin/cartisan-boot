package com.cartisan.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认的事件发布失败处理器，记录 ERROR 日志。
 *
 * @since 0.1.0
 */
public class LoggingEventPublishFailureHandler implements EventPublishFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublishFailureHandler.class);

    @Override
    public void onFailure(String publisherType, ApplicationEvent event, Exception exception) {
        log.error("Failed to publish event to {}: eventId={}, eventType={}",
            publisherType, event.eventId(), event.eventType(), exception);
    }
}
