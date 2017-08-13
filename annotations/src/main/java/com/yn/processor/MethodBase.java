package com.yn.processor;

import com.yn.enums.ThreadMode;

/**
 * Created by Whyn on 2017/8/9.
 */

public interface MethodBase {
    <T> Object invoke(String desc, T target, Object[] args) throws Exception;

    ThreadMode mode(String desc);

    boolean isRetrunTypeFuture(String desc);
}
