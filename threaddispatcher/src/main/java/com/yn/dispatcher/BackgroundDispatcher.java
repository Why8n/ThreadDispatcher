package com.yn.dispatcher;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Whyn on 2017/8/7.
 */

final class BackgroundDispatcher implements Runnable {

    private final Switcher switcher;
    private ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean isRunning = false;

    public BackgroundDispatcher(Switcher switcher) {
        this.switcher = switcher;
    }

    public void enqueue(Runnable action) {
        //ignore new action when queue is full
        if (!taskQueue.offer(action))
            return;
        if (!isRunning) {
            isRunning = true;
            switcher.mBackgroundExecutor.execute(this);
        }
    }

    @Override
    public void run() {
        while (true) {
            Runnable action = taskQueue.poll();
            if (action == null) {
                isRunning = false;
                break;
            }
            action.run();
        }
    }
}
