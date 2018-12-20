package org.infobip.mobile.messaging.mobile.appinstance;

import android.content.Context;

import org.infobip.mobile.messaging.CustomUserDataValue;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.UserDataMapper;
import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.mobile.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Platform;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.StringUtils;
import org.infobip.mobile.messaging.util.SystemInformation;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;


public class InstallationSynchronizer {

    private static final String OS = "Android";

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final RetryPolicyProvider retryPolicyProvider;
    private final MobileApiAppInstance mobileApiAppInstance;

    public InstallationSynchronizer(
            Context context,
            MobileMessagingCore mobileMessagingCore,
            MobileMessagingStats stats,
            Executor executor,
            Broadcaster broadcaster,
            RetryPolicyProvider retryPolicyProvider,
            MobileApiAppInstance mobileApiAppInstance) {

        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.retryPolicyProvider = retryPolicyProvider;
        this.mobileApiAppInstance = mobileApiAppInstance;
    }

    public void sync() {
        sync(null);
    }

    public void sync(InstallationActionListener actionListener) {
        AppInstance appInstance = new AppInstance();
        SystemData systemDataForReport = systemDataForReport();
        boolean shouldUpdateInstance = false;
        boolean cloudTokenPresentAndUnreported = isCloudTokenPresentAndUnreported();

        if (systemDataForReport != null) {
            shouldUpdateInstance = true;
            appInstance = from(systemDataForReport);
        }

        if (cloudTokenPresentAndUnreported) {
            shouldUpdateInstance = true;
            appInstance.setPushServiceToken(mobileMessagingCore.getCloudToken());
        }

        if (mobileMessagingCore.isPushServiceTypeChanged()) {
            shouldUpdateInstance = true;
            appInstance.setPushServiceType(Platform.usedPushServiceType);
        }

        if (mobileMessagingCore.getUnreportedPrimarySetting() != null) {
            shouldUpdateInstance = true;
            appInstance.setIsPrimary(mobileMessagingCore.getUnreportedPrimarySetting());
        }

        if (!mobileMessagingCore.isApplicationUserIdReported()) {
            shouldUpdateInstance = true;
            appInstance.setApplicationUserId(mobileMessagingCore.getApplicationUserId());
        }

        appInstance.setRegEnabled(mobileMessagingCore.isPushRegistrationEnabled());

        if (mobileMessagingCore.isRegistrationUnavailable()) {
            if (cloudTokenPresentAndUnreported) createInstance(appInstance, actionListener);
        } else {
            if (shouldUpdateInstance) patchInstance(appInstance, actionListener);
        }
    }

    public void updateApplicationUserId(String applicationUserId, InstallationActionListener actionListener) {
        AppInstance appInstance = new AppInstance();
        appInstance.setApplicationUserId(applicationUserId);
        patchInstance(appInstance, actionListener);
    }

    public void updateCustomAttributes(Map<String,CustomUserDataValue> customAtts, InstallationActionListener actionListener) {
        AppInstance appInstance = new AppInstance();
        appInstance.setCustomAttributes(UserDataMapper.mapCustomAttsForBackendReport(customAtts));
        patchInstance(appInstance, actionListener);
    }

    public void updatePushRegEnabledStatus(Boolean enabled, InstallationActionListener actionListener) {
        AppInstance installation = new AppInstance();
        installation.setRegEnabled(enabled);
        patchInstance(installation, actionListener);
    }

    public void updatePrimaryStatus(Boolean primary, InstallationActionListener actionListener) {
        updatePrimaryStatus(null, primary, actionListener);
    }

    public void updatePrimaryStatus(String pushRegId, Boolean primary, InstallationActionListener actionListener) {
        AppInstance installation = new AppInstance();
        installation.setPushRegId(pushRegId);
        installation.setIsPrimary(primary);
        patchInstance(installation, actionListener);
    }

    public void patch(Installation installation, InstallationActionListener actionListener) {
        patchInstance(installation.toAppInstance(), actionListener);
    }

    private void createInstance(final AppInstance appInstance, final InstallationActionListener actionListener) {
        new MRetryableTask<Void, AppInstanceWithPushRegId>() {

            @Override
            public AppInstanceWithPushRegId run(Void[] voids) {
                MobileMessagingLogger.v("CREATE INSTALLATION >>>", appInstance);
                setCloudTokenReported(true);
                return mobileApiAppInstance.createInstance(false, appInstance);
            }

            @Override
            public void after(AppInstanceWithPushRegId appInstanceWithPushRegId) {
                MobileMessagingLogger.v("CREATE INSTALLATION <<<", appInstanceWithPushRegId);

                if (appInstanceWithPushRegId == null) {
                    setCloudTokenReported(false);
                    return;
                }

                Installation installation = Installation.from(appInstanceWithPushRegId);
                setPushRegistrationId(installation.getPushRegistrationId());
                updateInstallationReported(installation, true);

                broadcaster.installationCreated(installation);

                if (actionListener != null) {
                    actionListener.onSuccess(Collections.<UserData.Installation>singletonList(installation));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("CREATE INSTALLATION ERROR <<<", error);
                setCloudTokenReported(false);

                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.REGISTRATION_SYNC_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(error));

                if (actionListener != null) {
                    actionListener.onError(MobileMessagingError.createFrom(error));
                }
            }
        }
                .retryWith(retryPolicyProvider.DEFAULT())
                .execute(executor);
    }

