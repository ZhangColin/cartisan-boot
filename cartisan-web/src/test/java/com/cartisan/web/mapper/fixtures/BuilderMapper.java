package com.cartisan.web.mapper.fixtures;

import com.cartisan.web.mapper.DomainMapper;
import org.mapstruct.Mapper;

/**
 * 测试用 Mapper，用于验证 Lombok Builder 映射。
 *
 * <p>不指定 componentModel，MapStruct 会生成 {@code INSTANCE} 字段。
 */
@Mapper
public interface BuilderMapper extends DomainMapper<SimpleEntity, BuilderDto> {

    BuilderDto toBuilderDto(SimpleEntity entity);
}
