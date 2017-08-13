package com.yn.exception;

/**
 * Created by Whyn on 2017/8/7.
 */

public class ThreadDispatcherException extends RuntimeException {
    public ThreadDispatcherException(String s) {
        super(s);
    }

    public ThreadDispatcherException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ThreadDispatcherException(Throwable throwable) {
        super(throwable);
    }

    protected ThreadDispatcherException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
