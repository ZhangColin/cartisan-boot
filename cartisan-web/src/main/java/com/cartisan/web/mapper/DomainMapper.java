package com.cartisan.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
 *     // convert 方法由 MapStruct 自动生成实现
 * }
 * }</pre>
 *
 * <p>本接口提供了：
 * <ul>
 *   <li>{@link #convert(Object)} - 单个对象转换（抽象方法，由子类/MapStruct 实现）</li>
 *   <li>{@link #convertList(List)} - 批量转换 List（默认方法）</li>
 *   <li>{@link #convertSet(Set)} - 批量转换 Set（默认方法）</li>
 * </ul>
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

    /**
     * 转换单个对象。
     *
     * <p>此方法由子接口定义，MapStruct 会自动生成实现。
     *
     * @param source 源对象
     * @return 转换后的目标对象
     */
    T convert(S source);

    /**
     * 批量转换 List。
     *
     * <p>默认方法会调用 {@link #convert(Object)} 方法进行单个元素转换。
     *
     * @param sources 源对象列表，可以为 null
     * @return 转换后的目标对象列表，如果输入为 null 或空则返回空列表
     */
    default List<T> convertList(List<S> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream()
                .map(this::convert)
                .toList();
    }

    /**
     * 批量转换 Set。
     *
     * <p>默认方法会调用 {@link #convert(Object)} 方法进行单个元素转换。
     *
     * @param sources 源对象集合，可以为 null
     * @return 转换后的目标对象集合，如果输入为 null 或空则返回空集合
     */
    default Set<T> convertSet(Set<S> sources) {
        if (sources == null || sources.isEmpty()) {
            return Set.of();
        }
        return sources.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }
}