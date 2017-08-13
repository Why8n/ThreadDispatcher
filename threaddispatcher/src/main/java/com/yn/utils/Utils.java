package com.yn.utils;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by Whyn on 2017/8/7.
 */

public final class Utils {
    private Utils() {
    }

    public static <T> void validateInterface(Class<T> clz) {
        if (!clz.isInterface())
            throw new IllegalArgumentException("API declaractions must be interfaces.");
        if (clz.getInterfaces().length > 0)
            throw new IllegalArgumentException("API interfaces must not extend other interfaces.");
    }

    public static void validateInterface(Object obj) {
        if (obj.getClass().getInterfaces().length <= 0)
            throw new IllegalArgumentException(
                    String.format("%s must implement interfaces.", obj.getClass()));
    }


    /**
     * Extract the raw class type from {@code type}. For example, the type representing
     * {@code List<? extends Runnable>} returns {@code List.class}.
     */
    public static Class<?> getRawType(Type type) {
        checkNotNull(type, "type == null");

        if (type instanceof Class<?>) {
            // Type is a normal class.
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
            // suspects some pathological case related to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class)) throw new IllegalArgumentException();
            return (Class<?>) rawType;
        }
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();
        }
        if (type instanceof TypeVariable) {
            // We could use the variable's bounds, but that won't work if there are multiple. Having a raw
            // type that's more general than necessary is okay.
            return Object.class;
        }
        if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);
        }

        throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
                + "GenericArrayType, but <" + type + "> is of type " + type.getClass().getName());
    }

    public static Type getActualType(Type returnType) {
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
        } else {
            return getParameterUpperBound(0, (ParameterizedType) returnType);
        }
    }

    private static Type getParameterUpperBound(int index, ParameterizedType type) {
        Type[] types = type.getActualTypeArguments();
        if (index >= 0 && index < types.length) {
            Type paramType = types[index];
            return paramType instanceof WildcardType ? ((WildcardType) paramType).getUpperBounds()[0] : paramType;
        } else {
            throw new IllegalArgumentException("Index " + index + " not in range [0," + types.length + ") for " + type);
        }
    }


    static <T> T checkNotNull(T object, String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }

    public static <T> Object doMethod(T target, Method method, Object[] args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> Runnable doMethodInRunnable(final T target,
                                                  final Method method,
                                                  final Object[] args) {
        return new Runnable() {
            @Override
            public void run() {
                doMethod(target, method, args);
            }
        };
    }

    public static <T> Callable<Object> doMethodInCallable(final T target,
                                                          final Method method,
                                                          final Object[] args,
                                                          final boolean isFuture) {
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return !isFuture ? method.invoke(target, args) :
                        ((Future<Object>) method.invoke(target, args)).get();
            }
        };
    }

}
