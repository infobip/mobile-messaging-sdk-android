package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.telephony.MobileNetworkStateListener;
import org.infobip.mobile.messaging.util.ExceptionUtils;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author sslavin
 * @since 28.04.2016.
 */
public class MobileMessagingCore {
    private static MobileMessagingCore instance;

    private final RegistrationSynchronizer registrationSynchronizer = new RegistrationSynchronizer();
    private final DeliveryReporter deliveryReporter = new DeliveryReporter();
    private final SeenStatusReporter seenStatusReporter = new SeenStatusReporter();
    private final MsisdnSynchronizer msisdnSynchronizer = new MsisdnSynchronizer();
    private final MobileNetworkStateListener mobileNetworkStateListener;
    private final MobileMessagingStats stats;
    private NotificationSettings notificationSettings;
    private MessageStore messageStore;
    private Context context;
    private OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (!MobileMessagingProperty.MSISDN.getKey().equals(key)) {
                return;
            }
            setMsisdnReported(false);
            sync();
        }
    };

    protected MobileMessagingCore(Context context) {
        this.context = context;
        this.stats = new MobileMessagingStats(context);
        this.mobileNetworkStateListener = new MobileNetworkStateListener(context);
        PreferenceHelper.registerOnSharedPreferenceChangeListener(context, onSharedPreferenceChangeListener);
    }

    public static MobileMessagingCore getInstance(Context context) {
        if (null != instance) {
            return instance;
        }

        instance = new MobileMessagingCore(context);
        return instance;
    }

    public void sync() {
        registrationSynchronizer.syncronize(context, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdReported(), getStats());
        deliveryReporter.report(context, registrationSynchronizer, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdReported(), getUnreportedMessageIds(), getStats());
        seenStatusReporter.report(context, registrationSynchronizer, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdReported(), getUnreportedSeenMessageIds(), getStats());
    }

    public void syncMsisdn() {
        msisdnSynchronizer.syncronize(context, getDeviceApplicationInstanceId(), getMsisdn(), isMsisdnReported(), getStats());
    }

    public long getMsisdn() {
        return PreferenceHelper.findLong(context, MobileMessagingProperty.MSISDN);
    }

    protected void setMsisdn(long msisdn) {
        if (msisdn < 0) {
            throw new IllegalArgumentException("MSISDN can't be negative!");
        }
        long oldMsisdn = getMsisdn();
        if (oldMsisdn != 0 && oldMsisdn == msisdn) {
            return;
        }
        PreferenceHelper.saveLong(context, MobileMessagingProperty.MSISDN, msisdn);
    }

    public String getRegistrationId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.GCM_REGISTRATION_ID);
    }

    public String getDeviceApplicationInstanceId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
    }

    public String[] getUnreportedMessageIds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS);
    }

    protected void addUnreportedMessageIds(final String... messageIDs) {
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public void removeUnreportedMessageIds(final String... messageIDs) {
        PreferenceHelper.deleteFromStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public String[] getUnreportedSeenMessageIds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS);
    }

    protected void addUnreportedSeenMessageIds(final String... messageIDs) {
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, messageIDs);
    }

    public void removeUnreportedSeenMessageIds(final String... messageIDs) {
        PreferenceHelper.deleteFromStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, messageIDs);
    }

    public void setMessagesDelivered(String... messageIds) {
        addUnreportedMessageIds(messageIds);
        sync();
    }

    protected void setMessagesSeen(String... messageIds) {
        if (isMessageStoreEnabled()) {
            for (Message message : messageStore.findAllMatching(context, messageIds)) {
                message.setSeenTimestamp(System.currentTimeMillis());
                messageStore.save(context, message);
            }
        }
        addUnreportedSeenMessageIds(messageIds);
        sync();
    }

    public NotificationSettings getNotificationSettings() {
        if (!isDisplayNotificationEnabled()) {
            return null;
        }
        if (null != notificationSettings)
            return notificationSettings;

        notificationSettings = new NotificationSettings(context);
        return notificationSettings;
    }

    protected void setNotificationSettings(NotificationSettings notificationSettings) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, null != notificationSettings);
        this.notificationSettings = notificationSettings;
    }

    private boolean isDisplayNotificationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED);
    }

    protected static void setGcmSenderId(Context context, String gcmSenderId) {
        if (StringUtils.isBlank(gcmSenderId)) {
            throw new IllegalArgumentException("gcmSenderId is mandatory! Get one here: https://developers.google.com/mobile/add?platform=android&cntapi=gcm");
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_SENDER_ID, gcmSenderId);
    }

    public String getGcmSenderId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.GCM_SENDER_ID);
    }

    public boolean isRegistrationIdReported() {
        return registrationSynchronizer.isRegistrationIdReported(context);
    }

    private void setRegistrationIdReported(boolean registrationIdReported) {
        registrationSynchronizer.setRegistrationIdReported(context, registrationIdReported);
    }

    public void setRegistrationId(String registrationId) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, registrationId);
        setRegistrationIdReported(false);
    }

    protected boolean isMsisdnReported() {
        return msisdnSynchronizer.isMsisdnReported(context);
    }

    protected void setMsisdnReported(boolean msisdnSaved) {
        msisdnSynchronizer.setMsisdnReported(context, msisdnSaved);
    }

    protected static void setMessageStoreClass(Context context, Class<? extends MessageStore> messageStoreClass) {
        String value = null != messageStoreClass ? messageStoreClass.getName() : null;
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, value);
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    protected Class<? extends MessageStore> getMessageStoreClass() {
        return PreferenceHelper.findClass(context, MobileMessagingProperty.MESSAGE_STORE_CLASS);
    }

    public MessageStore getMessageStore() {
        if (!isMessageStoreEnabled()) {
            return null;
        }

        if (null != messageStore) {
            return messageStore;
        }

        Class<? extends MessageStore> messageStoreClass = null;
        try {
            messageStoreClass = getMessageStoreClass();
            messageStore = messageStoreClass.newInstance();
            return messageStore;
        } catch (Exception e) {
            throw new MessageStoreInstantiationException("Can't create message store of type: " + messageStoreClass, e);
        }
    }

    public boolean isMessageStoreEnabled() {
        return null != getMessageStoreClass();
    }

    protected MobileMessagingStats getStats() {
        return stats;
    }

    public void setLastHttpException(Exception lastHttpException) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.LAST_HTTP_EXCEPTION, ExceptionUtils.stacktrace(lastHttpException));
    }

    @SuppressWarnings("unused")
    public String getLastHttpException() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.LAST_HTTP_EXCEPTION);
    }

    protected static void setApplicationCode(Context context, String applicationCode) {
        if (StringUtils.isBlank(applicationCode)) {
            throw new IllegalArgumentException("applicationCode is mandatory! Get one here: https://portal.infobip.com/push/applications");
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, applicationCode);
    }

    public String getApplicationCode() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.APPLICATION_CODE);
    }

    protected static void setApiUri(Context context, String apiUri) {
        if (StringUtils.isBlank(apiUri)) {
            throw new IllegalArgumentException("apiUri is mandatory! If in doubt, use " + MobileMessagingProperty.API_URI.getDefaultValue());
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, apiUri);
    }

    public String getApiUri() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.API_URI);
    }

    protected static void setReportCarrierInfo(Context context, boolean reportCarrierInfo) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_CARRIER_INFO, reportCarrierInfo);
    }

    protected static void setReportSystemInfo(Context context, boolean reportSystemInfo) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, reportSystemInfo);
    }
}
