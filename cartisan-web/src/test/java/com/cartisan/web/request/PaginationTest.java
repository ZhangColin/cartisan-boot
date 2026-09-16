package com.cartisan.web.request;

import com.cartisan.core.exception.ApplicationException;
import com.cartisan.core.exception.BaseCodeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Pagination} 纯 JVM 单元测试（#29）。
 *
 * <p>覆盖 clamp 契约矩阵（含缺省与显式 0 的区分）、两种转换形态、
 * offset/limit 出口、排序白名单防御。只测外部行为，不测内部实现。</p>
 */
@DisplayName("Pagination 单元测试")
class PaginationTest {

    @Nested
    @DisplayName("clamp 契约（compact constructor 不变量）")
    class Clamp {

        @Test
        @DisplayName("page 小于 1 静默贴边到 1")
        void shouldClampPageTo1_whenPageLessThanOne() {
            assertThat(new Pagination(0, 20, null).page()).isEqualTo(1);
            assertThat(new Pagination(-3, 20, null).page()).isEqualTo(1);
        }

        @Test
        @DisplayName("size 小于 1 静默贴边到 1")
        void shouldClampSizeTo1_whenSizeLessThanOne() {
            assertThat(new Pagination(1, 0, null).size()).isEqualTo(1);
            assertThat(new Pagination(1, -5, null).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("size 超过上限静默截断到 100")
        void shouldClampSizeTo100_whenSizeExceedsMax() {
            assertThat(new Pagination(1, 101, null).size()).isEqualTo(100);
            assertThat(new Pagination(1, 1000, null).size()).isEqualTo(100);
            assertThat(new Pagination(1, 100, null).size()).isEqualTo(100);
        }

        @Test
        @DisplayName("参数缺省（null 组件）取默认值 page=1、size=20")
        void shouldApplyDefaults_whenComponentsNull() {
            Pagination pagination = new Pagination(null, null, null);

            assertThat(pagination.page()).isEqualTo(1);
            assertThat(pagination.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("显式传 0 与缺省可区分：0 走 clamp 到 1，而非默认值")
        void shouldClampExplicitZeroTo1_insteadOfDefault() {
            assertThat(new Pagination(0, 0, null).size()).isEqualTo(1);
            assertThat(new Pagination(0, 0, null).size()).isNotEqualTo(20);
        }
    }

    @Nested
    @DisplayName("sort 归一化")
    class SortNormalization {

        @Test
        @DisplayName("null sort 归一化为空列表")
        void shouldNormalizeNullSortToEmptyList() {
            assertThat(new Pagination(1, 20, null).sort()).isEmpty();
        }

        @Test
        @DisplayName("token 内逗号展平为扁平 token 流")
        void shouldFlattenCommaSeparatedTokens() {
            assertThat(new Pagination(1, 20, List.of("createdAt,desc")).sort())
                    .containsExactly("createdAt", "desc");
        }

        @Test
        @DisplayName("多值与单值混合输入展平为同一 token 流")
        void shouldFlattenMixedMultiValueTokens() {
            assertThat(new Pagination(1, 20, List.of("id,asc", "createdAt,desc")).sort())
                    .containsExactly("id", "asc", "createdAt", "desc");
        }

        @Test
        @DisplayName("空白 token 被丢弃")
        void shouldDropBlankTokens() {
            assertThat(new Pagination(1, 20, List.of(" createdAt ", "", "desc")).sort())
                    .containsExactly("createdAt", "desc");
        }
    }

    @Nested
    @DisplayName("toPageRequest 转换")
    class ToPageRequest {

        @Test
        @DisplayName("1-based 页码转换为 Spring 0-based PageRequest")
        void shouldConvertToZeroBasedPageRequest() {
            PageRequest pageRequest = new Pagination(3, 10, List.of("createdAt,desc")).toPageRequest();

            assertThat(pageRequest).isEqualTo(PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        @Test
        @DisplayName("无排序时转换为 unsorted 的 PageRequest")
        void shouldConvertWithoutSort_whenSortEmpty() {
            PageRequest pageRequest = new Pagination(1, 20, null).toPageRequest();

            assertThat(pageRequest).isEqualTo(PageRequest.of(0, 20));
            assertThat(pageRequest.getSort().isSorted()).isFalse();
        }

        @Test
        @DisplayName("多组属性方向对全部解析")
        void shouldParseMultiplePropertyDirectionPairs() {
            PageRequest pageRequest = new Pagination(1, 20, List.of("id,asc", "name,desc")).toPageRequest();

            assertThat(pageRequest.getSort()).isEqualTo(Sort.by(
                    Sort.Order.asc("id"),
                    Sort.Order.desc("name")));
        }

        @Test
        @DisplayName("白名单内字段正常转换")
        void shouldConvertWithWhitelist_whenAllFieldsAllowed() {
            PageRequest pageRequest = new Pagination(1, 20, List.of("createdAt,desc"))
                    .toPageRequest(Set.of("createdAt", "id"));

            assertThat(pageRequest.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("白名单外字段抛 ApplicationException（BAD_REQUEST 语义）")
        void shouldRejectSortFieldOutsideWhitelist() {
            assertThatThrownBy(() -> new Pagination(1, 20, List.of("password,desc"))
                    .toPageRequest(Set.of("createdAt", "id")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(BaseCodeMessage.BAD_REQUEST.message());
        }

        @Test
        @DisplayName("无方向 token 的属性同样受白名单约束")
        void shouldRejectBarePropertyOutsideWhitelist() {
            assertThatThrownBy(() -> new Pagination(1, 20, List.of("password"))
                    .toPageRequest(Set.of("createdAt", "id")))
                    .isInstanceOf(ApplicationException.class);
        }
    }

    @Nested
    @DisplayName("默认排序回退（toPageRequest(Sort)，#31）")
    class DefaultSortFallback {

        private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

        @Test
        @DisplayName("wire 未传排序（null 或空列表）时回退端点默认排序")
        void shouldApplyDefaultSort_whenWireSortEmpty() {
            assertThat(new Pagination(2, 10, null).toPageRequest(DEFAULT_SORT))
                    .isEqualTo(PageRequest.of(1, 10, DEFAULT_SORT));
            assertThat(new Pagination(2, 10, List.of()).toPageRequest(DEFAULT_SORT))
                    .isEqualTo(PageRequest.of(1, 10, DEFAULT_SORT));
        }

        @Test
        @DisplayName("wire 传了排序时以 wire 为准，默认排序不生效")
        void shouldPreferWireSort_whenWireSortPresent() {
            PageRequest pageRequest = new Pagination(1, 20, List.of("name,asc")).toPageRequest(DEFAULT_SORT);

            assertThat(pageRequest.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "name"));
        }

        @Test
        @DisplayName("null 默认排序 fail loud（编程错误，非缺省语义）")
        void shouldRejectNullDefaultSort() {
            assertThatThrownBy(() -> new Pagination(1, 20, null).toPageRequest((Sort) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("defaultSort");
        }
    }

    @Nested
    @DisplayName("offset / limit 出口（jOOQ 读侧）")
    class OffsetLimit {

        @Test
        @DisplayName("offset 为 (page-1)*size")
        void shouldComputeOffsetAsPageMinusOneTimesSize() {
            assertThat(new Pagination(3, 10, null).offset()).isEqualTo(20);
        }

        @Test
        @DisplayName("首页 offset 为 0")
        void shouldReturnZeroOffset_whenFirstPage() {
            assertThat(new Pagination(1, 10, null).offset()).isZero();
        }

        @Test
        @DisplayName("大页码 offset 用 long 承载不溢出")
        void shouldComputeLargeOffsetAsLong() {
            assertThat(new Pagination(Integer.MAX_VALUE, 100, null).offset())
                    .isEqualTo((long) (Integer.MAX_VALUE - 1) * 100);
        }

        @Test
        @DisplayName("limit 等于（已 clamp 的）size")
        void shouldReturnLimitEqualToSize() {
            assertThat(new Pagination(1, 50, null).limit()).isEqualTo(50);
            assertThat(new Pagination(1, 1000, null).limit()).isEqualTo(100);
        }
    }
}
