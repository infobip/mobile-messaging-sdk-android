package org.infobip.mobile.messaging.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;

import java.util.HashSet;

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
}
