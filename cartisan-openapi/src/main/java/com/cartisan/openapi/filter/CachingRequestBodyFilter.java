package com.cartisan.openapi.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import com.cartisan.openapi.config.CartisanOpenapiProperties;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 缓存请求体 Filter，使请求体可多次读取（验签需要读取请求体计算 digest）。
 */
public class CachingRequestBodyFilter extends OncePerRequestFilter implements Ordered {

    private static final String BODY_TOO_LARGE_MESSAGE = "Request body too large";

    private final CartisanOpenapiProperties properties;

    public CachingRequestBodyFilter(CartisanOpenapiProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            long maxBytes = properties.getMaxBodySize().toBytes();

            // Check Content-Length header first (fast path)
            long contentLength = request.getContentLengthLong();
            if (contentLength > maxBytes) {
                sendBodyTooLarge(response);
                return;
            }

            // Read body and check actual size
            byte[] body = request.getInputStream().readAllBytes();
            if (body.length > maxBytes) {
                sendBodyTooLarge(response);
                return;
            }

            filterChain.doFilter(new CachedBodyHttpServletRequest(request, body), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void sendBodyTooLarge(jakarta.servlet.http.HttpServletResponse response) throws IOException {
        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType("text/plain");
        response.getWriter().write(BODY_TOO_LARGE_MESSAGE);
    }

    /**
     * 包装请求，使请求体可多次读取。
     */
    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.cachedBody = body;
        }

        public byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(cachedBody);
        }

        @Override
        public int getContentLength() {
            return cachedBody.length;
        }

        @Override
        public long getContentLengthLong() {
            return cachedBody.length;
        }
    }

    static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        CachedBodyServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return inputStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return inputStream.read(b, off, len);
        }
    }
}
