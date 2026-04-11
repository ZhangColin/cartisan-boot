package com.cartisan.core.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 自动传播 RequestContext 到线程池子任务的 ExecutorService 装饰器。
 *
 * <p>在 RequestContext bind 的作用域内，通过此 Executor 提交的任务
 * 能自动读取到当前线程的 RequestContext。</p>
 */
public class ContextAwareExecutor implements ExecutorService {

    private final ExecutorService delegate;

    public ContextAwareExecutor(ExecutorService delegate) {
        this.delegate = delegate;
    }

    /**
     * 将 Runnable 包装为自动传播 RequestContext 的版本。
     * 捕获当前线程的 RequestContext，在目标线程中绑定。
     */
    public static Runnable wrap(Runnable task) {
        RequestContext captured = RequestContext.CONTEXT.orElse(null);
        if (captured == null) {
            return task;
        }
        return () -> RequestContext.run(captured, task);
    }

    /**
     * 将 Callable 包装为自动传播 RequestContext 的版本。
     */
    public static <T> Callable<T> wrapCallable(Callable<T> task) {
        RequestContext captured = RequestContext.CONTEXT.orElse(null);
        if (captured == null) {
            return task;
        }
        return () -> RequestContext.runFor(captured, task);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(wrap(command));
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrapCallable(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrapAll(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(wrapAll(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapAll(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapAll(tasks), timeout, unit);
    }

    private <T> Collection<Callable<T>> wrapAll(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> wrapped = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrapped.add(wrapCallable(task));
        }
        return wrapped;
    }
}
