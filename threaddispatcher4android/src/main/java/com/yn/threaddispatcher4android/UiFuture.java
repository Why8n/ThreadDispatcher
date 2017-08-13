package com.yn.threaddispatcher4android;

import android.os.Handler;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Whyn on 2017/8/8.
 */

class UiFuture<T> implements Future<T>, Runnable {
    T value;
    private Handler handler;
    private Callable<T> action;
    volatile boolean isCancel;

    public UiFuture(Handler handler, Callable<T> action) {
        this.handler = handler;
        this.action = action;
    }

    public UiFuture() {
    }

    /**
     * @param mayInterruptIfRunning boolean: true if the thread executing this task should be
     *                              interrupted; otherwise, in-progress tasks are allowed to complete
     * @return
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (handler != null)
            handler.removeCallbacks(this);
        return isCancel = true;
    }

    @Override
    public boolean isCancelled() {
        return isCancel;
    }

    @Override
    public boolean isDone() {
        return isCancel;
    }

    @Override
    public synchronized T get(long l, @NonNull TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
        while (value == null)
            wait(timeUnit.toMillis(l));
        isCancel = true;
        return value;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        while (value == null)
            wait();
        isCancel = true;
        return value;
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                value = action.call();
                isCancel = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                notifyAll();
            }
        }
    }
}
