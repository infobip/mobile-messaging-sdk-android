package org.infobip.mobile.messaging.interactive.inapp.view;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import org.infobip.mobile.messaging.R;


/**
 * @author sslavin
 * @since 24/04/2018.
 */
public class ActivityWrapper {

    private final Activity activity;
    private final LayoutInflater layoutInflater;

    ActivityWrapper(Activity activity) {
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
    }

    public AlertDialog.Builder createAlertDialogBuilder(boolean useAppTheme) {
        return new AlertDialog.Builder(useAppTheme ? activity : new ContextThemeWrapper(activity, R.style.InAppDialog));
    }

    public View inflateView(int layoutResId) {
        return layoutInflater.inflate(layoutResId, null);
    }
}
