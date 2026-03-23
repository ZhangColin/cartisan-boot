package com.cartisan.web.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper 测试基类。
 *
 * <p>封装 Mapper 测试的通用逻辑，业务项目的 Mapper 测试继承此类后，
 * 只需实现三个抽象方法即可完成基本测试覆盖。
 *
 * <p>注意：测试基类假设业务项目定义了 {@code toResponse()} 方法。
 * 如果业务项目使用其他方法名，需要重写测试方法。
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 * @param <M> Mapper 类型
 */
public abstract class DomainMapperTest<S, T, M extends DomainMapper<S, T>> {

    protected abstract M getMapper();
    protected abstract S createSource();
    protected abstract void assertMapped(T target);

    /**
     * 映射函数，由子类提供具体实现。
     *
     * @param mapper Mapper 实例
     * @param source 源对象
     * @return 映射后的目标对象
     */
    protected abstract T map(M mapper, S source);

    @Test
    void shouldMapSourceToTarget() {
        S source = createSource();
        M mapper = getMapper();
        T target = map(mapper, source);
        assertMapped(target);
    }

    @Test
    void shouldReturnNull_whenSourceIsNull() {
        M mapper = getMapper();
        T target = map(mapper, null);
        assertThat(target).isNull();
    }
}