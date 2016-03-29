package org.infobip.mobile.messaging;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import org.infobip.mobile.messaging.api.registration.RegistrationResponse;
import org.infobip.mobile.messaging.gcm.RegistrationIntentService;
import org.infobip.mobile.messaging.tasks.CreateRegistrationTask;
import org.infobip.mobile.messaging.tasks.DeliveryReportResult;
import org.infobip.mobile.messaging.tasks.DeliveryReportTask;
import org.infobip.mobile.messaging.tasks.UpdateRegistrationTask;
import org.infobip.mobile.messaging.util.StringUtils;

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
    public static final String DEFAULT_TITLE = "Message";
    public static final int DEFAULT_ICON = R.drawable.ic_stat_ic_notification;
    public static final String DEFAULT_API_URI = "https://oneapi.infobip.com/";
    public static final String TAG = "MobileMessaging";

    private static final String GCM_REGISTRATION_ID_SAVED = "org.infobip.mobile.messaging.gcm.GCM_REGISTRATION_ID_SAVED";
    private static final String GCM_REGISTRATION_ID = "org.infobip.mobile.messaging.gcm.REGISTRATION_ID";
    private static final String INFOBIP_REGISTRATION_ID = "org.infobip.mobile.messaging.infobip.REGISTRATION_ID";
    private static final String INFOBIP_UNREPORTED_MESSAGE_IDS = "org.infobip.mobile.messaging.infobip.INFOBIP_UNREPORTED_MESSAGE_IDS";
    private static final long[] DEFAULT_VIBRATION_PATTERN = new long[]{0, 250, 200, 250, 150, 150, 75, 150, 75, 150};

    private static MobileMessaging instance;

    private final Context context;
    private String gcmSenderId;
    private String applicationCode;
    private String apiUri = DEFAULT_API_URI;
    private Class<?> callbackActivity;
    private String defaultTitle = DEFAULT_TITLE;
    private boolean displayNotificationEnabled;
    private int defaultIcon = DEFAULT_ICON;
    private long[] vibrate = DEFAULT_VIBRATION_PATTERN;
    private boolean messageStoreEnabled;
    private boolean notificationEnabled = true;

    private MobileMessaging(Context context) {
        this.context = context;
    }

    public static MobileMessaging getInstance() {
        if (null == instance) {
            throw new MobileMessagingNotInitializedException("You must initialize MobileMessaging using: MobileMessaging mobileMessaging = new MobileMessaging.Builder().build();");
        }

        return instance;
    }

    public String getRegistrationId() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(GCM_REGISTRATION_ID, null);
    }

    public void setRegistrationId(String registrationId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(GCM_REGISTRATION_ID, registrationId).apply();
    }

    public String getInfobipRegistrationId() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(INFOBIP_REGISTRATION_ID, null);
    }

    public void setInfobipRegistrationId(String registrationId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(INFOBIP_REGISTRATION_ID, registrationId).apply();
    }

    public String getGcmSenderId() {
        return gcmSenderId;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public String getApiUri() {
        return apiUri;
    }

    public Class<?> getCallbackActivity() {
        return callbackActivity;
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public void checkPlayServices() {
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

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public boolean isDisplayNotificationEnabled() {
        return displayNotificationEnabled;
    }

    public boolean isRegistrationIdSaved() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(GCM_REGISTRATION_ID_SAVED, false);
    }

    public void setRegistrationIdSaved(boolean registrationIdSaved) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(GCM_REGISTRATION_ID_SAVED, registrationIdSaved).apply();
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
        String infobipRegistrationId = getInfobipRegistrationId();
        if (null != infobipRegistrationId && isRegistrationIdSaved()) {
            return;
        }

        reportRegistration();
    }

    public void reportRegistration() {
        if (StringUtils.isBlank(getRegistrationId())) {
            return;
        }

        AsyncTask task;
        if (null == getInfobipRegistrationId()) {
            task = new CreateRegistrationTask() {
                @Override
                protected void onPostExecute(RegistrationResponse registrationResponse) {
                    if (null == registrationResponse || StringUtils.isBlank(registrationResponse.getDeviceApplicationInstanceId())) {
                        Log.e(TAG, "MobileMessaging API didn't return any value!");

                        Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                        return;
                    }
                    MobileMessaging.getInstance().setInfobipRegistrationId(registrationResponse.getDeviceApplicationInstanceId());
                    MobileMessaging.getInstance().setRegistrationIdSaved(true);

                    Intent registrationCreated = new Intent(Event.REGISTRATION_CREATED.getKey());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationCreated);
                }

                @Override
                protected void onCancelled() {
                    MobileMessaging.getInstance().setRegistrationIdSaved(false);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                }
            };
        } else {
            task = new UpdateRegistrationTask() {
                @Override
                protected void onPostExecute(RegistrationResponse registrationResponse) {
                    if (null == registrationResponse || StringUtils.isBlank(registrationResponse.getDeviceApplicationInstanceId())) {
                        Log.e(TAG, "MobileMessaging API didn't return any value!");

                        Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                        return;
                    }
                    MobileMessaging.getInstance().setInfobipRegistrationId(registrationResponse.getDeviceApplicationInstanceId());
                    MobileMessaging.getInstance().setRegistrationIdSaved(true);

                    Intent registrationChanged = new Intent(Event.REGISTRATION_CHANGED.getKey());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationChanged);
                }

                @Override
                protected void onCancelled() {
                    MobileMessaging.getInstance().setRegistrationIdSaved(false);

                    Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
                }
            };
        }
        task.execute();
    }

    public void reportUnreportedMessageIds() {
        if (getUnreportedMessageIds().length == 0) {
            return;
        }

        new DeliveryReportTask() {
            @Override
            protected void onPostExecute(DeliveryReportResult result) {
                if (null == result) {
                    Log.e(TAG, "MobileMessaging API didn't return any value!");

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
                Intent registrationSaveError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                LocalBroadcastManager.getInstance(context).sendBroadcast(registrationSaveError);
            }
        }.execute();
    }

    public int getDefaultIcon() {
        return defaultIcon;
    }

    public long[] getVibrate() {
        return vibrate;
    }

    public boolean isMessageStoreEnabled() {
        return messageStoreEnabled;
    }

    public static final class Builder {
        private final MobileMessaging mobileMessaging;

        public Builder(Context context) {
            this.mobileMessaging = new MobileMessaging(null != context ? context.getApplicationContext() : null);
        }

        public Builder withGcmSenderId(String gcmSenderId) {
            mobileMessaging.gcmSenderId = gcmSenderId;
            return this;
        }

        public Builder withApplicationCode(String applicationCode) {
            mobileMessaging.applicationCode = applicationCode;
            return this;
        }

        public Builder withCallbackActivity(Class<?> callbackActivity) {
            mobileMessaging.callbackActivity = callbackActivity;
            mobileMessaging.displayNotificationEnabled = null != callbackActivity;
            return this;
        }

        public Builder withApiUri(String apiUri) {
            mobileMessaging.apiUri = apiUri;
            return this;
        }

        public Builder withDefaultTitle(String defaultTitle) {
            mobileMessaging.defaultTitle = defaultTitle;
            return this;
        }

        public Builder withVibrate(long[] vibrate) {
            mobileMessaging.vibrate = vibrate;
            return this;
        }

        public Builder withoutVibrate() {
            mobileMessaging.vibrate = null;
            return this;
        }

        public Builder withDefaultIcon(int defaultIcon) {
            mobileMessaging.defaultIcon = defaultIcon;
            return this;
        }

        public Builder withDisplayNotification() {
            mobileMessaging.displayNotificationEnabled = true;
            return this;
        }

        public Builder withoutDisplayNotification() {
            mobileMessaging.displayNotificationEnabled = false;
            return this;
        }

        public Builder withMessageStore() {
            mobileMessaging.messageStoreEnabled = true;
            return this;
        }

        public Builder withoutMessageStore() {
            mobileMessaging.messageStoreEnabled = false;
            return this;
        }

        public MobileMessaging build() {
            if (null == mobileMessaging.context.getResources().getDrawable(mobileMessaging.defaultIcon)) {
                throw new IllegalArgumentException("defaultIcon doesn't exist: " + mobileMessaging.defaultIcon);
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

            if (mobileMessaging.displayNotificationEnabled && null == mobileMessaging.getCallbackActivity()) {
                throw new IllegalArgumentException("callbackActivity is mandatory! You should use the activity that will display received messages.");
            }

            MobileMessaging.instance = mobileMessaging;
            mobileMessaging.checkPlayServices();
            mobileMessaging.reportUnreportedRegistration();
            mobileMessaging.reportUnreportedMessageIds();
            return mobileMessaging;
        }
    }

    private abstract class SetMutator {
        abstract void mutate(Set<String> set);
    }
}
