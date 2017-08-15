package com.yn.threaddispatcherdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.yn.DispatcherIndex;
import com.yn.annotations.Switch;
import com.yn.dispatcher.Switcher;
import com.yn.enums.ThreadMode;
import com.yn.threaddispatcher4android.UiDispatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Whyn on 2017/8/10.
 */

public class
MethodAliasUsageActivity extends AppCompatActivity {

    private static final String ALIAS_RUN_PUBLIC = "runPublic";
    private static final String ALIAS_RUN_PROTECTED = "runProtected";
    private static final String ALIAS_RUN_PACKGAE = "runPackage";
    private static final String ALIAS_RUN_PRIVATE = "runPrivate";
    private static final String ALIAS_CALC_PUBLIC = "calcPublic";
    private static final String ALIAS_CALC_PRIVATE = "calcPrivate";
    private static final String ALIAS_RUN_STATIC_PUBLIC = "staticPublic";
    private static final String ALIAS_RUN_STATIC_PRIVATE = "staticPrivate";

    Switcher switcher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alias_usage);
        ButterKnife.bind(this);
        switcher = new Switcher.Builder()
                .setIndex(new DispatcherIndex())
                .setUiExecutor(new UiDispatcher())
                .build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        switcher.shutdown();
    }

    @OnClick(R.id.btnPublic)
    public void doPublic() {
        switcher.run(ALIAS_RUN_PUBLIC, this, 1);
    }

    @OnClick(R.id.btnProtectedMethod)
    public void doProtected() {
        switcher.run(ALIAS_RUN_PROTECTED, this, new Object[]{new String[]{"doProcted", "protected"}});
    }

    @OnClick(R.id.btnPackageMethod)
    public void doPackage() {
        switcher.run(ALIAS_RUN_PACKGAE, this, 8, "I am runPackage alias");
    }

    @OnClick(R.id.btnPrivateMethod)
    public void doPrivate() {
        switcher.run(ALIAS_RUN_PRIVATE, this);
    }

    private CountDownLatch countDownLatch;

    @OnClick(R.id.btnCalcPublicCost)
    public void doCalcPublic() {
        int count = (int) Math.pow(10, 4);
        countDownLatch = new CountDownLatch(count);
        long startTime = System.nanoTime();
        for (int i = 0; i < count; ++i) {
            switcher.run(ALIAS_CALC_PUBLIC, this);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();
        log(String.format("runPublic %d times,cost %f ms",
                count, (endTime - startTime) / Math.pow(10, 6)));
    }

    @OnClick(R.id.btnCalcPrivateCalc)
    public void doCalcPrivate() {
        int count = (int) Math.pow(10, 4);
        countDownLatch = new CountDownLatch(count);
        long startTime = System.nanoTime();
        for (int i = 0; i < count; ++i) {
            switcher.run(ALIAS_CALC_PRIVATE, this);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();
        log(String.format("runPirvate %d times,cost %f ms",
                count, (endTime - startTime) / Math.pow(10, 6)));
    }

    @OnClick(R.id.btnRunStaticPublicMethod)
    public void doStaticPublic() {
        int[][] intint = new int[][]{
                new int[]{1, 2, 3, 4, 5},
                new int[]{100, 300, 2343, 341324},
        };
        switcher.run(ALIAS_RUN_STATIC_PUBLIC, MethodAliasUsageActivity.class, new String[][]{}, intint);
    }

    @OnClick(R.id.btnRunStaticPrivateMethod)
    public void doStaticPrivate() {
        List<String> list = new LinkedList<>();
        list.add("asdfad");
        list.add("aaaaaaaaaaaaaaaaaaaa");

        Map<String, List<String>> maps = new LinkedHashMap<>();
        maps.put("withValue", list);
        maps.put("withoutValue", new ArrayList<String>());
        switcher.run(ALIAS_RUN_STATIC_PRIVATE, MethodAliasUsageActivity.class, list, maps);
    }

    @Switch(alias = ALIAS_RUN_STATIC_PUBLIC)
    public static void staticPublicMethod(String[][] strstr, int[][] intint) {
        Log.i("Whyn111", String.format("publicStaticMethod:threadMode = %s, Thread.name = %s",
                ThreadMode.BACKGROUND, Thread.currentThread().getName()));
        log(strstr);
        for (int i = 0; i < intint.length; ++i) {
            for (int j = 0; j < intint[i].length; ++j) {
                Log.i("Whyn111", String.format("arrarr[%d][%d] = %d", i, j, intint[i][j]));
            }
        }
    }

    @Switch(alias = ALIAS_RUN_STATIC_PRIVATE)
    private static <T, V> void staticPrivateMethod(List<T> list, Map<String, V> maps) {
        Log.i("Whyn111", String.format("privateStaticMethod:threadMode = %s, Thread.name = %s",
                ThreadMode.BACKGROUND, Thread.currentThread().getName()));
        Log.i("Whyn111", "list:" + list.toArray());
        int i = 0;
        for (Map.Entry<String, V> entry : maps.entrySet()) {
            Log.i("Whyn111", String.format("maps[%d] = [%s,%s]", i++, entry.getKey(), entry.getValue()));
        }
    }

    private static <T> void log(T[][] arrarr) {
        for (int i = 0; i < arrarr.length; ++i) {
            for (int j = 0; j < arrarr[i].length; ++j) {
                Log.i("Whyn111", String.format("arrarr[%d][%d] = %d", i, j, arrarr[i][j]));
            }
        }
    }

    @Switch(alias = ALIAS_RUN_PUBLIC, threadMode = ThreadMode.BACKGROUND)
    public void publicMethod(int i) {
        log(String.format("publicMethod:threadMode = %s, Thread.name = %s",
                ThreadMode.BACKGROUND, Thread.currentThread().getName()));
        log("publicMethod:params = " + i);
    }

    @Switch(alias = ALIAS_RUN_PROTECTED, threadMode = ThreadMode.ASYNC)
    protected void protectedMethod(String[] msgs) {
        log(String.format("protectedMethod:threadMode = %s, Thread.name = %s",
                ThreadMode.ASYNC, Thread.currentThread().getName()));
        log("protectedMethod:params = " + Arrays.toString(msgs));
    }

    @Switch(alias = ALIAS_RUN_PACKGAE, threadMode = ThreadMode.POST)
    void packageMethod(int arg0, String arg1) {
        log(String.format("packageMethod:threadMode = %s, Thread.name = %s",
                ThreadMode.POST, Thread.currentThread().getName()));
        log("packageMethod:arg0=" + arg0 + ",arg1=" + arg1);
    }

    @Switch(alias = ALIAS_RUN_PRIVATE, threadMode = ThreadMode.MAIN)
    private void privateMethod() {
        log(String.format("privateMethod:threadMode = %s, Thread.name = %s",
                ThreadMode.MAIN, Thread.currentThread().getName()));
    }

    @Switch(alias = ALIAS_CALC_PUBLIC, threadMode = ThreadMode.ASYNC)
    public void calcPublic() {
        countDownLatch.countDown();
        log(String.format("calcPublic:threadMode = %s, Thread.name = %s",
                ThreadMode.ASYNC, Thread.currentThread().getName()));
    }

    @Switch(alias = ALIAS_CALC_PRIVATE, threadMode = ThreadMode.ASYNC)
    private void calcPrivate() {
        countDownLatch.countDown();
        log(String.format("calcPrivate:threadMode = %s, Thread.name = %s",
                ThreadMode.ASYNC, Thread.currentThread().getName()));
    }

    private void log(String msg) {
        Log.i("Whyn111", msg);
    }
}
