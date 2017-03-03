package org.infobip.mobile.messaging.gcm;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * @author sslavin
 * @since 25/05/16.
 */
public class MobileMessagingInstanceIDListenerService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        final RegistrationTokenHandler registrationTokenHandler = new RegistrationTokenHandler();
        registrationTokenHandler.handleRegistrationTokenUpdate(this);
    }
}
