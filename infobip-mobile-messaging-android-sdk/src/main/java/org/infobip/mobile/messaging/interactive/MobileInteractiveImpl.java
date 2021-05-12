package org.infobip.mobile.messaging.interactive;

import android.annotation.SuppressLint;
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
import org.infobip.mobile.messaging.interactive.inapp.InAppNotificationHandler;
import org.infobip.mobile.messaging.interactive.inapp.InAppNotificationHandlerImpl;
import org.infobip.mobile.messaging.interactive.predefined.PredefinedActionsProvider;
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

    @SuppressLint("StaticFieldLeak")
    private static MobileInteractiveImpl instance;
    private Context context;
    private final JsonSerializer serializer = new JsonSerializer(false);
    private Set<NotificationCategory> customNotificationCategories;
    private Set<NotificationCategory> predefinedNotificationCategories;
    private MobileMessagingCore mobileMessagingCore;
    private InAppNotificationHandler inAppNotificationHandler;
    private PredefinedActionsProvider predefinedActionsProvider;

    public MobileInteractiveImpl() {
    }

    @VisibleForTesting
    MobileInteractiveImpl(Context context, MobileMessagingCore mobileMessagingCore, InAppNotificationHandler inAppNotificationHandler, PredefinedActionsProvider predefinedActionsProvider) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.inAppNotificationHandler = inAppNotificationHandler;
        this.predefinedActionsProvider = predefinedActionsProvider;
    }

    public static MobileInteractiveImpl getInstance(Context context) {
        if (instance != null) {
            return instance;
        }

        instance = MobileMessagingCore.getInstance(context).getMessageHandlerModule(MobileInteractiveImpl.class);
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

        predefinedActionsProvider(context).verifyResourcesForCategory(categoryId);

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
    @NonNull
    public Set<NotificationCategory> getNotificationCategories() {
        if (!isDisplayNotificationEnabled()) {
            return new HashSet<>();
        }

        Set<NotificationCategory> notificationCategories = getPredefinedNotificationCategories();
        Set<NotificationCategory> customNotificationCategories = getCustomNotificationCategories();
        notificationCategories.addAll(customNotificationCategories);
        return notificationCategories;
    }

    @Override
    public void triggerSdkActionsFor(NotificationAction action, Message message) {
        markAsSeen(context, message.getMessageId());
        sendMo(context, message.getCategory(), action, message);
        inAppNotificationHandler(context).userPressedNotificationButtonForMessage(message);
    }

    private void markAsSeen(Context context, String messageId) {
        NotificationSettings notificationSettings = mobileMessagingCore(context).getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }
        if (notificationSettings.markSeenOnTap()) {
            mobileMessagingCore(context).setMessagesSeen(messageId);
        }
    }

    private void sendMo(Context context, String categoryId, NotificationAction action, Message initialMessage) {
        if (!action.sendsMoMessage()) {
            return;
        }

        if (StringUtils.isBlank(categoryId)) {
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

    private Set<NotificationCategory> getPredefinedNotificationCategories() {
        if (null != predefinedNotificationCategories) {
            return predefinedNotificationCategories;
        }

        return predefinedActionsProvider(context).getPredefinedCategories();
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
    public void init(Context appContext) {
        this.context = appContext;
    }

    @Override
    public boolean handleMessage(Message message) {
        inAppNotificationHandler(context).handleMessage(message);
        return false;
    }

    @Override
    public boolean messageTapped(Message message) {
        inAppNotificationHandler(context).userTappedNotificationForMessage(message);
        return false;
    }

    @Override
    public void applicationInForeground() {
        inAppNotificationHandler(context).appWentToForeground();
    }

    @Override
    public void cleanup() {
        PreferenceHelper.remove(context, MobileMessagingProperty.INTERACTIVE_CATEGORIES.getKey());
    }

    @Override
    public void displayInAppDialogFor(@NonNull Message message) {
        inAppNotificationHandler(context).displayDialogFor(message);
    }


    @Override
    public void depersonalize() {
        //do nothing
    }

    @Override
    public void performSyncActions() {
        // do nothing
    }

    private MobileMessagingCore mobileMessagingCore(Context context) {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(context);
        }
        return mobileMessagingCore;
    }

    private InAppNotificationHandler inAppNotificationHandler(Context context) {
        if (inAppNotificationHandler == null) {
            inAppNotificationHandler = new InAppNotificationHandlerImpl(context);
        }
        return inAppNotificationHandler;
    }

    private PredefinedActionsProvider predefinedActionsProvider(Context context) {
        if (predefinedActionsProvider == null) {
            predefinedActionsProvider = new PredefinedActionsProvider(context);
        }
        return predefinedActionsProvider;
    }
}
