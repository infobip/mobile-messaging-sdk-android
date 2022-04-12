package org.infobip.mobile.messaging.demo;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.cryptor.ECBCryptorImpl;
import org.infobip.mobile.messaging.util.DeviceInformation;

public class CryptorMigrationApplication extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ECBCryptorImpl oldCryptor = new ECBCryptorImpl(DeviceInformation.getDeviceID(this));
        new MobileMessaging.Builder(this)
                .withCryptorMigration(oldCryptor)
                .build();
    }
}
