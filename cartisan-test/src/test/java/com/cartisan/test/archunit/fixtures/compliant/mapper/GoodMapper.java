package com.cartisan.test.archunit.fixtures.compliant.mapper;

import com.cartisan.web.mapper.DomainMapper;
import org.mapstruct.Mapper;

/**
 * 合规：MapStruct Mapper 继承 DomainMapper
 * 符合规则：mapstructMappersShouldExtendDomainMapper
 */
@Mapper(componentModel = "spring")
public interface GoodMapper extends DomainMapper<String, String> {
    // convert 方法由 MapStruct 自动生成
}