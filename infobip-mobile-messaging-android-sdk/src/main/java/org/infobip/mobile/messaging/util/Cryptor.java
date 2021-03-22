package org.infobip.mobile.messaging.util;

import android.support.annotation.Nullable;

public abstract class Cryptor {

    @Nullable
    public abstract String encrypt(String data);

    @Nullable
    public abstract String decrypt(String encryptedBase64Data);
}
