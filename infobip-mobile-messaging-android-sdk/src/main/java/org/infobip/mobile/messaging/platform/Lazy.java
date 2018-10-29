package org.infobip.mobile.messaging.platform;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.lang.reflect.Constructor;

/**
 * @author sslavin
 * @since 03/09/2018.
 */
public class Lazy<T, P> {

    public interface Initializer<T, P> {
        T initialize(P param);
    }

    private final Initializer<T, P> initializer;
    private final Object lock = new Object();
    private volatile T value = null;

    private Lazy(Initializer<T, P> initializer) {
        this.initializer = initializer;
    }

    @NonNull
    public final T get(P param) {
        if (value == null) {
            synchronized (lock) {
                if (value == null) {
                    value = initializer.initialize(param);
                }
            }
        }
        return value;
    }

    public static <T, P> Lazy<T, P> fromSingleArgConstructor(final Class<T> cls, final Class<P> argCls) {
        final Constructor<T> constructor;
        try {
            constructor = cls.getDeclaredConstructor(argCls);
            constructor.setAccessible(true);
        } catch (Exception e) {
            MobileMessagingLogger.e("Class " + cls.getName() + " does not have appropriate constructor that accepts " + argCls.getName(), e);
            throw new RuntimeException(e);
        }
        return new Lazy<>(new Initializer<T, P>() {
            @Override
            public T initialize(P param) {
                try {
                    return constructor.newInstance(param);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static <T, P> Lazy<T, P> create(Initializer<T, P> initializer) {
        return new Lazy<>(initializer);
    }

    public static <T, P> Lazy<T, P> just(final T value) {
        return new Lazy<>(new Lazy.Initializer<T, P>() {
            @Override
            public T initialize(P param) {
                return value;
            }
        });
    }
}
