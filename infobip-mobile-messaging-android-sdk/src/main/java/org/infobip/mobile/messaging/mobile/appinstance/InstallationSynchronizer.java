package org.infobip.mobile.messaging.mobile.appinstance;

import android.content.Context;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.InstallationMapper;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.UserMapper;
import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.Result;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;


public class InstallationSynchronizer {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final RetryPolicyProvider retryPolicyProvider;
    private final MobileApiAppInstance mobileApiAppInstance;

    private static class PushInstallation extends Installation {
        void setServiceType() {
            super.setPushServiceType(Platform.usedPushServiceType);
        }

        void setToken(String token) {
            super.setPushServiceToken(token);
        }
    }

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

    public void sync(MobileMessaging.ResultListener<Installation> actionListener) {
        PushInstallation installation = new PushInstallation();

        SystemData systemDataForReport = systemDataForReport();
        if (systemDataForReport != null) {
            installation = from(systemDataForReport);
        }

        boolean cloudTokenPresentAndUnreported = isCloudTokenPresentAndUnreported();
        if (cloudTokenPresentAndUnreported) {
            installation.setToken(mobileMessagingCore.getCloudToken());
        }

        if (mobileMessagingCore.isPushServiceTypeChanged()) {
            installation.setServiceType();
        }

        if (mobileMessagingCore.getUnreportedPrimarySetting() != null) {
            installation.setPrimaryDevice(mobileMessagingCore.getUnreportedPrimarySetting());
        }

        if (!mobileMessagingCore.isApplicationUserIdReported()) {
            installation.setApplicationUserId(mobileMessagingCore.getApplicationUserId());
        }

        if (mobileMessagingCore.isPushRegistrationEnabledUnreported()) {
            installation.setPushRegistrationEnabled(mobileMessagingCore.isPushRegistrationEnabled());
        }

        if (mobileMessagingCore.isRegistrationUnavailable()) {
            installation.setServiceType();
            if (cloudTokenPresentAndUnreported) createInstance(installation, actionListener);
        } else {
            if (installation.hasDataToReport()) patch(installation, actionListener);
        }
    }

    public void updatePrimaryStatus(String pushRegId, Boolean primary, MobileMessaging.ResultListener<Installation> actionListener) {
        Installation installation = new Installation(pushRegId);
        installation.setPrimaryDevice(primary);
        patch(installation, actionListener);
    }

    private void createInstance(final Installation installation, final MobileMessaging.ResultListener<Installation> actionListener) {
        new MRetryableTask<Void, AppInstance>() {

            @Override
            public AppInstance run(Void[] voids) {
                MobileMessagingLogger.v("CREATE INSTALLATION >>>", installation);
                setCloudTokenReported(true);
                return mobileApiAppInstance.createInstance(false, InstallationMapper.toBackend(installation));
            }

            @Override
            public void after(AppInstance appInstance) {
                MobileMessagingLogger.v("CREATE INSTALLATION <<<", appInstance);

                if (appInstance == null) {
                    setCloudTokenReported(false);
                    return;
                }

                Installation installation = InstallationMapper.fromBackend(appInstance);
                setPushRegistrationId(installation.getPushRegistrationId());
                updateInstallationReported(installation, true);

                broadcaster.registrationCreated(installation.getPushServiceToken(), installation.getPushRegistrationId());

                if (actionListener != null) {
                    actionListener.onResult(new Result<>(installation));
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
                    actionListener.onResult(new Result<>(mobileMessagingCore.getInstallation(true), MobileMessagingError.createFrom(error)));
                }
            }
        }

                .retryWith(retryPolicyProvider.DEFAULT())
                .execute(executor);
    }

    public void patch(final Installation installation, final MobileMessaging.ResultListener<Installation> actionListener) {
        String pushRegId = mobileMessagingCore.getPushRegistrationId();
        final boolean myDevice = isMyDevice(installation, pushRegId);
        if (!myDevice) {
            pushRegId = installation.getPushRegistrationId();
        }

        if (!installation.hasDataToReport()) {
            MobileMessagingLogger.w("Attempt to save empty installation data, will do nothing");
            if (actionListener != null) {
                actionListener.onResult(new Result<Installation, MobileMessagingError>(InternalSdkError.ERROR_SAVING_EMPTY_OBJECT.getError()));
            }
            return;
        }

        final String pushRegIdToUpdate = pushRegId;
        new MRetryableTask<Void, Void>() {
            @Override
            public Void run(Void[] voids) {
                MobileMessagingLogger.v("UPDATE INSTALLATION >>>");
                mobileApiAppInstance.patchInstance(pushRegIdToUpdate, true, new HashMap<>(installation.getMap()));
                return null;
            }

            @Override
            public void after(Void aVoid) {
                MobileMessagingLogger.v("UPDATE INSTALLATION <<<");

                updateInstallationReported(installation, myDevice);
                Installation installationToReturn = installation;
                if (myDevice) {
                    installationToReturn = mobileMessagingCore.getInstallation(true);
                }
                broadcaster.installationUpdated(installationToReturn);

                if (actionListener != null) {
                    actionListener.onResult(new Result<>(installationToReturn));
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
                    actionListener.onResult(new Result<>(mobileMessagingCore.getInstallation(true), MobileMessagingError.createFrom(error)));
                }
            }
        }
                .retryWith(retryPolicyProvider.DEFAULT())
                .execute(executor);
    }

