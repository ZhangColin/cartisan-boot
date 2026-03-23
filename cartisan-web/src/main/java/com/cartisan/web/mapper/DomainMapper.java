package com.cartisan.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct 基础 Mapper 接口。
 *
 * <p>业务项目的 Mapper 接口继承此接口后，只需添加 {@code @Mapper} 注解即可，
 * 推荐配置已在此接口的注解中定义。
 *
 * <p><b>推荐用法：</b>
 * <pre>{@code
 * @Mapper(  // 继承 DomainMapper 的配置
 *     componentModel = "spring",
 *     nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
 *     nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT
 * )
 * public interface UserMapper extends DomainMapper<User, UserResponse> {
 *     UserResponse toResponse(User user);
 *     List<UserResponse> toResponseList(List<User> users);
 * }
 * }</pre>
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 */
public interface DomainMapper<S, T> {
    // 配置参考：业务项目继承后需要添加 @Mapper 注解
    // 推荐配置：
    // - componentModel = "spring": 启用 Spring 依赖注入
    // - nullValueCheckStrategy = ALWAYS: 总是检查 null 值
    // - nullValuePropertyMappingStrategy = SET_TO_DEFAULT: 设置默认值
    // 映射方法由业务项目根据需要定义
}