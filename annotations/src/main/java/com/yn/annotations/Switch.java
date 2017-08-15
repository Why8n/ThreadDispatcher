package com.yn.annotations;

import com.yn.enums.ThreadMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Whyn on 2017/8/7.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Switch {
    ThreadMode threadMode() default ThreadMode.POST;

    String alias() default "";
}