    private boolean isMyDevice(Installation installation, String myPushRegId) {
        return (installation.getPushRegistrationId() != null && myPushRegId.equals(installation.getPushRegistrationId())) || installation.getPushRegistrationId() == null;
    }

    private void updateInstallationReported(Installation installation, boolean myDevice) {
        PreferenceHelper.remove(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED);

        if (!myDevice) {
            mobileMessagingCore.savePrimarySetting(false);
            return;
        }

        if (installation.isPrimaryDevice() != null) {
            mobileMessagingCore.savePrimarySetting(installation.isPrimaryDevice());
        }
        if (installation.isPushRegistrationEnabled() != null) {
            setPushRegistrationEnabled(installation.isPushRegistrationEnabled());
            mobileMessagingCore.setPushRegistrationEnabledReported();
        }
        setCloudTokenReported(true);
        mobileMessagingCore.setApplicationUserIdReported(true);

        String unreportedCustomAttributes = mobileMessagingCore.getUnreportedCustomAttributes();
        if (unreportedCustomAttributes != null) {
            mobileMessagingCore.setUnreportedCustomAttributes(null);
            String reportedCustomAtts = mobileMessagingCore.getCustomAttributes();
            Map<String, CustomAttributeValue> customAttsMap = UserMapper.customAttsFrom(reportedCustomAtts);
            Map<String, CustomAttributeValue> unreportedCustomAttsMap = UserMapper.customAttsFrom(unreportedCustomAttributes);
            if (customAttsMap == null) {
                customAttsMap = new HashMap<>();
            }
            customAttsMap.putAll(unreportedCustomAttsMap);

            mobileMessagingCore.saveCustomAttributes(customAttsMap);
        }
        mobileMessagingCore.setSystemDataReported();
        mobileMessagingCore.setReportedPushServiceType();
    }

    public void fetchInstance(final MobileMessaging.ResultListener<Installation> actionListener) {
        if (mobileMessagingCore.isRegistrationUnavailable()) {
            return;
        }

        new MRetryableTask<Void, AppInstance>() {
            @Override
            public AppInstance run(Void[] voids) {
                MobileMessagingLogger.v("GET INSTALLATION >>>");
                return mobileApiAppInstance.getInstance(mobileMessagingCore.getPushRegistrationId());
            }

            @Override
            public void after(AppInstance instance) {
                Installation installation = InstallationMapper.fromBackend(instance);
                if (installation.isPrimaryDevice() != null) {
                    mobileMessagingCore.savePrimarySetting(installation.isPrimaryDevice());
                }
                mobileMessagingCore.saveCustomAttributes(installation.getCustomAttributes());

                if (actionListener != null) {
                    actionListener.onResult(new Result<>(installation));
                }
                MobileMessagingLogger.v("GET INSTALLATION <<<");
            }

            @Override
            public void error(Throwable error) {
                if (actionListener != null) {
                    actionListener.onResult(new Result<>(mobileMessagingCore.getInstallation(true), MobileMessagingError.createFrom(error)));
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
                reportEnabled ? SystemInformation.getAndroidDeviceName(context) : "",
                reportEnabled ? DeviceInformation.getDeviceTimeZoneOffset() : "");

        Integer hash = PreferenceHelper.findInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
        if (hash != data.hashCode()) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, data.toString());
            return data;
        }

        return null;
    }

    private void setPushRegistrationEnabled(Boolean pushRegistrationEnabled) {
        if (pushRegistrationEnabled != null) {
            PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, pushRegistrationEnabled);
        }
    }

    private void setPushRegistrationId(String registrationId) {
        if (registrationId != null) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, registrationId);
        }
    }

    public void setCloudTokenReported(boolean reported) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, reported);
    }

    public boolean isCloudTokenReported() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED);
    }

    private PushInstallation from(SystemData data) {
        PushInstallation installation = new PushInstallation();
        installation.setSdkVersion(data.getSdkVersion());
        installation.setOsVersion(data.getOsVersion());
        installation.setDeviceManufacturer(data.getDeviceManufacturer());
        installation.setDeviceModel(data.getDeviceModel());
        installation.setAppVersion(data.getApplicationVersion());
        installation.setGeoEnabled(data.isGeofencing());
        installation.setNotificationsEnabled(data.areNotificationsEnabled());
        installation.setDeviceSecure(data.isDeviceSecure());
        installation.setLanguage(data.getLanguage());
        installation.setDeviceTimezoneOffset(data.getDeviceTimeZoneOffset());
        installation.setDeviceName(data.getDeviceName());
        installation.setOs(Platform.os);
        return installation;
    }
}
