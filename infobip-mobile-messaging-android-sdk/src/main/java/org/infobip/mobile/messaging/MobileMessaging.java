package org.infobip.mobile.messaging;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import org.infobip.mobile.messaging.api.registration.RegistrationResponse;
import org.infobip.mobile.messaging.gcm.RegistrationIntentService;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.tasks.DeliveryReportResult;
import org.infobip.mobile.messaging.tasks.DeliveryReportTask;
import org.infobip.mobile.messaging.tasks.UpsertRegistrationTask;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * apiUri blah
 *
 * @author mstipanov
 * @since 29.02.2016.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MobileMessaging implements Configuration {
    public static final String TAG = "MobileMessaging";

    public static final String GCM_REGISTRATION_ID_SAVED = "org.infobip.mobile.messaging.gcm.GCM_REGISTRATION_ID_SAVED";
    public static final String GCM_REGISTRATION_ID = "org.infobip.mobile.messaging.gcm.REGISTRATION_ID";
    public static final String INFOBIP_REGISTRATION_ID = "org.infobip.mobile.messaging.infobip.REGISTRATION_ID";
    public static final String GCM_SENDER_ID = "org.infobip.mobile.messaging.gcm.GCM_SENDER_ID";
    public static final String APPLICATION_CODE = "org.infobip.mobile.messaging.infobip.APPLICATION_CODE";
    public static final String LAST_HTTP_EXCEPTION = "org.infobip.mobile.messaging.infobip.LAST_HTTP_EXCEPTION";

    private static final String DEFAULT_TITLE_VALUE = "Message";
    private static final int DEFAULT_ICON_VALUE = R.drawable.ic_stat_ic_notification;
    private static final String DEFAULT_API_URI = "https://oneapi.infobip.com/";
    private static final long[] DEFAULT_VIBRATION_PATTERN = new long[]{0, 250, 200, 250, 150, 150, 75, 150, 75, 150};
    private static final String INFOBIP_UNREPORTED_MESSAGE_IDS = "org.infobip.mobile.messaging.infobip.INFOBIP_UNREPORTED_MESSAGE_IDS";
    private static final String API_URI = "org.infobip.mobile.messaging.infobip.API_URI";
    private static final String DEFAULT_TITLE = "org.infobip.mobile.messaging.infobip.DEFAULT_TITLE";
    private static final String DISPLAY_NOTIFICATION_ENABLED = "org.infobip.mobile.messaging.infobip.DISPLAY_NOTIFICATION_ENABLED";
    private static final String CALLBACK_ACTIVITY = "org.infobip.mobile.messaging.infobip.CALLBACK_ACTIVITY";
    private static final String DEFAULT_ICON = "org.infobip.mobile.messaging.infobip.DEFAULT_ICON";
    private static final String VIBRATE = "org.infobip.mobile.messaging.infobip.VIBRATE";
    private static final String MESSAGE_STORE_CLASS = "org.infobip.mobile.messaging.infobip.MESSAGE_STORE_CLASS";

    private static MobileMessaging instance;
    private final Context context;
    private MobileMessagingStats stats;
    private MessageStore messageStore;

    private MobileMessaging(Context context) {
        this.context = context;
        stats = new MobileMessagingStats(context);
    }

    public synchronized static MobileMessaging getInstance(Context context) {
        if (null != instance) {
            return instance;
        }

        instance = new MobileMessaging(context);
        return instance;
    }

    public String getRegistrationId() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(GCM_REGISTRATION_ID, null);
    }

    public void setRegistrationId(String registrationId) {
        saveString(GCM_REGISTRATION_ID, registrationId);
    }

    public String getDeviceApplicationInstanceId() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(INFOBIP_REGISTRATION_ID, null);
    }

    private void setDeviceApplicationInstanceId(String registrationId) {
        saveString(INFOBIP_REGISTRATION_ID, registrationId);
    }

    public String getGcmSenderId() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(GCM_SENDER_ID, null);
    }

    private void setGcmSenderId(String gcmSenderId) {
        saveString(GCM_SENDER_ID, gcmSenderId);
    }

    public String getApplicationCode() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(APPLICATION_CODE, null);
    }

    private void setApplicationCode(String applicationCode) {
        saveString(APPLICATION_CODE, applicationCode);
    }

    public String getApiUri() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(API_URI, DEFAULT_API_URI);
    }

    private void setApiUri(String apiUri) {
        saveString(API_URI, apiUri);
    }

    public Class<?> getCallbackActivity() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String callbackActivityClassName = sharedPreferences.getString(CALLBACK_ACTIVITY, null);
        if (StringUtils.isBlank(callbackActivityClassName)) {
            return null;
        }
        //TODO cache
        try {
            return Class.forName(callbackActivityClassName);
        } catch (ClassNotFoundException e) {
            //TODO log
            return null;
        }
    }

    private void setCallbackActivity(Class<?> callbackActivity) {
        saveString(CALLBACK_ACTIVITY, null != callbackActivity.getName() ? callbackActivity.getName() : null);
    }

    public String getDefaultTitle() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(DEFAULT_TITLE, DEFAULT_TITLE_VALUE);
    }

    private void setDefaultTitle(String defaultTitle) {
        saveString(DEFAULT_TITLE, defaultTitle);
    }

    public boolean isDisplayNotificationEnabled() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(DISPLAY_NOTIFICATION_ENABLED, true);
    }

    private void setDisplayNotificationEnabled(boolean displayNotificationEnabled) {
        saveBoolean(DISPLAY_NOTIFICATION_ENABLED, displayNotificationEnabled);
    }

    public int getDefaultIcon() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(DEFAULT_ICON, DEFAULT_ICON_VALUE);
    }

    private void setDefaultIcon(int defaultIcon) {
        saveInt(DEFAULT_ICON, defaultIcon);
    }

    public String getLastHttpException() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(LAST_HTTP_EXCEPTION, null);
    }

    public void setLastHttpException(Exception lastHttpException) {
        String s = null;
        if (null != lastHttpException) {
            PrintWriter writer = null;
            try {
                StringWriter sw = new StringWriter();
                writer = new PrintWriter(sw);
                lastHttpException.printStackTrace(writer);
                s = sw.toString();
            } finally {
                if (null != writer) {
                    try {
                        writer.close();
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
        }
        saveString(LAST_HTTP_EXCEPTION, s);
    }

    public long[] getVibrate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String vibrate = sharedPreferences.getString(VIBRATE, null);
        if (null == vibrate) {
            return DEFAULT_VIBRATION_PATTERN;
        }
        //TODO cache
        try {
            JSONArray jsonArray = new JSONArray(vibrate);
            long[] vibratePattern = new long[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                vibratePattern[i] = jsonArray.getLong(i);
            }
            return vibratePattern;
        } catch (JSONException e) {
            //TODO log
            return DEFAULT_VIBRATION_PATTERN;
        }
    }

    private void setVibrate(long[] vibrate) {
        saveLongArray(VIBRATE, vibrate);
    }

    @SuppressWarnings("unchecked")
    public Class<? extends MessageStore> getMessageStoreClass() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String messageStoreClassName = sharedPreferences.getString(MESSAGE_STORE_CLASS, null);
        if (StringUtils.isBlank(messageStoreClassName)) {
            return null;
        }
        //TODO cache
        try {
            return (Class<? extends MessageStore>) Class.forName(messageStoreClassName);
        } catch (ClassNotFoundException e) {
            //TODO log
            return null;
        }
    }

    private void setMessageStoreClass(Class<? extends MessageStore> messageStoreClass) {
        saveString(MESSAGE_STORE_CLASS, null != messageStoreClass.getName() ? messageStoreClass.getName() : null);
    }

    public boolean isRegistrationIdSaved() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(GCM_REGISTRATION_ID_SAVED, false);
    }

    public void setRegistrationIdSaved(boolean registrationIdSaved) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(GCM_REGISTRATION_ID_SAVED, registrationIdSaved).apply();
    }

    private void saveString(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (null == value) {
            sharedPreferences.edit().remove(key).apply();
            return;
        }
        sharedPreferences.edit().putString(key, value).apply();
    }

    private void saveBoolean(String key, boolean value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    private void saveInt(String key, int value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putInt(key, value).apply();
    }

    private void saveLongArray(String key, long[] value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (null == value) {
            sharedPreferences.edit().remove(key).apply();
            return;
        }
        JSONArray jsonArray = new JSONArray();
        for (long aValue : value) {
            jsonArray.put(aValue);
        }
        sharedPreferences.edit().putString(key, jsonArray.toString()).apply();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private void checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.e(TAG, "Error accessing GCM.");
                //TODO raise event!
            } else {
                Log.i(TAG, "This device is not supported.");
                //TODO raise event!
            }
            return;
        }

        // Start IntentService to register this application with GCM.
        Intent intent = new Intent(context, RegistrationIntentService.class);
        context.startService(intent);
    }

    public String[] getUnreportedMessageIds() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> unreportedMessageIdSet = sharedPreferences.getStringSet(INFOBIP_UNREPORTED_MESSAGE_IDS, new HashSet<String>());
        return unreportedMessageIdSet.toArray(new String[unreportedMessageIdSet.size()]);
    }

    public void addUnreportedMessageIds(final String... messageIDs) {
        SetMutator mutator = new SetMutator() {
            @Override
            void mutate(Set<String> set) {
                set.addAll(Arrays.asList(messageIDs));
            }
        };
        editUnreportedMessageIds(mutator);
    }

    public void removeUnreportedMessageIds(final String... messageIDs) {
        SetMutator mutator = new SetMutator() {
            @Override
            void mutate(Set<String> set) {
                set.removeAll(Arrays.asList(messageIDs));
            }
        };
        editUnreportedMessageIds(mutator);
    }

    private synchronized void editUnreportedMessageIds(SetMutator mutator) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> unreportedMessageIdSet = sharedPreferences.getStringSet(INFOBIP_UNREPORTED_MESSAGE_IDS, new HashSet<String>());
        mutator.mutate(unreportedMessageIdSet);
        if (unreportedMessageIdSet.isEmpty()) {
            sharedPreferences.edit().remove(INFOBIP_UNREPORTED_MESSAGE_IDS).apply();
            return;
        }
        sharedPreferences.edit().putStringSet(INFOBIP_UNREPORTED_MESSAGE_IDS, unreportedMessageIdSet).apply();
    }

    public void reportUnreportedRegistration() {
        checkPlayServices();
        String infobipRegistrationId = getDeviceApplicationInstanceId();
        if (null != infobipRegistrationId && isRegistrationIdSaved()) {
            return;
        }

        reportRegistration();
    }

    public void reportRegistration() {
        if (StringUtils.isBlank(getRegistrationId())) {
            return;
        }

        new UpsertRegistrationTask(context) {
            @Override
            protected void onPostExecute(RegistrationResponse registrationResponse) {
                if (null == registrationResponse || StringUtils.isBlank(registrationResponse.getDeviceApplicationInstanceId())) {
                    Log.e(TAG, "MobileMessaging API didn't return any value!");
                    getStats().reportError(MobileMessagingError.CREATE_REGISTRATION_ERROR);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }
                setDeviceApplicationInstanceId(registrationResponse.getDeviceApplicationInstanceId());
                setRegistrationIdSaved(true);

                Intent registrationCreated = new Intent(Event.REGISTRATION_CREATED.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationCreated);
            }

            @Override
            protected void onCancelled() {
                Log.e(TAG, "Error creating registration!");
                setRegistrationIdSaved(false);

                getStats().reportError(MobileMessagingError.CREATE_REGISTRATION_ERROR);
                Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
            }
        }.execute();
    }

    public void reportUnreportedMessageIds() {
        if (getUnreportedMessageIds().length == 0) {
            return;
        }

        if (StringUtils.isBlank(getDeviceApplicationInstanceId())) {
            Log.w(TAG, "Can't report delivery reports to MobileMessaging API without saving registration first!");
            reportUnreportedRegistration();
            return;
        }

        new DeliveryReportTask(context) {
            @Override
            protected void onPostExecute(DeliveryReportResult result) {
                if (null == result) {
                    Log.e(TAG, "MobileMessaging API didn't return any value!");

                    getStats().reportError(MobileMessagingError.DELIVERY_REPORTING_ERROR);
                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                    return;
                }

                Intent messageReceived = new Intent(Event.DELIVERY_REPORTS_SENT.getKey());
                Bundle extras = new Bundle();
                extras.putStringArray("messageIDs", result.getMessageIDs());
                messageReceived.putExtras(extras);
                LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
            }

            @Override
            protected void onCancelled() {
                getStats().reportError(MobileMessagingError.DELIVERY_REPORTING_ERROR);
                Log.e(TAG, "Error reporting delivery!");
                Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
            }
        }.execute();
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

    public MobileMessagingStats getStats() {
        return stats;
    }

    public static final class Builder {
        private final MobileMessaging mobileMessaging;

        public Builder(Context context) {
            this.mobileMessaging = new MobileMessaging(null != context ? context.getApplicationContext() : null);
            loadDefaultTitle(context);
            loadDefaultApiUri(context);
        }

        private void loadDefaultTitle(Context context) {
            int appNameResource = ResourceLoader.loadResourceByName(context, "string", "app_name");
            if (appNameResource > 0) {
                String appName = context.getResources().getString(appNameResource);
                if (StringUtils.isNotBlank(appName)) {
                    mobileMessaging.setDefaultTitle(appName);
                }
            }
        }

        private void loadDefaultApiUri(Context context) {
            int infobipApiUriResource = ResourceLoader.loadResourceByName(context, "string", "infobip_api_uri");
            if (infobipApiUriResource > 0) {
                String apiUri = context.getResources().getString(infobipApiUriResource);
                if (StringUtils.isNotBlank(apiUri)) {
                    mobileMessaging.setApiUri(apiUri);
                }
            }
        }

        public Builder withGcmSenderId(String gcmSenderId) {
            mobileMessaging.setGcmSenderId(gcmSenderId);
            return this;
        }

        public Builder withApplicationCode(String applicationCode) {
            mobileMessaging.setApplicationCode(applicationCode);
            return this;
        }

        public Builder withCallbackActivity(Class<?> callbackActivity) {
            mobileMessaging.setCallbackActivity(callbackActivity);
            return this;
        }

        public Builder withApiUri(String apiUri) {
            mobileMessaging.setApiUri(apiUri);
            return this;
        }

        public Builder withDefaultTitle(String defaultTitle) {
            mobileMessaging.setDefaultTitle(defaultTitle);
            return this;
        }

        public Builder withVibrate(long[] vibrate) {
            mobileMessaging.setVibrate(vibrate);
            return this;
        }

        public Builder withoutVibrate() {
            mobileMessaging.setVibrate(null);
            return this;
        }

        public Builder withDefaultIcon(int defaultIcon) {
            mobileMessaging.setDefaultIcon(defaultIcon);
            return this;
        }

        public Builder withDisplayNotification() {
            mobileMessaging.setDisplayNotificationEnabled(true);
            return this;
        }

        public Builder withoutDisplayNotification() {
            mobileMessaging.setDisplayNotificationEnabled(false);
            return this;
        }

        public Builder withMessageStore(Class<? extends MessageStore> messageStoreClass) {
            mobileMessaging.setMessageStoreClass(messageStoreClass);
            return this;
        }

        public Builder withoutMessageStore() {
            mobileMessaging.setMessageStoreClass(null);
            return this;
        }

        public MobileMessaging build() {
            if (null == mobileMessaging.context.getResources().getDrawable(mobileMessaging.getDefaultIcon())) {
                throw new IllegalArgumentException("defaultIcon doesn't exist: " + mobileMessaging.getDefaultIcon());
            }

            if (null == mobileMessaging.getApiUri()) {
                throw new IllegalArgumentException("apiUri is mandatory! If in doubt, use " + DEFAULT_API_URI);
            }

            if (null == mobileMessaging.getApplicationCode()) {
                throw new IllegalArgumentException("applicationCode is mandatory!"); //TODO link to the explanation how to get one!
            }

            if (null == mobileMessaging.getGcmSenderId()) {
                throw new IllegalArgumentException("gcmSenderId is mandatory!"); //TODO link to the explanation how to get one!
            }

            if (mobileMessaging.isDisplayNotificationEnabled() && null == mobileMessaging.getCallbackActivity()) {
                throw new IllegalArgumentException("callbackActivity is mandatory! You should use the activity that will display received messages.");
            }

            MobileMessaging.instance = mobileMessaging;
            mobileMessaging.setLastHttpException(null);
            mobileMessaging.reportUnreportedRegistration();
            mobileMessaging.reportUnreportedMessageIds();
            return mobileMessaging;
        }
    }

    private abstract class SetMutator {
        abstract void mutate(Set<String> set);
    }
}
