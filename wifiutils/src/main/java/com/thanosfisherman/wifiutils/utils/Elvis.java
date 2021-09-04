package com.thanosfisherman.wifiutils.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.util.Consumer;

import java.util.Objects;

/**
 * Created by Thanos Psaridis on 19/9/2017.
 * Custom Elvis Operator an alternative to Optional Operator
 */

public final class Elvis<T> {
    @Nullable
    private final T mObject;

    private Elvis(@Nullable T t) {
        mObject = t;
    }

    private Elvis() {
        mObject = null;
    }

    public static <T> Elvis<T> of(T t) {
        return new Elvis<>(t);
    }

    @NonNull
    public static <T> Elvis<T> ofNonNull(@NonNull T t) {
        return new Elvis<>(Objects.requireNonNull(t, "SHOULD NOT BE NULL"));
    }

    public static <T> Elvis<T> empty() {
        return new Elvis<>();
    }

    public <S> Elvis<S> next(@NonNull Function<? super T, ? extends S> plumber) {
        return new Elvis<>(mObject == null ? null : plumber.apply(mObject));
    }

    @Nullable
    public T get() {
        return mObject;
    }

    public boolean getBoolean() {
        if (mObject == null)
            return false;
        else if (mObject instanceof Boolean)
            return (Boolean) mObject;
        return false;
    }

    public int getInt() {
        if (mObject == null)
            return 0;
        else if (mObject instanceof Integer)
            return (Integer) mObject;
        return 0;
    }

    public long getLong() {
        if (mObject == null)
            return 0;
        else if (mObject instanceof Long)
            return (Long) mObject;
        return 0;
    }

    public double getDouble() {
        if (mObject == null)
            return 0;
        else if (mObject instanceof Double)
            return (Double) mObject;
        return 0;
    }

    @NonNull
    public T orElse(@NonNull T other) {
        return mObject == null ? other : mObject;
    }

    public void ifPresent(@NonNull Consumer<? super T> consumer) {
        if (mObject != null)
            consumer.accept(mObject);
    }

    public boolean isPresent() {
        return this.mObject != null;
    }
}