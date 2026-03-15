package com.cartisan.data.query.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

@DisplayName("PageQuery 分页查询参数测试")
class PageQueryTest {

    @DisplayName("给定 page < 1，构造时应修正为 1")
    @Test
    void given_pageLessThanOne_when_construct_then_pageNormalizedToOne() {
        PageQuery query = new PageQuery(0, 20);
        assertThat(query.page()).isEqualTo(1);
    }

    @DisplayName("给定 page 为负数，构造时应修正为 1")
    @Test
    void given_negativePage_when_construct_then_pageNormalizedToOne() {
        PageQuery query = new PageQuery(-5, 20);
        assertThat(query.page()).isEqualTo(1);
    }

    @DisplayName("给定 size < 1，构造时应修正为默认值 20")
    @Test
    void given_sizeLessThanOne_when_construct_then_sizeNormalizedToTwenty() {
        PageQuery query = new PageQuery(1, 0);
        assertThat(query.size()).isEqualTo(20);
    }

    @DisplayName("给定 size > 100，构造时应修正为最大值 100")
    @Test
    void given_sizeGreaterThanMax_when_construct_then_sizeNormalizedToHundred() {
        PageQuery query = new PageQuery(1, 150);
        assertThat(query.size()).isEqualTo(100);
    }

    @DisplayName("给定有效 page 和 size，构造时应保持原值")
    @Test
    void given_validPageAndSize_when_construct_then_valuesUnchanged() {
        PageQuery query = new PageQuery(2, 50);
        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(50);
    }

    @DisplayName("给定边界值 page=1 size=1，offset 应返回 0")
    @Test
    void given_boundaryValues_when_offset_then_returnZero() {
        PageQuery query = new PageQuery(1, 1);
        assertThat(query.offset()).isEqualTo(0);
    }

    @DisplayName("给定 page=2 size=20，offset 应返回 20")
    @Test
    void given_pageTwoSizeTwenty_when_offset_then_returnCorrectValue() {
        PageQuery query = new PageQuery(2, 20);
        assertThat(query.offset()).isEqualTo(20);
    }

    @DisplayName("给定 page=3 size=10，offset 应返回 20")
    @Test
    void given_pageThreeSizeTen_when_offset_then_returnCorrectValue() {
        PageQuery query = new PageQuery(3, 10);
        assertThat(query.offset()).isEqualTo(20);
    }

    @DisplayName("给定 page 和 size，of() 静态方法应创建正确实例")
    @Test
    void given_pageAndSize_when_of_then_returnPageQuery() {
        PageQuery query = PageQuery.of(5, 30);
        assertThat(query.page()).isEqualTo(5);
        assertThat(query.size()).isEqualTo(30);
    }

    @DisplayName("给定非法参数，of() 静态方法也应触发校验修正")
    @Test
    void given_invalidParams_when_of_then_applyNormalization() {
        PageQuery query = PageQuery.of(-1, 200);
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(100);
    }
}
