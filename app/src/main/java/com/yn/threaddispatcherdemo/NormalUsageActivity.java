package com.yn.threaddispatcherdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.yn.dispatcher.Switcher;

import java.util.concurrent.CountDownLatch;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Whyn on 2017/8/10.
 */

public class NormalUsageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.normal_usage);
        ButterKnife.bind(this);
    }


    @OnClick(R.id.testNormalDispatchers)
    public void onNormalDispatchers() {
        Switcher.getDefault().main(new Runnable() {
            @Override
            public void run() {
                //this will run on main thread
                String msg = "main in main thread--> thread.name = " + Thread.currentThread().getName();
                log(msg);
                toast(msg);
            }
        }).post(new Runnable() {
            @Override
            public void run() {
                //this will run on post thread
                String msg = "post in main thread --> thread.name = " + Thread.currentThread().getName();
                log(msg);
                toast(msg);
            }
        }).background(new Runnable() {
            @Override
            public void run() {
                //this will run on background thread
                String msg = "background in main thread --> thread.name = " + Thread.currentThread().getName();
                log(msg);
                toast(msg);
            }
        }).async(new Runnable() {
            @Override
            public void run() {
                //this will run on async thread
                String msg = "async in main thread --> thread.name = " + Thread.currentThread().getName();
                log(msg);
                toast(msg);
            }
        });
    }

    private boolean testAsync = false;

    @OnClick(R.id.testBackgroudnAsync)
    public void testBackgroudnAsync() {
        if (testAsync)
            testAsync();
        else
            testBackground();
        testAsync = !testAsync;
    }

    private int testCount = 1000;

    private void testAsync() {
        final CountDownLatch countDownLatch = new CountDownLatch(testCount);
        log("strat testing async");
        for (int i = 0; i < testCount; ++i) {
            final int id = i;
            Switcher.getDefault().async(new Runnable() {
                @Override
                public void run() {
                    log(String.format("async :: num = %d,thread.name = %s",
                            id, Thread.currentThread().getName()));
                    countDownLatch.countDown();
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            log("end testing async");
        }
    }

    private void testBackground() {
        final CountDownLatch countDownLatch = new CountDownLatch(testCount);
        log("strat testing background");
        for (int i = 0; i < testCount; ++i) {
            final int id = i;
            Switcher.getDefault().background(new Runnable() {
                @Override
                public void run() {
                    log(String.format("background :: num = %d,thread.name = %s",
                            id, Thread.currentThread().getName()));
                    countDownLatch.countDown();
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            log("end testing background");
        }
    }

    private void log(String msg) {
//        run application.log method
        Switcher.getDefault().run("log", NormalUsageActivity.this.getApplicationContext(), msg);
    }

    private void toast(String msg) {
        //run application.toast method
        Switcher.getDefault().run("toast", NormalUsageActivity.this.getApplicationContext(), this, msg);
    }

}
