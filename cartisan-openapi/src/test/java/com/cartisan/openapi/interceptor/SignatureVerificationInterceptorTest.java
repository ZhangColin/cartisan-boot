package com.cartisan.openapi.interceptor;

import com.cartisan.openapi.annotation.NoSignature;
import com.cartisan.openapi.annotation.RequireSignature;
import com.cartisan.openapi.filter.SignatureVerificationFilter;
import com.cartisan.openapi.provider.ApiKeyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignatureVerificationInterceptorTest {

    @Mock
    private ObjectMapper objectMapper;

    private SignatureVerificationInterceptor interceptor;

    private static final ApiKeyInfo API_KEY_INFO = new ApiKeyInfo(
            "app-1", "App One", "secret", "ACTIVE");

    @BeforeEach
    void setUp() {
        interceptor = new SignatureVerificationInterceptor(objectMapper);
    }

    // ---- Test controller with annotations ----

    static class TestController {
        public void noAnnotation() {}

        @NoSignature
        public void excludedMethod() {}

        @RequireSignature
        public void requireSignature() {}
    }

    @Test
    void shouldPassThrough_whenNoAnnotation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, handlerMethod("noAnnotation"));

        assertThat(result).isTrue();
    }

    @Test
    void shouldPassThrough_whenNoSignatureAnnotation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, handlerMethod("excludedMethod"));

        assertThat(result).isTrue();
    }

    @Test
    void shouldReject_whenRequireSignatureButNoApiKeyInfo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"code\":401,\"message\":\"Signature required\"}");

        boolean result = interceptor.preHandle(request, response, handlerMethod("requireSignature"));

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void shouldAllow_whenRequireSignatureWithValidApiKeyInfo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SignatureVerificationFilter.API_KEY_INFO_ATTR, API_KEY_INFO);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, handlerMethod("requireSignature"));

        assertThat(result).isTrue();
    }

    @Test
    void shouldPassThrough_whenNotHandlerMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    // ---- Helper ----

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }
}
