package org.infobip.mobile.messaging.chat.utils;

import android.os.Build;

public class CommonUtils {
    private CommonUtils() {
    }

    public static boolean isOSOlderThanKitkat() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
    }
}
