package org.infobip.mobile.messaging.interactive.inapp.rules;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Size;

import org.infobip.mobile.messaging.interactive.NotificationAction;

/**
 * @author sslavin
 * @since 15/04/2018.
 */
public class ShowOrNot {
    private final Action action;
    private final NotificationAction[] actionsToDisplayFor;
    private final Activity baseActivityForDialog;

    public enum Action {
        DontShow,
        ShowNow,
        ShowWhenInForeground
    }
    private ShowOrNot(Action action, NotificationAction[] actionsToDisplayFor, Activity baseActivityForDialog) {
        this.action = action;
        this.actionsToDisplayFor = actionsToDisplayFor;
        this.baseActivityForDialog = baseActivityForDialog;
    }

    public static ShowOrNot not() {
        return new ShowOrNot(Action.DontShow, new NotificationAction[0], null);
    }

    public static ShowOrNot showNow(@NonNull @Size(min = 1) NotificationAction actions[], Activity activity) {
        return new ShowOrNot(Action.ShowNow, actions, activity);
    }

    public static ShowOrNot showWhenInForeground(@NonNull @Size(min = 1) NotificationAction actions[]) {
        return new ShowOrNot(Action.ShowWhenInForeground, actions, null);
    }

    public boolean shouldShowNow() {
        return action == Action.ShowNow;
    }

    public boolean shouldShowWhenInForeground() {
        return action == Action.ShowWhenInForeground;
    }

    public NotificationAction[] getActionsToShowFor() {
        return actionsToDisplayFor;
    }

    public Activity getBaseActivityForDialog() {
        return baseActivityForDialog;
    }
}
