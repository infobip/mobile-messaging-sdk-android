package org.infobip.mobile.messaging.tools;

import junit.framework.TestCase;

import org.mockito.Mockito;

/**
 * @author sslavin
 * @since 02/03/2017.
 */

public class BrockitoTest extends TestCase {

    public void test_getTimeout_success() {
        assertEquals(1000, Brockito.getTimeout(Mockito.after(1000).times(1)));
    }

    public void test_getTimeout_error() {
        assertEquals(-1, Brockito.getTimeout(Mockito.times(1)));
    }

    public void test_getTimes_success() {
        assertEquals(4, Brockito.getTimes(Mockito.times(4)));
    }
}
