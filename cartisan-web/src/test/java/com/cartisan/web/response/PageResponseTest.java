package com.cartisan.web.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResponse 单元测试")
class PageResponseTest {

    @Test
    @DisplayName("应该构造分页响应")
    void should_construct_page_response() {
        PageResponse<String> response = new PageResponse<>(
                java.util.List.of("item1", "item2"),
                100L,
                1,
                10
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0)).isEqualTo("item1");
        assertThat(response.total()).isEqualTo(100L);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("应该支持空列表")
    void should_support_empty_items() {
        PageResponse<String> response = new PageResponse<>(
                java.util.List.of(),
                0L,
                1,
                10
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isEqualTo(0L);
    }

    @Test
    @DisplayName("应该支持泛型类型")
    void should_support_generic_types() {
        PageResponse<Integer> intResponse = new PageResponse<>(
                java.util.List.of(1, 2, 3),
                3L,
                1,
                10
        );

        assertThat(intResponse.items().get(0)).isInstanceOf(Integer.class);
    }
}
