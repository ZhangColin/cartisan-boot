package com.cartisan.web.request;

import com.cartisan.core.exception.ApplicationException;
import com.cartisan.core.exception.BaseCodeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Ordering} 纯 JVM 单元测试（#29）。
 *
 * <p>覆盖排序 token 解析（属性/方向配对、缺省方向）、白名单防御，
 * 白名单语义与 {@link Pagination} 一致。</p>
 */
@DisplayName("Ordering 单元测试")
class OrderingTest {

    @Nested
    @DisplayName("token 解析")
    class Parse {

        @Test
        @DisplayName("属性 + 方向配对解析为 Sort.Order")
        void shouldParsePropertyWithDirection() {
            assertThat(new Ordering(List.of("createdAt,desc")).toSort())
                    .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("只有属性名时方向缺省为 ASC")
        void shouldDefaultDirectionToAsc_whenOnlyProperty() {
            assertThat(new Ordering(List.of("createdAt")).toSort())
                    .isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
        }

        @Test
        @DisplayName("方向关键词大小写不敏感")
        void shouldParseDirectionCaseInsensitively() {
            assertThat(new Ordering(List.of("createdAt", "DESC")).toSort())
                    .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("多组属性方向对全部解析、顺序保持")
        void shouldParseMultiplePairsInOrder() {
            assertThat(new Ordering(List.of("id,asc", "name,desc")).toSort())
                    .isEqualTo(Sort.by(Sort.Order.asc("id"), Sort.Order.desc("name")));
        }

        @Test
        @DisplayName("方向只作用于紧邻其前的属性")
        void shouldApplyDirectionToPrecedingPropertyOnly() {
            assertThat(new Ordering(List.of("a,desc", "b")).toSort())
                    .isEqualTo(Sort.by(Sort.Order.desc("a"), Sort.Order.asc("b")));
        }

        @Test
        @DisplayName("空 sort 转换为 unsorted")
        void shouldReturnUnsorted_whenSortEmpty() {
            assertThat(new Ordering(null).toSort().isSorted()).isFalse();
            assertThat(new Ordering(List.of()).toSort().isSorted()).isFalse();
        }
    }

    @Nested
    @DisplayName("白名单防御")
    class Whitelist {

        @Test
        @DisplayName("白名单内字段正常转换")
        void shouldConvertWithWhitelist_whenAllFieldsAllowed() {
            assertThat(new Ordering(List.of("createdAt,desc")).toSort(Set.of("createdAt", "id")))
                    .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("白名单外字段抛 ApplicationException（BAD_REQUEST 语义）")
        void shouldRejectFieldOutsideWhitelist() {
            assertThatThrownBy(() -> new Ordering(List.of("password,desc")).toSort(Set.of("createdAt", "id")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessage(BaseCodeMessage.BAD_REQUEST.message());
        }

        @Test
        @DisplayName("无方向 token 的属性同样受白名单约束")
        void shouldRejectBarePropertyOutsideWhitelist() {
            assertThatThrownBy(() -> new Ordering(List.of("password")).toSort(Set.of("createdAt", "id")))
                    .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("多字段中只要一个越界即整体拒绝")
        void shouldRejectWhenAnyFieldOutsideWhitelist() {
            assertThatThrownBy(() -> new Ordering(List.of("createdAt,desc", "password,asc"))
                    .toSort(Set.of("createdAt", "id")))
                    .isInstanceOf(ApplicationException.class);
        }
    }
}
