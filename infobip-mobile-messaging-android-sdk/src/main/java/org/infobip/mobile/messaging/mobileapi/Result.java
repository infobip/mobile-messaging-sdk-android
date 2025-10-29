/*
 * Result.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi;

public class Result<T, E extends MobileMessagingError> {

    private T data;
    private E error;

    public Result(T t) {
        data = t;
    }

    public Result(E e) {
        error = e;
    }

    public Result(T t, E e) {
        data = t;
        error = e;
    }

    public T getData() {
        return this.data;
    }

    public E getError() {
        return this.error;
    }

    public boolean isSuccess() {
        return error == null;
    }
}

