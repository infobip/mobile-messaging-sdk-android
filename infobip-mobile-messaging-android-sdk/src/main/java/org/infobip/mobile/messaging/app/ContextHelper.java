package org.infobip.mobile.messaging.app;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;

/**
 * @author sslavin
 * @since 07/05/2018.
 */
public class ContextHelper {

    private final Context context;

    public ContextHelper(Context context) {
        this.context = context;
    }

    public Application getApplication() {
        if (context instanceof Activity) {
            return ((Activity) context).getApplication();
        } else if (context instanceof Service) {
            return ((Service) context).getApplication();
        } else if (context.getApplicationContext() instanceof Application) {
            return (Application) context.getApplicationContext();
        }
        return null;
    }
}
