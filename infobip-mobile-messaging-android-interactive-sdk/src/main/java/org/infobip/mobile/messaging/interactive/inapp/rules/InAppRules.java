package org.infobip.mobile.messaging.interactive.inapp.rules;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.app.ForegroundState;
import org.infobip.mobile.messaging.app.ForegroundStateMonitor;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.util.StringUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author sslavin
 * @since 15/04/2018.
 */
public class InAppRules {

    private final MobileInteractive mobileInteractive;
    private final ForegroundStateMonitor foregroundStateMonitor;

    public InAppRules(MobileInteractive mobileInteractive, ForegroundStateMonitor foregroundStateMonitor) {
        this.mobileInteractive = mobileInteractive;
        this.foregroundStateMonitor = foregroundStateMonitor;
    }

    public ShowOrNot shouldDisplayDialogFor(Message message) {
        if (StringUtils.isBlank(message.getCategory()) || message.isSilent()) {
            return ShowOrNot.not();
        }

        if (!hasInAppEnabled(message)) {
            return ShowOrNot.not();
        }

        NotificationCategory category = mobileInteractive.getNotificationCategory(message.getCategory());
        if (category == null || category.getNotificationActions() == null) {
            return ShowOrNot.not();
        }

        List<NotificationAction> actions = Arrays.asList(category.getNotificationActions());
        if (actions.size() == 0) {
            return ShowOrNot.not();
        }

        NotificationAction eligibleActions[] = filterActionsForInAppDialog(category.getNotificationActions());
        if (eligibleActions.length == 0) {
            return ShowOrNot.not();
        }

        ForegroundState state = foregroundStateMonitor.isInForeground();
        if (state.isForeground()) {
            return ShowOrNot.showNow(eligibleActions, state.getForegroundActivity());
        } else {
            return ShowOrNot.showWhenInForeground(eligibleActions);
        }
    }

    private NotificationAction[] filterActionsForInAppDialog(NotificationAction actions[]) {
        List<NotificationAction> as = new ArrayList<>();
        for (NotificationAction action : actions) {
            // remove "input" actions
            if (!action.hasInput()) {
                as.add(action);
            }
        }
        return as.toArray(new NotificationAction[as.size()]);
    }

    private static boolean hasInAppEnabled(Message message) {
        try {
            JSONObject json = new JSONObject(message.getInternalData());
            return json.getBoolean("inApp");
        } catch (Exception e) {
            return false;
        }
    }
}
