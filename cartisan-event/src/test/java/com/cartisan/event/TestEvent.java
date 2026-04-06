package com.cartisan.event;

import java.time.Instant;

@PublishTo("nonexistent")
public record TestEvent(
    String eventId,
    Instant occurredAt
) implements ApplicationEvent {
    
    @Override
    public String eventType() {
        return "test.event";
    }
}
