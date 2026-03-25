package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

enum TestVisibility implements BaseEnum<TestVisibility> {
    PUBLIC(1, "公开"),
    PRIVATE(0, "私有");

    private final Integer code;
    private final String name;

    TestVisibility(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}

@ExtendWith(MockitoExtension.class)
class BaseEnumSerializerTest {

    @Mock
    private JsonGenerator mockGen;

    @Mock
    private SerializerProvider mockProvider;

    @Test
    void shouldSerializeEnumToCode() throws Exception {
        var serializer = new BaseEnumSerializer();
        var response = TestVisibility.PUBLIC;

        serializer.serialize(response, mockGen, mockProvider);

        verify(mockGen).writeNumber(1);
    }

    @Test
    void shouldSerializeNullEnum() throws Exception {
        var serializer = new BaseEnumSerializer();
        var response = (TestVisibility) null;

        serializer.serialize(response, mockGen, mockProvider);

        verify(mockGen).writeNull();
    }
}