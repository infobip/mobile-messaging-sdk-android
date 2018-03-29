package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.telephony.TelephonyManager;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.BDDMockito;

import static junit.framework.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * @author sslavin
 * @since 29/03/2018.
 */

public class MobileNetworkInformationTest extends MobileMessagingTestCase {

    private TelephonyManager telephonyManager = mock(TelephonyManager.class);
    private Context context = mock(Context.class);
    private String invalidOperatorCodes[] = new String[]{null, "", "1", "12"};

    @Override
    public void setUp() throws Exception {
        super.setUp();

        given(context.getSystemService(eq(Context.TELEPHONY_SERVICE))).willReturn(telephonyManager);
        reset(telephonyManager);
    }

    @Test
    public void shouldReturnUnknown_whenReadingMobileCountryCode_ifNetworkOperatorIsInvalid() {
        givenMethodWillReturn(telephonyManager.getNetworkOperator(), invalidOperatorCodes);

        for (String ignored : invalidOperatorCodes) {
            assertEquals("unknown", MobileNetworkInformation.getMobileCountryCode(context));
        }
    }

    @Test
    public void shouldReturnUnknown_whenReadingSIMCountryCode_ifNetworkOperatorIsInvalid() {
        givenMethodWillReturn(telephonyManager.getSimOperator(), invalidOperatorCodes);

        for (String ignored : invalidOperatorCodes) {
            assertEquals("unknown", MobileNetworkInformation.getSIMCountryCode(context));
        }
    }

    @Test
    public void shouldReturnUnknown_whenReadingMobileNetworkCode_ifNetworkOperatorIsInvalid() {
        givenMethodWillReturn(telephonyManager.getNetworkOperator(), invalidOperatorCodes);

        for (String ignored : invalidOperatorCodes) {
            assertEquals("unknown", MobileNetworkInformation.getMobileNetworkCode(context));
        }
    }

    @Test
    public void shouldReturnUnknown_whenReadingSIMNetworkCode_ifNetworkOperatorIsInvalid() {
        givenMethodWillReturn(telephonyManager.getSimOperator(), invalidOperatorCodes);

        for (String ignored : invalidOperatorCodes) {
            assertEquals("unknown", MobileNetworkInformation.getSIMNetworkCode(context));
        }
    }

    private void givenMethodWillReturn(String method, String values[]) {
        BDDMockito.BDDMyOngoingStubbing<String> stubbing = given(method);
        for (String value : values) {
            stubbing = stubbing.willReturn(value);
        }
    }
}
