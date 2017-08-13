package com.yn.threaddispatcher4android;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Whyn on 2017/8/7.
 */

abstract class UiDispatcherAdapter implements ExecutorService {

    @Override
    public abstract void execute(@NonNull Runnable runnable);

    @Override
    public abstract void shutdown();

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long l, @NonNull TimeUnit timeUnit) throws InterruptedException {
        return false;
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> callable) {
        return null;
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Runnable runnable, T t) {
        return null;
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable runnable) {
        return null;
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> collection) throws InterruptedException {
        return null;
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> collection, long l, @NonNull TimeUnit timeUnit) throws InterruptedException {
        return null;
    }

    @NonNull
    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> collection) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> collection, long l, @NonNull TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

}
