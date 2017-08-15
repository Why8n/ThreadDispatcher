package com.yn.threaddispatcherdemo;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.yn.annotations.Switch;
import com.yn.enums.ThreadMode;

import java.util.concurrent.TimeUnit;

/**
 * Created by Whyn on 2017/8/10.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Switch(alias = "log")
    public void log(String msg) {
        Log.i("Whyn111", msg);
    }

    @Switch(alias = "toast", threadMode = ThreadMode.MAIN)
    public void toast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Switch(alias = "sleep")
    public void sleep(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
