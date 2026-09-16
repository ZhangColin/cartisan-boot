package com.cartisan.web.response;

import com.cartisan.web.request.Pagination;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResponse 单元测试")
class PageResponseTest {

    @Test
    @DisplayName("给定分页数据 - 构造 PageResponse - 返回正确分页响应")
    void given_pageData_when_construct_then_return_page_response() {
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
    @DisplayName("给定空列表 - 构造 PageResponse - 支持空列表")
    void given_emptyList_when_construct_then_support_empty_items() {
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
    @DisplayName("给定不同类型数据 - 构造 PageResponse - 支持泛型类型")
    void given_differentTypeData_when_construct_then_support_generic_types() {
        PageResponse<Integer> intResponse = new PageResponse<>(
                java.util.List.of(1, 2, 3),
                3L,
                1,
                10
        );

        assertThat(intResponse.items().get(0)).isInstanceOf(Integer.class);
    }

    @Nested
    @DisplayName("of(Page) 工厂（1-based 回显）")
    class OfFactory {

        @Test
        @DisplayName("Spring 0-based 页码回显为 1-based")
        void shouldEchoOneBasedPageNumber_whenBuiltFromPage() {
            PageImpl<String> page = new PageImpl<>(List.of("item1", "item2"), PageRequest.of(0, 10), 100);

            PageResponse<String> response = PageResponse.of(page);

            assertThat(response.items()).containsExactly("item1", "item2");
            assertThat(response.total()).isEqualTo(100);
            assertThat(response.page()).isEqualTo(1);
            assertThat(response.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("第二页（0-based 1）回显为 2")
        void shouldEchoSecondPageAsTwo() {
            PageImpl<String> page = new PageImpl<>(List.of(), PageRequest.of(1, 20), 100);

            assertThat(PageResponse.of(page).page()).isEqualTo(2);
        }

        @Test
        @DisplayName("请求页超出尾页时空列表并原样回显请求页码")
        void shouldEchoRequestedPage_whenBeyondLastPage() {
            PageImpl<String> page = new PageImpl<>(List.of(), PageRequest.of(99, 20), 0);

            PageResponse<String> response = PageResponse.of(page);

            assertThat(response.items()).isEmpty();
            assertThat(response.total()).isZero();
            assertThat(response.page()).isEqualTo(100);
            assertThat(response.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("page.map 转换内容后回显页码仍正确")
        void shouldKeepPageNumber_whenContentMapped() {
            PageImpl<Integer> page = new PageImpl<>(List.of(1, 2), PageRequest.of(4, 2), 100);

            PageResponse<String> response = PageResponse.of(page.map(i -> "item" + i));

            assertThat(response.items()).containsExactly("item1", "item2");
            assertThat(response.page()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("empty(Pagination) 工厂")
    class EmptyFactory {

        @Test
        @DisplayName("空行、总 0、回显请求页码与页大小")
        void shouldEchoPagination_whenEmpty() {
            Pagination pagination = new Pagination(3, 50, List.of());

            PageResponse<String> response = PageResponse.empty(pagination);

            assertThat(response.items()).isEmpty();
            assertThat(response.total()).isZero();
            assertThat(response.page()).isEqualTo(3);
            assertThat(response.size()).isEqualTo(50);
        }

        @Test
        @DisplayName("与手写空页字面量等价")
        void shouldEqualHandWrittenLiteral() {
            Pagination pagination = new Pagination(1, 20, List.of());

            PageResponse<String> empty = PageResponse.empty(pagination);
            PageResponse<String> literal = new PageResponse<>(List.of(), 0, 1, 20);

            assertThat(empty).isEqualTo(literal);
        }
    }
}
