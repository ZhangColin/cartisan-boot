package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * BaseEnum Jackson 序列化器。
 * <p>
 * 将 BaseEnum 序列化为其 code 值（Integer）。
 */
public class BaseEnumSerializer extends JsonSerializer<BaseEnum<?>> {

    @Override
    public void serialize(BaseEnum<?> value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeNumber(value.getCode());
        }
    }
}