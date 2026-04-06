package com.cartisan.event;

import java.lang.annotation.*;

/**
 * 标记事件发布方式。
 *
 * <p>支持同时发布到多个目标（如Spring + RabbitMQ）。</p>
 *
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishTo {
    String[] value();
}
