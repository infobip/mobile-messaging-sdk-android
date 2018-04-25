package org.infobip.mobile.messaging.app;

import android.app.Activity;

/**
 * @author sslavin
 * @since 24/04/2018.
 */
public class ForegroundState {

    private boolean foreground;
    private Activity foregroundActivity;

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
