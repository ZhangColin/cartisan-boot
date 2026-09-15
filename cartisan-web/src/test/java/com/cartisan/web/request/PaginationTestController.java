package com.cartisan.web.request;

import com.cartisan.web.response.PageResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分页参数绑定集成测试用 Controller（#29）。
 *
 * <p>暴露四类端点供 MockMvc 断言：Pagination 扁平绑定、白名单转换、
 * Ordering 独立绑定、PageResponse.of 超尾页回显。</p>
 */
@RestController
@RequestMapping("/pagination-test")
public class PaginationTestController {

    /** 本 fixture 允许排序的白名单字段 */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "id");

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
        return ordering.toSort().stream()
                .map(order -> order.getProperty() + ":" + order.getDirection())
                .toList();
    }

    @GetMapping("/page-response")
    public PageResponse<String> pageResponse() {
        return PageResponse.of(new PageImpl<>(List.of(), PageRequest.of(99, 20), 0));
    }
}
