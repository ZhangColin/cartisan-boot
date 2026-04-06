package com.cartisan.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 复合应用事件发布器。
 */
@Component
public class CompositeApplicationEventPublisher implements ApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CompositeApplicationEventPublisher.class);

    private final Map<String, ApplicationEventPublisher> publishers;

    public CompositeApplicationEventPublisher(List<ApplicationEventPublisher> publisherList) {
        this.publishers = publisherList.stream()
            .collect(Collectors.toMap(
                ApplicationEventPublisher::getType,
                Function.identity()
            ));
        log.info("Initialized event publishers: {}", publishers.keySet());
    }

    @Override
    public String getType() {
        return "composite";
    }

    @Override
    public void publishApplicationEvent(ApplicationEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        PublishTo annotation = event.getClass().getAnnotation(PublishTo.class);

        if (annotation == null) {
            publishTo("spring", event);
            return;
        }

        for (String type : annotation.value()) {
            publishTo(type, event);
        }
    }

    private void publishTo(String type, ApplicationEvent event) {
        ApplicationEventPublisher publisher = publishers.get(type);
        if (publisher == null) {
            throw new IllegalArgumentException(
                "No publisher found for type: " + type +
                ". Available types: " + publishers.keySet()
            );
        }

        try {
            publisher.publishApplicationEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish event to {}: eventId={}, eventType={}",
                type, event.eventId(), event.eventType(), e);
        }
    }
}
