package com.cartisan.test.archunit.fixtures.violation.shared;

import org.mapstruct.Mapper;

/**
 * 违规：MapStruct Mapper 不继承 DomainMapper
 * 违反规则：mapstructMappersShouldExtendDomainMapper
 */
@Mapper(componentModel = "spring")
public interface BadMapper {
    String convert(String input);
}