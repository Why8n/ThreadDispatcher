package com.yn.threaddispatcherdemo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.yn.dispatcher.Switcher;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class InterfaceMethodUsageActivity extends AppCompatActivity implements ITestInterface {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interface_usage);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.main_In_main)
    public void onMainInMain() {
        log("onMainInxx :run on thread.name = " + Thread.currentThread().getName());
        ITestInterface proxy = Switcher.getDefault().create(this);
        String result = proxy.doMain(null);
        if (result == null) {
            log("return type is not Future," +
                    "so you are unable to obtain the return value");
        } else {
            log("if you delete the annotation,then you can get the result:" + result);
        }
    }

    @OnClick(R.id.main_In_background)
    public void mainInBackground() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                onMainInMain();
            }
        }.start();
    }

    @OnClick(R.id.background_In_main)
    public void backgroundInMain() {
        log("backgroundInxx :run on thread.name = " + Thread.currentThread().getName());
        ITestInterface proxy = Switcher.getDefault().create(this);
        Future<String> result = proxy.doBackground(1);
        try {
            String returnValue = result.get(); //this will block until background thread done.
            log("backgroundInMain:return value = " + returnValue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.background_In_background)
    public void backgroundInBackground() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                backgroundInMain();
            }
        }.start();
    }

    @OnClick(R.id.async_In_main)
    public void asyncInMain() {
        log("asyncInxx :run on thread.name = " + Thread.currentThread().getName());
        ITestInterface proxy = Switcher.getDefault().create(this);
        proxy.doAsync(new String[]{"asyn1,aasnc2,asy3"});
    }

    @OnClick(R.id.async_In_async)
    public void asyncInAsync() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                asyncInMain();
            }
        }.start();
    }

    @OnClick(R.id.post_In_main)
    public void postInMain() {
        log("postInxx :run on thread.name = " + Thread.currentThread().getName());
        ITestInterface proxy = Switcher.getDefault().create(this);
        String result = proxy.doPost(0.0);
        log("post:return value = " + result);
    }

    @OnClick(R.id.post_In_background)
    public void postInBackground() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                postInMain();
            }
        }.start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public String doMain(List<String> test) {
        log("Interface::doMain --> thead.name = " + Thread.currentThread().getName());
        return "doMain";
    }

    @Override
    public Future<String> doBackground(int background) {
        log("Interface::doBackground --> thead.name = " + Thread.currentThread().getName());
        log("doBackground:: imitate time consuming");
        sleep(3);
        log("doBackground:: complete");
        return new Future<String>() {
            @Override
            public boolean cancel(boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public String get() throws InterruptedException, ExecutionException {
                return "doBackground";
            }

            @Override
            public String get(long l, @NonNull TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }

    @Override
    public void doAsync(String[] async) {
        log("Interface::doAsync --> thead.name = " + Thread.currentThread().getName());
        log("doAsync::imitate time consuming");
        sleep(10);
        log("doAsync:: complete");
    }

    @Override
    public String doPost(Double value) {
        log("Interface::doPost --> thead.name = " + Thread.currentThread().getName());
        return "doPost";
    }

    @Override
    public void test() {
        log("InterfaceSecond::test --> thead.name = " + Thread.currentThread().getName());
    }


    private void log(String msg) {
        Switcher.getDefault().run("log", this.getApplicationContext(), msg);
    }

    private void toast(final String msg) {
        Switcher.getDefault().main(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(InterfaceMethodUsageActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sleep(long seconds) {
        Switcher.getDefault().run("sleep", this.getApplicationContext(), seconds);
    }
}
