package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

enum TestPriority implements BaseEnum<TestPriority> {
    LOW(0, "低"),
    MEDIUM(1, "中"),
    HIGH(2, "高");

    private final Integer code;
    private final String name;

    TestPriority(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}

record TestPriorityRequest(TestPriority priority) {}

class BaseEnumDeserializerTest {

    @Test
    void shouldDeserializeCodeToEnum() throws Exception {
        var mapper = createConfiguredObjectMapper();
        String json = "{\"priority\":2}";

        TestPriorityRequest request = mapper.readValue(json, TestPriorityRequest.class);

        assertThat(request.priority()).isEqualTo(TestPriority.HIGH);
    }

    @Test
    void shouldDeserializeNullToNull() throws Exception {
        var mapper = createConfiguredObjectMapper();
        String json = "{\"priority\":null}";

        TestPriorityRequest request = mapper.readValue(json, TestPriorityRequest.class);

        assertThat(request.priority()).isNull();
    }

    @Test
    void shouldDeserializeInvalidCodeToNull() throws Exception {
        var mapper = createConfiguredObjectMapper();
        String json = "{\"priority\":999}";

        TestPriorityRequest request = mapper.readValue(json, TestPriorityRequest.class);

        // parseByCode 对无效 code 返回 null
        assertThat(request.priority()).isNull();
    }

    private ObjectMapper createConfiguredObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        // Create a custom deserializer for TestPriority
        module.addDeserializer(TestPriority.class, new JsonDeserializer<TestPriority>() {
            @Override
            public TestPriority deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                Integer code = p.getValueAsInt();
                if (code == null) {
                    return null;
                }
                return BaseEnum.parseByCode(TestPriority.class, code);
            }
        });
        mapper.registerModule(module);
        return mapper;
    }
}