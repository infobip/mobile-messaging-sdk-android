/*
 * DomainHelperTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.MobileMessagingTestable;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DomainHelperTest extends MobileMessagingTestCase {

    @Test
    public void shouldAllowWithoutDomains() {
        DomainHelper domainHelper = new DomainHelper(context);

        assertTrue(domainHelper.isTrustedDomain("https://example.com"));
    }

    @Test
    public void shouldNotAllowUnknownDomain() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com");

        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        DomainHelper domainHelper = new DomainHelper(context);

        assertFalse(domainHelper.isTrustedDomain("https://www.bla.com"));
    }

    @Test
    public void shouldAllowSubdomain() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com");
        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        DomainHelper domainHelper = new DomainHelper(context);

        assertTrue(domainHelper.isTrustedDomain("https://some.example.com"));
    }

    @Test
    public void shouldAllowComplicatedSubdomain() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com.au");
        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        DomainHelper domainHelper = new DomainHelper(context);

        assertTrue(domainHelper.isTrustedDomain("http://www.some.example.com.au"));
    }

    @Test
    public void shouldNotAllowFishySubdomain() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com.au");
        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        DomainHelper domainHelper = new DomainHelper(context);

        assertFalse(domainHelper.isTrustedDomain("http://some.example.com.au.fishy"));
    }

    @Test
    public void shouldNotAllowSneakyDomain() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com");
        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        DomainHelper domainHelper = new DomainHelper(context);

        assertFalse(domainHelper.isTrustedDomain("http://sneaky-example.com"));
    }

    @Test
    public void shouldPersistTrustedDomains() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com");
        trustedDomains.add("trusted.org");
        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        assertEquals(trustedDomains, PreferenceHelper.findStringSet(context, MobileMessagingProperty.TRUSTED_DOMAINS));
    }

    @Test
    public void shouldNotAllowUnknownDomain_afterProcessRestart() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com");
        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        simulateProcessRestart();

        DomainHelper domainHelper = new DomainHelper(context);
        assertFalse(domainHelper.isTrustedDomain("https://www.bla.com"));
        assertTrue(domainHelper.isTrustedDomain("https://some.example.com"));
    }

    @Test
    public void shouldRemovePersistedTrustedDomains_whenSetToNull() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com");
        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        MobileMessagingCore.getInstance(context).setTrustedDomains(null);
        simulateProcessRestart();

        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.TRUSTED_DOMAINS));
        assertNull(MobileMessagingCore.getInstance(context).getTrustedDomains());
        assertTrue(new DomainHelper(context).isTrustedDomain("https://www.bla.com"));
    }

    @Test
    public void shouldRemovePersistedTrustedDomains_whenSetToEmpty() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com");
        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        MobileMessagingCore.getInstance(context).setTrustedDomains(new HashSet<>());
        simulateProcessRestart();

        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.TRUSTED_DOMAINS));
        assertTrue(new DomainHelper(context).isTrustedDomain("https://www.bla.com"));
    }

    @Test
    public void shouldNotChangeTrustedDomains_whenSetsOutsideOfSdkAreModified() {
        HashSet<String> trustedDomains = new HashSet<>();
        trustedDomains.add("example.com");
        MobileMessagingCore.getInstance(context).setTrustedDomains(trustedDomains);

        trustedDomains.add("bla.com");
        MobileMessagingCore.getInstance(context).getTrustedDomains().add("bla.com");

        HashSet<String> expectedTrustedDomains = new HashSet<>();
        expectedTrustedDomains.add("example.com");
        assertEquals(expectedTrustedDomains, MobileMessagingCore.getInstance(context).getTrustedDomains());
        assertEquals(expectedTrustedDomains, PreferenceHelper.findStringSet(context, MobileMessagingProperty.TRUSTED_DOMAINS));
        assertFalse(new DomainHelper(context).isTrustedDomain("https://www.bla.com"));
    }

    /**
     * New core instance without in-memory trusted domains, same as the one created by {@link MobileMessagingCore#getInstance}
     * when a push message starts the application process before the SDK is initialized.
     */
    private void simulateProcessRestart() {
        MobileMessagingTestable.create(context, broadcaster, mobileApiResourceProvider, firebaseAppProvider);
    }
}
