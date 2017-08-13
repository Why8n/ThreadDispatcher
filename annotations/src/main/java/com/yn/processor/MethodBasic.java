package com.yn.processor;

import com.yn.enums.ThreadMode;

/**
 * Created by Whyn on 2017/8/9.
 */

public class MethodBasic {
    public final boolean isPublic;
    public final String name;
    public final boolean hasReturn;
    public final ThreadMode mode;
    public final boolean isReturnTypeFuture;
    public final String[] paramsType;
    public final boolean isStatic;

    public MethodBasic(String name,
                       ThreadMode mode,
                       boolean isPublic,
                       boolean hasReturn,
                       boolean isReturnTypeFuture,
                       String[] paramsType,
                       boolean isStatic) {
        this.isPublic = isPublic;
        this.name = name;
        this.mode = mode;
        this.hasReturn = hasReturn;
        this.isReturnTypeFuture = isReturnTypeFuture;
        this.paramsType = paramsType;
        this.isStatic = isStatic;
    }
}
