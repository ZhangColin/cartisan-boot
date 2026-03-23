package com.cartisan.web.mapper.fixtures;

import com.cartisan.web.mapper.DomainMapper;
import org.mapstruct.Mapper;

/**
 * 测试用 Mapper，用于验证 MapStruct 生成代码。
 *
 * <p>注意：使用 {@code componentModel = "default"} 让 MapStruct 生成 {@code INSTANCE} 字段，
 * 方便测试时直接获取实例。
 */
@Mapper(componentModel = "default")
public interface TestMapper extends DomainMapper<SimpleEntity, SimpleDto> {
    // convert 方法由 MapStruct 自动生成实现，继承自 DomainMapper
}
