package com.cartisan.web.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cartisan.core.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RequestLogFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RequestLogFilter filter;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    // Shared test context
    private static final RequestContext TEST_CTX = new RequestContext(
            "test-request-id", "192.168.1.1",
            null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        filter = new RequestLogFilter();

        logger = (Logger) LoggerFactory.getLogger(RequestLogFilter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    /**
     * Run the test logic within a RequestContext scope.
     */
    private void runWithTestContext(Runnable test) {
        RequestContext.run(TEST_CTX, test);
    }

    @Test
    void shouldLogRequestWithBasicInfo() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getQueryString()).thenReturn(null);

        runWithTestContext(() -> {
            try {
                filter.doFilterInternal(request, response, filterChain);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(filterChain).doFilter(any(), any());
        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage()).contains("test-request-id");
        assertThat(logEvent.getFormattedMessage()).contains("GET");
        assertThat(logEvent.getFormattedMessage()).contains("/api/test");
    }

    @Test
    void shouldExcludeSwaggerPaths() throws Exception {
        String[] swaggerPaths = {"/swagger-ui/index.html", "/v3/api-docs", "/swagger-resources"};

        for (String path : swaggerPaths) {
            logAppender.list.clear();
            org.mockito.Mockito.reset(request, filterChain);

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");

            runWithTestContext(() -> {
                try {
                    filter.doFilterInternal(request, response, filterChain);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            verify(filterChain).doFilter(any(), any());
            assertThat(logAppender.list).isEmpty();
        }
    }

    @Test
    void shouldExcludeDruidPaths() throws Exception {
        String[] druidPaths = {"/druid/index.html", "/druid/api", "/druid/sql.html"};

        for (String path : druidPaths) {
            logAppender.list.clear();
            org.mockito.Mockito.reset(request, filterChain);

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");

            runWithTestContext(() -> {
                try {
                    filter.doFilterInternal(request, response, filterChain);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            verify(filterChain).doFilter(any(), any());
            assertThat(logAppender.list).isEmpty();
        }
    }

    @Test
    void shouldExcludeActuatorPaths() throws Exception {
        String[] actuatorPaths = {"/actuator/health", "/actuator/info", "/actuator/metrics"};

        for (String path : actuatorPaths) {
            logAppender.list.clear();
            org.mockito.Mockito.reset(request, filterChain);

            when(request.getRequestURI()).thenReturn(path);
            when(request.getMethod()).thenReturn("GET");

            runWithTestContext(() -> {
                try {
                    filter.doFilterInternal(request, response, filterChain);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            verify(filterChain).doFilter(any(), any());
            assertThat(logAppender.list).isEmpty();
        }
    }

    @Test
    void shouldLogRequestWithQueryString() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getQueryString()).thenReturn("page=1&size=10");

        runWithTestContext(() -> {
            try {
                filter.doFilterInternal(request, response, filterChain);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage()).contains("page=1&size=10");
    }
}
