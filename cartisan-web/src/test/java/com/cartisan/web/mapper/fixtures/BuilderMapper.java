package com.cartisan.web.mapper.fixtures;

import com.cartisan.web.mapper.DomainMapper;
import org.mapstruct.Mapper;

/**
 * 测试用 Mapper，用于验证传统 POJO 映射。
 */
@Mapper(componentModel = "default")
public interface BuilderMapper extends DomainMapper<SimpleEntity, BuilderDto> {

    BuilderDto toBuilderDto(SimpleEntity entity);
}
