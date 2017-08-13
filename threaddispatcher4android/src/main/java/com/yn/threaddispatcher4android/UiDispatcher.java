package com.yn.threaddispatcher4android;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by Whyn on 2017/8/7.
 */

public class UiDispatcher extends UiDispatcherAdapter {
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper())
            action.run();
        else
            handler.post(action);
    }

    @Override
    public void shutdown() {
        handler.removeCallbacksAndMessages(null);
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> action) {
        final UiFuture<T> uiFuture = new UiFuture<T>(handler,action);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                uiFuture.value = action.call();
                uiFuture.isCancel = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            handler.post(uiFuture);
        }
        return uiFuture;
    }


}