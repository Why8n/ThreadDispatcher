package com.yn.threaddispatcherdemo;

import com.yn.annotations.Switch;

import java.util.List;
import java.util.concurrent.Future;

import static com.yn.enums.ThreadMode.ASYNC;
import static com.yn.enums.ThreadMode.BACKGROUND;
import static com.yn.enums.ThreadMode.MAIN;
import static com.yn.enums.ThreadMode.POST;

/**
 * Created by Whyn on 2017/8/7.
 */


public interface ITestInterface extends ITestSecond {

    @Switch(threadMode = MAIN, alias = "domain")
    String doMain(List<String> test);

    @Switch(threadMode = BACKGROUND, alias = "doback")
    Future<String> doBackground(int background);

    @Switch(threadMode = ASYNC, alias = "doasync")
    void doAsync(String[] async);

    @Switch(threadMode = POST, alias = "dopos")
    String doPost(Double value);
}
