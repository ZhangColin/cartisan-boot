package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.web.TestApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

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

/**
 * BaseEnum Jackson 反序列化器测试。
 * <p>
 * 使用 Spring Boot 集成测试，验证 Jackson 配置正确注册了 BaseEnumDeserializer。
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class
)
class BaseEnumDeserializerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldDeserializeCodeToEnum() throws Exception {
        String json = "{\"priority\":2}";

        TestPriorityRequest request = objectMapper.readValue(json, TestPriorityRequest.class);

        assertThat(request.priority()).isEqualTo(TestPriority.HIGH);
    }

    @Test
    void shouldDeserializeNullToNull() throws Exception {
        String json = "{\"priority\":null}";

        TestPriorityRequest request = objectMapper.readValue(json, TestPriorityRequest.class);

        assertThat(request.priority()).isNull();
    }

    @Test
    void shouldDeserializeInvalidCodeToNull() throws Exception {
        String json = "{\"priority\":999}";

        TestPriorityRequest request = objectMapper.readValue(json, TestPriorityRequest.class);

        // parseByCode 对无效 code 返回 null
        assertThat(request.priority()).isNull();
    }
}