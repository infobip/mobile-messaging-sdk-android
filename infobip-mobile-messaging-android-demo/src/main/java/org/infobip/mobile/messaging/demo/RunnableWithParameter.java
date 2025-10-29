/*
 * RunnableWithParameter.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

/**
 * @author sslavin
 * @since 13/11/2017.
 */

interface RunnableWithParameter<T> {
    void run(T param);
}
