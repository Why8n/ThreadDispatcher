package com.yn.threaddispatcherdemo;

import com.yn.annotations.Switch;
import com.yn.enums.ThreadMode;

/**
 * Created by Whyn on 2017/8/7.
 */

public interface ITestSecond {
    @Switch(threadMode = ThreadMode.POST)
    void test();
}
