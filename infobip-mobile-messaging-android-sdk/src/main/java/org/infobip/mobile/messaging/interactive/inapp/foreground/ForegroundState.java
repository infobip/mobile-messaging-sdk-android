/*
 * ForegroundState.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.foreground;

import android.app.Activity;

/**
 * @author sslavin
 * @since 24/04/2018.
 */
public class ForegroundState {

    private final boolean foreground;
    private final Activity foregroundActivity;

    private ForegroundState(boolean foreground, Activity foregroundActivity) {
        this.foreground = foreground;
        this.foregroundActivity = foregroundActivity;
    }

    public static ForegroundState background() {
        return new ForegroundState(false, null);
    }

    public static ForegroundState foreground(Activity activity) {
        return new ForegroundState(true, activity);
    }

    public Activity getForegroundActivity() {
        return foregroundActivity;
    }

    public boolean isForeground() {
        return foreground;
    }
}