    private void patchInstance(final AppInstance appInstance, final InstallationActionListener actionListener) {
        String pushRegId = mobileMessagingCore.getPushRegistrationId();
        final boolean myDevice = isMyDevice(appInstance, pushRegId);
        if (!myDevice) {
            pushRegId = appInstance.getPushRegId();
        }

        final String pushRegIdToUpdate = pushRegId;
        new MRetryableTask<Void, Void>() {
            @Override
            public Void run(Void[] voids) {
                MobileMessagingLogger.v("UPDATE INSTALLATION >>>");
                return mobileApiAppInstance.patchInstance(pushRegIdToUpdate, true, appInstance);
            }

            @Override
            public void after(Void aVoid) {
                MobileMessagingLogger.v("UPDATE INSTALLATION <<<");
                Installation installation = Installation.from(appInstance);
                //TODO get local instance

                updateInstallationReported(installation, myDevice);

                broadcaster.installationUpdated(installation);

                if (actionListener != null) {
                    actionListener.onSuccess(Collections.<UserData.Installation>singletonList(installation));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("UPDATE INSTALLATION ERROR <<<", error);
                setCloudTokenReported(false);

                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.REGISTRATION_SYNC_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(error));

                if (actionListener != null) {
                    actionListener.onError(MobileMessagingError.createFrom(error));
                }
            }
        }
                .retryWith(retryPolicyProvider.DEFAULT())
                .execute(executor);
    }

    private boolean isMyDevice(AppInstance appInstance, String myPushRegId) {
        return  (appInstance.getPushRegId() != null && myPushRegId.equals(appInstance.getPushRegId())) || appInstance.getPushRegId() == null;
    }

    private void updateInstallationReported(Installation installation, boolean myDevice) {
        if (!myDevice) {
            PreferenceHelper.remove(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED);
            PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_PRIMARY, false);
            return;
        }

        PreferenceHelper.remove(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED);
        if (installation.getPrimaryDevice() != null) {
            PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_PRIMARY, installation.getPrimaryDevice());
        }
        setPushRegistrationEnabled(installation.isPushRegistrationEnabled());
        setCloudTokenReported(true);
        mobileMessagingCore.setApplicationUserIdReported(true);

        mobileMessagingCore.setCustomAttributesReported(true);
        mobileMessagingCore.setSystemDataReported();
        mobileMessagingCore.setReportedPushServiceType();
    }

    public void fetchInstance(final InstallationActionListener actionListener) {
        if (mobileMessagingCore.isRegistrationUnavailable()) {
            return;
        }

        new MRetryableTask<Void, AppInstanceWithPushRegId>() {
            @Override
            public AppInstanceWithPushRegId run(Void[] voids) {
                MobileMessagingLogger.v("GET INSTALLATION >>>");
                return mobileApiAppInstance.getInstance(mobileMessagingCore.getPushRegistrationId());
            }

            @Override
            public void after(AppInstanceWithPushRegId instance) {
                Installation installation = Installation.from(instance);

                if (actionListener != null) {
                    actionListener.onSuccess(Collections.<UserData.Installation>singletonList(installation));
                }
                MobileMessagingLogger.v("GET INSTALLATION <<<");
            }

            @Override
            public void error(Throwable error) {
                if (actionListener != null) {
                    actionListener.onError(MobileMessagingError.createFrom(error));
                }
                MobileMessagingLogger.v("GET INSTALLATION ERROR <<<", error);
            }
        }
                .retryWith(retryPolicyProvider.DEFAULT())
                .execute(executor);
    }

    private boolean isCloudTokenPresentAndUnreported() {
        return !isCloudTokenReported() && StringUtils.isNotBlank(mobileMessagingCore.getCloudToken());
    }

    private SystemData systemDataForReport() {
        boolean reportEnabled = PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO);

        SystemData data = new SystemData(SoftwareInformation.getSDKVersionWithPostfixForSystemData(context),
                reportEnabled ? SystemInformation.getAndroidSystemVersion() : "",
                reportEnabled ? DeviceInformation.getDeviceManufacturer() : "",
                reportEnabled ? DeviceInformation.getDeviceModel() : "",
                reportEnabled ? SoftwareInformation.getAppVersion(context) : "",
                mobileMessagingCore.isGeofencingActivated(),
                SoftwareInformation.areNotificationsEnabled(context),
                reportEnabled && DeviceInformation.isDeviceSecure(context),
                reportEnabled ? SystemInformation.getAndroidSystemLanguage() : "",
                reportEnabled ? SystemInformation.getAndroidDeviceName(context) : "");

        Integer hash = PreferenceHelper.findInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
        if (hash != data.hashCode()) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, data.toString());
            return data;
        }

        return null;
    }

    private void setPushRegistrationEnabled(Boolean pushRegistrationEnabled) {
        if (pushRegistrationEnabled == null) {
            return;
        }

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, pushRegistrationEnabled);
    }

    private void setPushRegistrationId(String registrationId) {
        if (registrationId == null) {
            return;
        }

        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, registrationId);
    }

    public void setCloudTokenReported(boolean reported) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, reported);
    }

    public boolean isCloudTokenReported() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED);
    }

    private AppInstance from(SystemData data) {
        return new AppInstance(
                data.getSdkVersion(),
                data.getOsVersion(),
                data.getDeviceManufacturer(),
                data.getDeviceModel(),
                data.getApplicationVersion(),
                data.isGeofencing(),
                data.areNotificationsEnabled(),
                data.isDeviceSecure(),
                data.getOsLanguage(),
                data.getDeviceName(),
                OS);
    }
}
