package com.cartisan.web.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper 测试基类。
 *
 * <p>封装 Mapper 测试的通用逻辑，业务项目的 Mapper 测试继承此类后，
 * 只需实现抽象方法即可完成基本测试覆盖。
 *
 * <p><b>注意：</b>由于 {@link DomainMapper} 是标记接口（仅用于继承 MapStruct 配置），
 * 具体的映射方法由子接口定义，因此此基类无法提供通用的映射测试。
 * 业务项目应直接编写测试，或参考 {@link DomainMapperIntegrationTest} 的实现。
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 * @param <M> Mapper 类型
 * @deprecated DomainMapper 是标记接口，没有通用方法可供测试。
 *             建议直接编写测试类，参考 {@link DomainMapperIntegrationTest}。
 */
@Deprecated(since = "1.0", forRemoval = true)
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
     * 映射函数，由子类提供具体实现。
     *
     * @param mapper Mapper 实例
     * @param source 源对象
     * @return 映射后的目标对象
     */
    protected abstract T map(M mapper, S source);

    /**
     * 测试正常映射场景。
     */
    @Test
    void shouldMapSourceToTarget() {
        S source = createSource();
        M mapper = getMapper();

        T target = map(mapper, source);

        assertMapped(target);
    }

    /**
     * 测试源对象为 null 时的行为。
     */
    @Test
    void shouldReturnNull_whenSourceIsNull() {
        M mapper = getMapper();

        T target = map(mapper, null);

        assertThat(target).isNull();
    }
}