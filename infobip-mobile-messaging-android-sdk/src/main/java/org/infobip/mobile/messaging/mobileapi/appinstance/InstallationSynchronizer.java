package org.infobip.mobile.messaging.mobileapi.appinstance;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.InstallationMapper;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;


public class InstallationSynchronizer {

    private static final long SYNC_THROTTLE_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final RetryPolicyProvider retryPolicyProvider;
    private final MobileApiAppInstance mobileApiAppInstance;
    private volatile Long lastSyncTimeMillis;
    private volatile boolean isSyncStarting;

    private static class PushInstallation extends Installation {
        void setServiceType() {
            super.setPushServiceType();
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

    /**
     * @param actionListener needed only for tests
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    @VisibleForTesting
    void sync(MobileMessaging.ResultListener<Installation> actionListener) {
        if (didSyncRecently()) {
            if (actionListener != null) {
                actionListener.onResult(new Result<>(mobileMessagingCore.getInstallation(true), InternalSdkError.INSTALLATION_SYNC_IN_PROGRESS.getError()));
            }
            return;
        }
        isSyncStarting = true;

        PushInstallation installation = new PushInstallation();

        SystemData systemDataForReport = mobileMessagingCore.systemDataForReport(false);
        if (systemDataForReport != null) {
            installation = from(systemDataForReport);
        }

        boolean cloudTokenPresentAndUnreported = isCloudTokenPresentAndUnreported();
        if (cloudTokenPresentAndUnreported) {
            installation.setToken(mobileMessagingCore.getCloudToken());
            installation.setServiceType();
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

        if (!mobileMessagingCore.isRegistrationAvailable()) {
            if (cloudTokenPresentAndUnreported) {
                createInstallation(installation, actionListener);
                lastSyncTimeMillis = Time.now();
            }
        } else {
            if (installation.hasDataToReport()) {
                patchMyInstallation(installation, actionListener);
                lastSyncTimeMillis = Time.now();
            }
        }
        isSyncStarting = false;
    }

    private boolean didSyncRecently() {
        return isSyncStarting || lastSyncTimeMillis != null && Time.now() - lastSyncTimeMillis < SYNC_THROTTLE_INTERVAL_MILLIS;
    }

    public void updatePrimaryStatus(String pushRegId, Boolean primary, MobileMessaging.ResultListener<Installation> actionListener) {
        Installation installation = new Installation(pushRegId);
        installation.setPrimaryDevice(primary);
        patch(installation, actionListener, mobileMessagingCore.isMyInstallation(installation));
    }

    private void createInstallation(final Installation installation, final MobileMessaging.ResultListener<Installation> actionListener) {
        new MRetryableTask<Void, AppInstance>() {

            @Override
            public boolean shouldCancel() {
                return mobileMessagingCore.isRegistrationAvailable() || isSystemDataAbsent(installation);
            }

            @Override
            public AppInstance run(Void[] voids) {
                MobileMessagingLogger.v("CREATE INSTALLATION >>>", installation);
                mobileMessagingCore.setCloudTokenReported(true);
                final AppInstance appInstance = InstallationMapper.toBackend(installation);
                return mobileApiAppInstance.createInstance(appInstance);
            }

            @Override
            public void afterBackground(AppInstance appInstance) {
                MobileMessagingLogger.v("CREATE INSTALLATION DONE <<<", appInstance);

                if (appInstance == null) {
                    mobileMessagingCore.setCloudTokenReported(false);
                    return;
                }

                Installation installation = InstallationMapper.fromBackend(appInstance);
                setPushRegistrationId(installation.getPushRegistrationId());
                updateInstallationReported(installation, true);
            }

            @Override
            public void after(AppInstance appInstance) {
                String pushServiceToken = installation.getPushServiceToken();
                String pushRegId = appInstance.getPushRegId();
                appInstance.setPushServiceToken(pushServiceToken);
                Installation backendInstallation = InstallationMapper.fromBackend(appInstance);
                broadcaster.registrationCreated(pushServiceToken, pushRegId);
                if (actionListener != null) {
                    actionListener.onResult(new Result<>(backendInstallation));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("CREATE INSTALLATION ERROR <<<", error);

                mobileMessagingCore.setCloudTokenReported(false);
                stats.reportError(MobileMessagingStatsError.REGISTRATION_SYNC_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(error));

                if (actionListener != null) {
                    actionListener.onResult(new Result<>(mobileMessagingCore.getInstallation(true), MobileMessagingError.createFrom(error)));
                }
            }

            @Override
            public void cancelled(Void[] voids) {
                MobileMessagingLogger.v("CREATE INSTALLATION CANCELLED <<<");
                if (actionListener != null) {
                    actionListener.onResult(new Result<>(mobileMessagingCore.getInstallation(true)));
                }
            }
        }
                .retryWith(retryPolicyProvider.DEFAULT())
                .execute(executor);
    }

    private boolean isSystemDataAbsent(Installation installation) {
        return installation.getSdkVersion() == null ||
                installation.getDeviceManufacturer() == null ||
                installation.getOs() == null ||
                installation.getOsVersion() == null;
    }

    public void patchMyInstallation(@NonNull final Installation installation, final MobileMessaging.ResultListener<Installation> actionListener) {
        patch(installation, actionListener, true);
    }

    public void patch(@NonNull final Installation installation, final MobileMessaging.ResultListener<Installation> actionListener, final boolean myInstallation) {
        SystemData systemDataForReport = mobileMessagingCore.systemDataForReport(false);
        if (systemDataForReport != null && myInstallation) {
            mobileMessagingCore.populateInstallationWithSystemData(systemDataForReport, installation);
        }

        if (!installation.hasDataToReport()) {
            MobileMessagingLogger.w("Attempt to save empty installation data, will do nothing");
            if (actionListener != null) {
                actionListener.onResult(new Result<Installation, MobileMessagingError>(InternalSdkError.ERROR_SAVING_EMPTY_OBJECT.getError()));
            }
            return;
        }

        String pushRegId = mobileMessagingCore.getPushRegistrationId();
        if (!myInstallation) {
            pushRegId = installation.getPushRegistrationId();
        }

        if (StringUtils.isBlank(pushRegId)) {
            if (myInstallation)
                MobileMessagingLogger.w("Push registration ID is not available from the provided context, will patch installation data later");
            else
                MobileMessagingLogger.w("Push registration ID is not provided, unable to patch installation data");

            if (actionListener != null) {
                actionListener.onResult(new Result<>(mobileMessagingCore.getInstallation(true), InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        final String pushRegIdToUpdate = pushRegId;
        final Map<String, Object> installationMap = installation.getMap();
        new MRetryableTask<Void, Void>() {

            @Override
            public Void run(Void[] voids) {
                MobileMessagingLogger.v("UPDATE INSTALLATION >>>");
                mobileApiAppInstance.patchInstance(pushRegIdToUpdate, installationMap);
                return null;
            }

            @Override
            public void after(Void aVoid) {
                MobileMessagingLogger.v("UPDATE INSTALLATION DONE <<<");

                updateInstallationReported(installation, myInstallation);
                Installation installationToReturn = installation;
                if (myInstallation) {
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
                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);

                if (error instanceof BackendInvalidParameterException) {
                    mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
                }

                mobileMessagingCore.setCloudTokenReported(false);
                stats.reportError(MobileMessagingStatsError.REGISTRATION_SYNC_ERROR);
                broadcaster.error(mobileMessagingError);
                if (actionListener != null) {
                    actionListener.onResult(new Result<>(mobileMessagingCore.getInstallation(true), mobileMessagingError));
                }
            }
        }
                .retryWith(retryPolicyProvider.DEFAULT())
                .execute(executor);
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
            mobileMessagingCore.setPushRegistrationEnabled(installation.isPushRegistrationEnabled());
            mobileMessagingCore.setPushRegistrationEnabledReported(true);
        }
        if (installation.getPushServiceToken() != null) {
            mobileMessagingCore.setCloudToken(installation.getPushServiceToken());
        }
        mobileMessagingCore.setCloudTokenReported(true);
        mobileMessagingCore.setApplicationUserIdReported(true);

        Map<String, CustomAttributeValue> customAttsMap = mobileMessagingCore.getMergedUnreportedAndReportedCustomAtts();
        mobileMessagingCore.setUnreportedCustomAttributes(null);
        mobileMessagingCore.saveCustomAttributes(customAttsMap);

        mobileMessagingCore.setSystemDataReported();
        mobileMessagingCore.setReportedPushServiceType();
    }

    @SuppressWarnings("unchecked")
    public void fetchInstance(final MobileMessaging.ResultListener<Installation> listener) {
        if (!mobileMessagingCore.isRegistrationAvailable()) {
            MobileMessagingLogger.w("Registration not available yet, you can fetch installation when push registration ID becomes available");
            if (listener != null) {
                listener.onResult(new Result(mobileMessagingCore.getInstallation(), InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
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
                if (installation.isPushRegistrationEnabled() != null) {
                    mobileMessagingCore.setPushRegistrationEnabled(installation.isPushRegistrationEnabled());
                }
                mobileMessagingCore.saveCustomAttributes(installation.getCustomAttributes());

                if (listener != null) {
                    listener.onResult(new Result<>(installation));
                }
                mobileMessagingCore.setShouldRepersonalize(false);
                MobileMessagingLogger.v("GET INSTALLATION DONE <<<");
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);

                if (error instanceof BackendInvalidParameterException) {
                    mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
                }

                if (listener != null) {
                    listener.onResult(new Result<>(mobileMessagingCore.getInstallation(true), mobileMessagingError));
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

    private void setPushRegistrationId(String registrationId) {
        if (registrationId != null) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, registrationId);
        }
    }

    public boolean isCloudTokenReported() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED);
    }

    private PushInstallation from(SystemData data) {
        PushInstallation installation = new PushInstallation();
        return (PushInstallation) mobileMessagingCore.populateInstallationWithSystemData(data, installation);
    }
}
