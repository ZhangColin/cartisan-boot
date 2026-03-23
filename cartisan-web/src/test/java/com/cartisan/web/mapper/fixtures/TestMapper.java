package com.cartisan.web.mapper.fixtures;

import com.cartisan.web.mapper.DomainMapper;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 测试用 Mapper，用于验证 MapStruct 生成代码。
 *
 * <p>注意：使用 {@code componentModel = "default"} 而非继承的 {@code "spring"}，
 * 这样 MapStruct 会生成 {@code INSTANCE} 字段，方便测试时直接获取实例。
 */
@Mapper(componentModel = "default")
public interface TestMapper extends DomainMapper<SimpleEntity, SimpleDto> {

    SimpleDto toResponse(SimpleEntity entity);

    List<SimpleDto> toResponseList(List<SimpleEntity> entities);
}
