/*
 * ShowOrNot.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.rules;

import android.app.Activity;

import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

import androidx.annotation.NonNull;
import androidx.annotation.Size;

/**
 * @author sslavin
 * @since 15/04/2018.
 */
public class ShowOrNot {
    private final Action action;
    private final NotificationCategory category;
    private final NotificationAction[] actionsToDisplayFor;
    private final Activity baseActivityForDialog;

    public enum Action {
        DontShow,
        ShowNow,
        ShowWhenInForeground
    }

    private ShowOrNot(Action action, NotificationCategory category, NotificationAction[] actionsToDisplayFor, Activity baseActivityForDialog) {
        this.action = action;
        this.category = category;
        this.actionsToDisplayFor = actionsToDisplayFor;
        this.baseActivityForDialog = baseActivityForDialog;
    }

    public static ShowOrNot not() {
        return new ShowOrNot(Action.DontShow, null, new NotificationAction[0], null);
    }

    public static ShowOrNot showNowWithDefaultActions(Activity activity, @NonNull @Size(min = 1) NotificationAction[] defaultActions) {
        return new ShowOrNot(Action.ShowNow, null, defaultActions, activity);
    }

    public static ShowOrNot showNow(NotificationCategory category, @NonNull @Size(min = 1) NotificationAction[] actions, Activity activity) {
        return new ShowOrNot(Action.ShowNow, category, actions, activity);
    }

    public static ShowOrNot showWhenInForeground() {
        return new ShowOrNot(Action.ShowWhenInForeground, null, new NotificationAction[0], null);
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

    public NotificationCategory getCategory() {
        return category;
    }

    public Activity getBaseActivityForDialog() {
        return baseActivityForDialog;
    }
}
