/*
 * Cryptor.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

import androidx.annotation.Nullable;

public abstract class Cryptor {

    @Nullable
    public abstract String encrypt(String data);

    @Nullable
    public abstract String decrypt(String encryptedBase64Data);
}
