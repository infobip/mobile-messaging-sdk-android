package org.infobip.mobile.messaging.interactive;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author tjuric
 * @since 04/08/17.
 */
public class MobileInteractiveImpl extends MobileInteractive implements MessageHandlerModule {


    private static MobileInteractiveImpl instance;
    private Context context;
    private final JsonSerializer serializer = new JsonSerializer(false);
    private Set<NotificationCategory> customNotificationCategories;
    private Set<NotificationCategory> predefinedNotificationCategories;
    private MobileMessagingCore mobileMessagingCore;


    public MobileInteractiveImpl() {
    }

    @VisibleForTesting
    public MobileInteractiveImpl(Context context, MobileMessagingCore mobileMessagingCore) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
    }

    public static MobileInteractiveImpl getInstance(Context context) {
        if (instance != null) {
            return instance;
        }

        instance = MobileMessagingCore.getInstance(context).getInteractiveMessageHandlerModule();
        return instance;
    }

    @Override
    public void setNotificationCategories(NotificationCategory... notificationCategories) {
        validateWithParam(notificationCategories);
        this.predefinedNotificationCategories = getPredefinedNotificationCategories();
        setCustomNotificationCategories(notificationCategories);
    }

    @Override
    public NotificationCategory getNotificationCategory(String categoryId) {
        if (StringUtils.isBlank(categoryId)) {
            return null;
        }

        Set<NotificationCategory> storedNotificationCategories = getNotificationCategories();
        if (storedNotificationCategories == MobileMessagingProperty.INTERACTIVE_CATEGORIES.getDefaultValue()) {
            return null;
        }

        for (NotificationCategory notificationCategory : storedNotificationCategories) {
            if (categoryId.equals(notificationCategory.getCategoryId())) {
                return notificationCategory;
            }
        }

        return null;
    }

    @Override
    public void triggerSdkActionsFor(String categoryId, NotificationAction action, Message message) {
        markAsSeen(context, message.getMessageId());
        sendMo(context, categoryId, action, message);
    }

    void markAsSeen(Context context, String messageId) {
        NotificationSettings notificationSettings = mobileMessagingCore(context).getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }
        if (notificationSettings.markSeenOnTap()) {
            mobileMessagingCore(context).setMessagesSeen(messageId);
        }
    }

    void sendMo(Context context, String categoryId, NotificationAction action, Message initialMessage) {
        if (!action.sendsMoMessage()) {
            return;
        }

        mobileMessagingCore(context).sendMessagesWithRetry(messageFor(categoryId, action, initialMessage));
    }

    private Message messageFor(final String categoryId, final NotificationAction action, final Message initialMessage) {
        Message message = new Message();
        message.setBody(categoryId + " " + action.getId());
        HashMap<String, String> map = new HashMap<>();
        map.put("initialMessageId", initialMessage.getMessageId());
        message.setInternalData(InternalDataMapper.mergeExistingInternalDataWithAnythingToJson(initialMessage.getInternalData(), map));
        return message;
    }

    public Set<NotificationCategory> getNotificationCategories() {
        if (!isDisplayNotificationEnabled()) {
            return null;
        }

        Set<NotificationCategory> notificationCategories = getPredefinedNotificationCategories();
        Set<NotificationCategory> customNotificationCategories = getCustomNotificationCategories();
        notificationCategories.addAll(customNotificationCategories);

        return notificationCategories;
    }

    private Set<NotificationCategory> getPredefinedNotificationCategories() {
        if (null != predefinedNotificationCategories) {
            return predefinedNotificationCategories;
        }

        return PredefinedNotificationCategories.load();
    }

    void setCustomNotificationCategories(NotificationCategory[] notificationCategories) {
        if (notificationCategories == null) {
            return;
        }

        if (notificationCategories.length == 0) {
            this.customNotificationCategories = null;
        } else {
            this.customNotificationCategories = new HashSet<>(Arrays.asList(notificationCategories));
        }

        final Set<String> customNotificationCategoriesStringSet = new HashSet<>();
        for (NotificationCategory customNotificationCategory : notificationCategories) {
            customNotificationCategoriesStringSet.add(customNotificationCategory.toString());
        }
        PreferenceHelper.saveStringSet(context, MobileMessagingProperty.INTERACTIVE_CATEGORIES, customNotificationCategoriesStringSet);
    }

    @NonNull
    private Set<NotificationCategory> getCustomNotificationCategories() {
        if (null != customNotificationCategories) {
            return customNotificationCategories;
        }

        Set<String> notificationCategoriesStringSet = PreferenceHelper.findStringSet(context, MobileMessagingProperty.INTERACTIVE_CATEGORIES);
        Set<NotificationCategory> notificationCategoriesTemp = new HashSet<>();
        if (notificationCategoriesStringSet != MobileMessagingProperty.INTERACTIVE_CATEGORIES.getDefaultValue()) {
            for (String category : notificationCategoriesStringSet) {
                NotificationCategory notificationCategory = serializer.deserialize(category, NotificationCategory.class);
                notificationCategoriesTemp.add(notificationCategory);
            }
        }
        this.customNotificationCategories = notificationCategoriesTemp;

        return customNotificationCategories;
    }

    private boolean isDisplayNotificationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED);
    }

    private void validateWithParam(Object o) {
        if (null != o) {
            return;
        }
        throw new IllegalArgumentException("Can't use 'set' method with null argument!");
    }

    @Override
    public void setContext(Context appContext) {
        this.context = appContext;
    }

    @Override
    public void messageReceived(Message message) {
        //do nothing
    }

    @Override
    public void applicationInForeground() {
        //do nothing
    }

    MobileMessagingCore mobileMessagingCore(Context context) {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(context);
        }
        return mobileMessagingCore;
    }
}
