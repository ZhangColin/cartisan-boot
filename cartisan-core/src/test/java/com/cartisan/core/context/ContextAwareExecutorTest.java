package com.cartisan.core.context;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAwareExecutorTest {

    @Test
    void shouldPropagateContextToChildThread() throws Exception {
        RequestContext ctx = new RequestContext(
                "req-1", "10.0.0.1",
                "app-1", "AppOne",
                42L, "Alice",
                100L, "TenantX");

        ExecutorService delegate = Executors.newFixedThreadPool(1);
        ContextAwareExecutor executor = new ContextAwareExecutor(delegate);

        try {
            Future<String> future = RequestContext.runFor(ctx, () -> {
                Future<String> childFuture = executor.submit(() -> {
                    return RequestContext.getRequestId() + ":" + RequestContext.getUserId();
                });
                return childFuture;
            });

            String result = future.get(5, TimeUnit.SECONDS);
            assertThat(result).isEqualTo("req-1:42");
        } finally {
            delegate.shutdown();
        }
    }

    @Test
    void shouldReturnNullWhenNoContext() throws Exception {
        ExecutorService delegate = Executors.newFixedThreadPool(1);
        ContextAwareExecutor executor = new ContextAwareExecutor(delegate);

        try {
            Future<String> future = executor.submit(() -> {
                return String.valueOf(RequestContext.getRequestId());
            });

            String result = future.get(5, TimeUnit.SECONDS);
            assertThat(result).isEqualTo("null");
        } finally {
            delegate.shutdown();
        }
    }

    @Test
    void shouldWrapRunnableWithContext() throws Exception {
        RequestContext ctx = new RequestContext(
                "req-wrapped", "10.0.0.1",
                null, null, null, null, null, null);

        String[] captured = new String[1];

        RequestContext.run(ctx, () -> {
            Runnable wrapped = ContextAwareExecutor.wrap(() -> {
                captured[0] = RequestContext.getRequestId();
            });

            // Execute in another thread
            new Thread(wrapped).start();
        });

        Thread.sleep(200);
        assertThat(captured[0]).isEqualTo("req-wrapped");
    }
}
