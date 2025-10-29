/*
 * DomainHelper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.net.URI;
import java.util.HashSet;

import androidx.annotation.NonNull;

public class DomainHelper {
    private final Context context;

    public DomainHelper(Context context) {
        this.context = context;
    }

    public boolean isTrustedDomain(@NonNull String url) {
        HashSet<String> trustedDomains = MobileMessagingCore.getInstance(context).getTrustedDomains();
        if (trustedDomains == null || trustedDomains.isEmpty()) return true;

        try {
            URI uri = new URI(url);
            String domainName = uri.getHost();
            for (String domain : trustedDomains) {
                if (domainName.equals(domain) || domainName.endsWith("." + domain)) return true;
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("Error parsing url: " + url, e);
        }

        return false;
    }
}
