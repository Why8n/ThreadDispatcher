package com.yn.dispatcher;


import com.yn.annotations.Switch;
import com.yn.exception.ThreadDispatcherException;
import com.yn.processor.MethodBase;
import com.yn.utils.Utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Switcher {

    private static Map<String, Object> sProxyCache = new ConcurrentHashMap<>();
    private static Switcher sInstance;
    ExecutorService mBackgroundExecutor;
    ExecutorService mUiExecutor;
    private MethodBase mSpeedUp;

    private BackgroundDispatcher mBackgroundDispatcher;

    private Switcher(Builder builder) {
        mBackgroundDispatcher = new BackgroundDispatcher(this);
        mBackgroundExecutor = builder.backgroudnExecutor;
        if (mBackgroundExecutor == null)
            mBackgroundExecutor = Executors.newCachedThreadPool();
        mUiExecutor = builder.uiExecutor;
        mSpeedUp = builder.speedUp;
    }

    public Switcher() {
        mBackgroundExecutor = Executors.newCachedThreadPool();
        mBackgroundDispatcher = new BackgroundDispatcher(this);
    }

    public static Switcher getDefault() {
        Switcher inst = sInstance;
        if (inst == null) {
            synchronized (Switcher.class) {
                inst = sInstance;
                if (inst == null) {
                    inst = new Switcher();
                    sInstance = inst;
                }
            }
        }
        return inst;
    }

    public Switcher async(Runnable action) {
        if (action != null)
            mBackgroundExecutor.execute(action);
        return this;
    }

    public <T> Future<T> async(Callable<T> action) {
        if (action != null)
            return mBackgroundExecutor.submit(action);
        return null;
    }

    public Switcher background(Runnable action) {
        if (action != null)
            mBackgroundDispatcher.enqueue(action);
        return this;
    }

    public <T> Future<T> background(Callable<T> action) {
        if (action != null)
            return mBackgroundExecutor.submit(action);
        return null;
    }

    public Switcher post(Runnable action) {
        if (action != null)
            action.run();
        return this;
    }

    public Switcher main(Runnable action) {
        if (mUiExecutor == null)
            throw new IllegalStateException("please use builder.setUiExecutor() " +
                    "to specific main thread executor before calling main()");
        if (action != null)
            mUiExecutor.execute(action);
        return this;
    }

    public <T> Future<T> main(Callable<T> action) {
        if (mUiExecutor == null)
            throw new IllegalStateException("please use builder.setUiExecutor() " +
                    "to specific main thread executor before calling main()");
        if (action != null)
            return mUiExecutor.submit(action);
        return null;
    }

    public <T> Object run(final String alias, final T target) {
        return run(alias, target, new Object[]{});
    }

    public <T> Object run(final String alias, final T target, final Object... args) {
        checkIndexConfigure();
        switch (mSpeedUp.mode(alias)) {
            case MAIN:
                return doMain(alias, target, args);
            case BACKGROUND:
                return doBackground(alias, target, args);
            case ASYNC:
                return doAsync(alias, target, args);
            case POST:
            default:
                return doPost(alias, target, args);
        }
    }

    private Runnable invokeRunnable(final String desc, final Object target, final Object[] args) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    mSpeedUp.invoke(desc, target, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private <T> Callable<Object> invokeCallable(final String desc, final T target, final Object[] args) {
        return new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return ((Future<Object>) mSpeedUp.invoke(desc, target, args)).get();
            }
        };
    }

    private void checkIndexConfigure() {
        if (mSpeedUp == null)
            throw new IllegalStateException("method index not configure yet." +
                    "please using Builder.setIndex() first");
    }

    public static Builder builder() {
        return new Builder();
    }

    public void shutdown() {
        if (mBackgroundExecutor != null)
            mBackgroundExecutor.shutdown();
        if (mUiExecutor != null)
            mUiExecutor.shutdown();
    }

    public <T> T create(final T delegate) {
        Utils.validateInterface(delegate);
        Class<?> clz  = delegate.getClass();
        Object proxyCache = sProxyCache.get(clz.getCanonicalName());
        do {
            if (proxyCache != null)
                break;
            proxyCache = Proxy.newProxyInstance(
                    clz.getClassLoader(),
                    clz.getInterfaces(),
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object o, Method method, Object[] args) throws Throwable {
                            return usingReflection(delegate, method, args);
                        }
                    });
            sProxyCache.put(clz.getCanonicalName(), proxyCache);
        } while (false);
        return (T) proxyCache;
    }

    private <T> Object usingReflection(T target, Method method, Object[] args)
            throws InvocationTargetException, IllegalAccessException,
            ExecutionException, InterruptedException {
        Switch threadMode = method.getAnnotation(Switch.class);
        if (threadMode == null)
            return method.invoke(target, args);
        switch (threadMode.threadMode()) {
            case MAIN:
                return doMain(target, method, args);
            case ASYNC:
                return doAsync(target, method, args);
            case BACKGROUND:
                return doBackground(target, method, args);
            case POST:
            default:
                return doPost(target, method, args);
        }
    }

    private <T> Object doPost(final String desc, final T target, final Object[] args) {
        try {
            return mSpeedUp.invoke(desc, target, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> Object doPost(T target, Method method, Object[] args) {
        return Utils.doMethod(target, method, args);
    }

    /**
     * @param method
     * @return return type is Futrue or void,return true
     */
    private boolean checkReturnType(Method method) {
        if (Utils.getRawType(method.getGenericReturnType()) != Future.class
                && method.getGenericReturnType() != void.class) {
            System.err.println(String.format("please note that you can't obtain return value " +
                    "if method's return type is not Future<T>"));
            return false;
        }
        return true;
    }

    private <T> Object doBackground(final String desc, final T target, final Object[] args) {
        if (mSpeedUp.isRetrunTypeFuture(desc))
            return background(invokeCallable(desc, target, args));
        return background(invokeRunnable(desc, target, args));
    }

    private <T> Object doBackground(final T target,
                                    final Method method,
                                    final Object[] args)
            throws ExecutionException, InterruptedException {
        if (!checkReturnType(method)) {
            background(Utils.doMethodInRunnable(target, method, args));
            return null;
        }
        return background(Utils.doMethodInCallable(target, method, args, true));
    }

    private <T> Object doAsync(final String desc, final T target, final Object[] args) {
        if (mSpeedUp.isRetrunTypeFuture(desc))
            return async(invokeCallable(desc, target, args));
        return async(invokeRunnable(desc, target, args));
    }

    private <T> Object doAsync(final T target, final Method method, final Object[] args) {
        if (!checkReturnType(method)) {
            async(Utils.doMethodInRunnable(target, method, args));
            return null;
        }
        return async(Utils.doMethodInCallable(target, method, args, true));
    }

    private <T> Object doMain(final String desc, final T target, final Object[] args) {
        if (mSpeedUp.isRetrunTypeFuture(desc))
            return main(invokeCallable(desc, target, args));
        return main(invokeRunnable(desc, target, args));
    }

    private <T> Object doMain(final T target, final Method method, final Object[] args) {
        if (!checkReturnType(method)) {
            main(Utils.doMethodInRunnable(target, method, args));
            return null;
        }
        return main(Utils.doMethodInCallable(target, method, args, true));
    }

    public static class Builder {
        private ExecutorService backgroudnExecutor;
        private ExecutorService uiExecutor;
        private MethodBase speedUp;

        public Builder setBackgroundExecutor(ExecutorService executor) {
            this.backgroudnExecutor = executor;
            return this;
        }

        public Builder setUiExecutor(ExecutorService executor) {
            this.uiExecutor = executor;
            return this;
        }

        public Builder setIndex(MethodBase speedUp) {
            this.speedUp = speedUp;
            return this;
        }

        public Switcher build() {
            return new Switcher(this);
        }

        public Switcher installDefaultThreadDispatcher() {
            synchronized (Switcher.class) {
                if (Switcher.sInstance != null)
                    throw new ThreadDispatcherException("Default instance already exists." +
                            " It may be only set once before it's used the first time to" +
                            " ensure consistent behavior.");
            }
            return Switcher.sInstance = build();
        }
    }
}
