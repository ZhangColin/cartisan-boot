package com.cartisan.event.impl;

import com.cartisan.event.ApplicationEvent;
import com.cartisan.event.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 基于Spring事件机制的应用事件发布器。
 */
@Component
@ConditionalOnProperty(
    name = "cartisan.event.publisher.spring.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SpringApplicationEventPublisher implements ApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringApplicationEventPublisher.class);

    private final org.springframework.context.ApplicationEventPublisher publisher;

    public SpringApplicationEventPublisher(
            org.springframework.context.ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher cannot be null");
    }

    @Override
    public String getType() {
        return "spring";
    }

    @Override
    public void publishApplicationEvent(ApplicationEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        try {
            publisher.publishEvent(event);
        } catch (Exception e) {
            log.error("发布Spring事件失败: eventId={}, eventType={}",
                event.eventId(), event.eventType(), e);
        }
    }
}
