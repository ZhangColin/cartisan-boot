package com.cartisan.web.mapper;

import com.cartisan.web.mapper.fixtures.BuilderDto;
import com.cartisan.web.mapper.fixtures.BuilderMapper;
import com.cartisan.web.mapper.fixtures.SimpleDto;
import com.cartisan.web.mapper.fixtures.SimpleEntity;
import com.cartisan.web.mapper.fixtures.TestMapper;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DomainMapper 集成测试")
class DomainMapperIntegrationTest {

    @Test
    @DisplayName("应该生成 Mapper 实现类")
    void shouldGenerateMapperImplementation() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        SimpleEntity entity = new SimpleEntity(1L, "张三", "zhang@example.com");
        SimpleDto dto = mapper.toResponse(entity);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("张三");
        assertThat(dto.email()).isEqualTo("zhang@example.com");
    }

    @Test
    @DisplayName("应该支持 List 映射")
    void shouldSupportListMapping() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        List<SimpleEntity> entities = List.of(
            new SimpleEntity(1L, "张三", "zhang@example.com"),
            new SimpleEntity(2L, "李四", "li@example.com")
        );

        List<SimpleDto> dtos = mapper.toResponseList(entities);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).name()).isEqualTo("张三");
        assertThat(dtos.get(1).name()).isEqualTo("李四");
    }

    @Test
    @DisplayName("应该处理 null 输入")
    void shouldHandleNullInput() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        SimpleDto dto = mapper.toResponse(null);

        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("应该支持传统 POJO 映射")
    void shouldSupportTraditionalPOMapping() {
        BuilderMapper mapper = Mappers.getMapper(BuilderMapper.class);

        SimpleEntity entity = new SimpleEntity(1L, "张三", "zhang@example.com");
        BuilderDto dto = mapper.toBuilderDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("张三");
        assertThat(dto.getEmail()).isEqualTo("zhang@example.com");
    }
}
