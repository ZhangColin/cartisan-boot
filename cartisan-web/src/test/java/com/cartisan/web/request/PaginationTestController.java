package com.cartisan.web.request;

import com.cartisan.web.response.PageResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分页参数绑定集成测试用 Controller（#29）。
 *
 * <p>供 MockMvc 断言的端点：Pagination 扁平绑定、白名单转换、Ordering 独立绑定
 * （含白名单）、业务 Query 与 Pagination 并列组合、PageResponse.of 超尾页回显。</p>
 */
@RestController
@RequestMapping("/pagination-test")
public class PaginationTestController {

    /** 本 fixture 允许排序的白名单字段 */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "id");

    /** 业务过滤条件 Query（模拟 @Condition 查询 record，与分页参数并列） */
    record SearchQuery(String keyword, String status) {
    }

    @GetMapping("/list")
    public Map<String, Object> list(Pagination pagination) {
        return Map.of(
                "page", pagination.page(),
                "size", pagination.size(),
                "sort", pagination.sort(),
                "offset", pagination.offset(),
                "limit", pagination.limit());
    }

    @GetMapping("/whitelisted")
    public Map<String, Object> whitelisted(Pagination pagination) {
        PageRequest pageRequest = pagination.toPageRequest(ALLOWED_SORT_FIELDS);
        return Map.of(
                "pageNumber", pageRequest.getPageNumber(),
                "pageSize", pageRequest.getPageSize());
    }

    @GetMapping("/ordering")
    public List<String> ordering(Ordering ordering) {
        return toOrderList(ordering.toSort());
    }

    @GetMapping("/ordering-whitelisted")
    public List<String> orderingWhitelisted(Ordering ordering) {
        return toOrderList(ordering.toSort(ALLOWED_SORT_FIELDS));
    }

    /**
     * 业务 Query 与 Pagination 并列组合（查询端组合契约）：两类 record 各自
     * 按组件名绑定顶级参数，互不干扰、零注解零嵌套。
     */
    @GetMapping("/search")
    public Map<String, Object> search(SearchQuery query, Pagination pagination) {
        return Map.of(
                "keyword", query.keyword() == null ? "" : query.keyword(),
                "status", query.status() == null ? "" : query.status(),
                "page", pagination.page(),
                "size", pagination.size());
    }

    @GetMapping("/page-response")
    public PageResponse<String> pageResponse() {
        return PageResponse.of(new PageImpl<>(List.of(), PageRequest.of(99, 20), 0));
    }

    private static List<String> toOrderList(Sort sort) {
        return sort.stream()
                .map(order -> order.getProperty() + ":" + order.getDirection())
                .toList();
    }
}
