package org.infobip.mobile.messaging.mobile;

import org.infobip.mobile.messaging.api.support.ApiBackendException;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class MobileMessagingErrorTest extends MobileMessagingTestCase {

    @Test
    public void test_mobileMessagingServerError() throws Exception {
        MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(new ApiBackendException("3", "SomeWeirdError"));

        assertEquals(MobileMessagingError.Type.SERVER_ERROR, mobileMessagingError.getType());
        assertEquals("3", mobileMessagingError.getCode());
        assertEquals("SomeWeirdError", mobileMessagingError.getMessage());
    }

    @Test
    public void test_unknownError() throws Exception {
        MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(new Exception("Some exception"));

        assertEquals(MobileMessagingError.Type.UNKNOWN_ERROR, mobileMessagingError.getType());
        assertEquals("-10", mobileMessagingError.getCode());
        assertEquals("Some exception", mobileMessagingError.getMessage());
    }
}
