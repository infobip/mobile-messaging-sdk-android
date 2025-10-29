/*
 * DepersonalizeActionListener.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.user;

public interface DepersonalizeActionListener {
    void onUserInitiatedDepersonalizeCompleted();

    void onUserInitiatedDepersonalizeFailed(Throwable error);
}