package org.infobip.mobile.messaging.platform;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.registration.PushServiceType;
import org.infobip.mobile.messaging.cloud.MobileMessageHandler;
import org.infobip.mobile.messaging.cloud.RegistrationTokenHandler;
import org.infobip.mobile.messaging.cloud.firebase.FirebaseRegistrationTokenHandler;
import org.infobip.mobile.messaging.cloud.gcm.GCMManifestHelper;
import org.infobip.mobile.messaging.cloud.gcm.GCMRegistrationTokenHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.ComponentUtil;

/**
 * This class will try to keep all common components which hold Android Context inside.
 *
 * @author sslavin
 * @since 03/09/2018.
 */
public class Platform {

    public static volatile Lazy<MobileMessagingCore, Context> mobileMessagingCore = createForConstructorAcceptingContext(MobileMessagingCore.class);
    public static volatile Lazy<AndroidBroadcaster, Context> broadcaster = createForConstructorAcceptingContext(AndroidBroadcaster.class);
    public static volatile Lazy<MobileMessageHandler, Context> mobileMessageHandler = create(new Lazy.Initializer<MobileMessageHandler, Context>() {
        @Override
        public MobileMessageHandler initialize(Context context) {
            MobileMessagingCore mobileMessagingCore = Platform.mobileMessagingCore.get(context);
            return new MobileMessageHandler(
                    mobileMessagingCore,
                    broadcaster.get(context),
                    mobileMessagingCore.getNotificationHandler(),
                    mobileMessagingCore.getMessageStoreWrapper());
        }
    });
    public static volatile Lazy<RegistrationTokenHandler, Context> registrationTokenHandler = create(new Lazy.Initializer<RegistrationTokenHandler, Context>() {
        @Override
        public RegistrationTokenHandler initialize(Context param) {
            return initializeTokenHandler(param);
        }
    });

    public static final PushServiceType usedPushServiceType = usedPushServiceType();
    public static final boolean shouldUseGCM = PushServiceType.GCM.equals(usedPushServiceType);

    public static <T> Lazy<T, Context> create(Lazy.Initializer<T, Context> initializer) {
        return Lazy.create(initializer);
    }

    public static <T> Lazy<T, Context> createForConstructorAcceptingContext(Class<T> cls) {
        return Lazy.fromSingleArgConstructor(cls, Context.class);
    }

    public static void reset(MobileMessagingCore mobileMessagingCore) {
        Platform.mobileMessagingCore = Lazy.just(mobileMessagingCore);
    }

    public static void verify(Context context) {
        ComponentUtil.verifyManifestComponentsForPush(context);
        if (shouldUseGCM) {
            GCMManifestHelper.verifyAndConfigureManifest(context);
        }
    }

    @VisibleForTesting
    protected static void reset(AndroidBroadcaster broadcaster) {
        Platform.broadcaster = Lazy.just(broadcaster);
    }

    @VisibleForTesting
    protected static void reset(MobileMessageHandler mobileMessageHandler) {
        Platform.mobileMessageHandler = Lazy.just(mobileMessageHandler);
    }

    private static RegistrationTokenHandler initializeTokenHandler(Context context) {
        if (shouldUseGCM) {
            return new GCMRegistrationTokenHandler(mobileMessagingCore.get(context), broadcaster.get(context), context);
        } else {
            return new FirebaseRegistrationTokenHandler(mobileMessagingCore.get(context), broadcaster.get(context));
        }
    }

    private static PushServiceType usedPushServiceType() {
        PushServiceType usedPushServiceType = PushServiceType.Firebase;
        try {
            Class.forName("com.google.android.gms.iid.InstanceIDListenerService");
            usedPushServiceType = PushServiceType.GCM;
        } catch (ClassNotFoundException ignored) {
        }
        MobileMessagingLogger.d("Will use " + usedPushServiceType.name() + " for messaging");
        return usedPushServiceType;
    }
}
