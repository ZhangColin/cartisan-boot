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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DomainMapper 集成测试")
class DomainMapperIntegrationTest {

    @Test
    @DisplayName("应该生成 Mapper 实现类")
    void shouldGenerateMapperImplementation() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        SimpleEntity entity = new SimpleEntity(1L, "张三", "zhang@example.com");
        SimpleDto dto = mapper.convert(entity);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("张三");
        assertThat(dto.email()).isEqualTo("zhang@example.com");
    }

    @Test
    @DisplayName("应该处理 null 输入")
    void shouldHandleNullInput() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        SimpleDto dto = mapper.convert(null);

        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("应该支持 Lombok Builder 映射")
    void shouldSupportLombokBuilderMapping() {
        BuilderMapper mapper = Mappers.getMapper(BuilderMapper.class);

        SimpleEntity entity = new SimpleEntity(1L, "张三", "zhang@example.com");
        BuilderDto dto = mapper.toBuilderDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("张三");
        assertThat(dto.getEmail()).isEqualTo("zhang@example.com");
    }

    @Test
    @DisplayName("应该使用默认方法转换 List")
    void shouldConvertListUsingDefaultMethod() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        List<SimpleEntity> entities = List.of(
            new SimpleEntity(1L, "张三", "zhang@example.com"),
            new SimpleEntity(2L, "李四", "li@example.com")
        );

        List<SimpleDto> dtos = mapper.convertList(entities);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).name()).isEqualTo("张三");
        assertThat(dtos.get(1).name()).isEqualTo("李四");
    }

    @Test
    @DisplayName("应该使用默认方法转换 Set")
    void shouldConvertSetUsingDefaultMethod() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        Set<SimpleEntity> entities = Set.of(
            new SimpleEntity(1L, "张三", "zhang@example.com"),
            new SimpleEntity(2L, "李四", "li@example.com")
        );

        Set<SimpleDto> dtos = mapper.convertSet(entities);

        assertThat(dtos).hasSize(2);
        assertThat(dtos).extracting(SimpleDto::name).containsExactlyInAnyOrder("张三", "李四");
    }

    @Test
    @DisplayName("应该处理 null List")
    void shouldHandleNullList() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        List<SimpleDto> dtos = mapper.convertList(null);

        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("应该处理空 List")
    void shouldHandleEmptyList() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        List<SimpleDto> dtos = mapper.convertList(List.of());

        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("应该处理 null Set")
    void shouldHandleNullSet() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        Set<SimpleDto> dtos = mapper.convertSet(null);

        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("应该处理空 Set")
    void shouldHandleEmptySet() {
        TestMapper mapper = Mappers.getMapper(TestMapper.class);

        Set<SimpleDto> dtos = mapper.convertSet(Set.of());

        assertThat(dtos).isEmpty();
    }
}
