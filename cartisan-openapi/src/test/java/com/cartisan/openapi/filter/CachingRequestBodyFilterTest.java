package com.cartisan.openapi.filter;

import com.cartisan.openapi.config.CartisanOpenapiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class CachingRequestBodyFilterTest {

    private static final int MAX_BODY_SIZE = 1024;

    private CartisanOpenapiProperties properties;
    private CachingRequestBodyFilter filter;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        properties = new CartisanOpenapiProperties();
        properties.setMaxBodySize(org.springframework.util.unit.DataSize.ofBytes(MAX_BODY_SIZE));
        filter = new CachingRequestBodyFilter(properties);
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    void shouldRejectRequest_whenBodyExceedsMaxSize() throws Exception {
        byte[] largeBody = new byte[MAX_BODY_SIZE + 1];
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(largeBody);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).isEqualTo("Request body too large");
    }

    @Test
    void shouldRejectRequest_whenChunkedBodyExceedsMaxSize() throws Exception {
        byte[] largeBody = new byte[MAX_BODY_SIZE + 1];
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(largeBody);
        // No Content-Length header set (simulates chunked transfer)

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).isEqualTo("Request body too large");
    }

    @Test
    void shouldAllowRequest_whenBodyWithinLimit() throws Exception {
        byte[] smallBody = new byte[MAX_BODY_SIZE];
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(smallBody);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
