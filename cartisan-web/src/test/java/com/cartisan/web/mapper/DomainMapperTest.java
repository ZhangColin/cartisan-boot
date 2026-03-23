package com.cartisan.web.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper 测试基类。
 *
 * <p>封装 Mapper 测试的通用逻辑，业务项目的 Mapper 测试继承此类后，
 * 只需实现三个抽象方法即可完成基本测试覆盖。
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 * @param <M> Mapper 类型
 */
public abstract class DomainMapperTest<S, T, M extends DomainMapper<S, T>> {

    /**
     * 获取被测试的 Mapper 实例。
     *
     * <p>使用 {@code Mappers.getMapper()} 获取非 Spring 容器管理的实例。
     *
     * @return Mapper 实例
     */
    protected abstract M getMapper();

    /**
     * 创建用于测试的源对象。
     *
     * @return 测试用的源对象
     */
    protected abstract S createSource();

    /**
     * 断言映射结果是否符合预期。
     *
     * @param target 映射后的目标对象
     */
    protected abstract void assertMapped(T target);

    /**
     * 测试正常映射场景。
     */
    @Test
    void shouldMapSourceToTarget() {
        S source = createSource();
        M mapper = getMapper();

        T target = mapper.toResponse(source);

        assertMapped(target);
    }

    /**
     * 测试源对象为 null 时的行为。
     */
    @Test
    void shouldReturnNull_whenSourceIsNull() {
        M mapper = getMapper();

        T target = mapper.toResponse(null);

        assertThat(target).isNull();
    }
}