package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author tjuric
 * @since 31/08/2017.
 */
public class MobileMessagingConnectivityReceiverTest extends MobileMessagingTestCase {

    private MobileMessagingConnectivityReceiver mobileMessagingConnectivityReceiver;
    private MobileMessagingCore core;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        core = mock(MobileMessagingCore.class);
        ConnectivityManager connectivityManagerMock = Mockito.mock(ConnectivityManager.class);

        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.CONNECTIVITY_SERVICE))).thenReturn(connectivityManagerMock);
        given(core.getInternetConnected()).willReturn(false);

        mobileMessagingConnectivityReceiver = new MobileMessagingConnectivityReceiver(core);
    }

    @Test
    public void test_should_perform_retry_sync_on_internet_connected() throws Exception {
        // Given
        Intent givenIntent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        givenIntent.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

        // When
        mobileMessagingConnectivityReceiver.onReceive(contextMock, givenIntent);

        // Then
        verify(core, times(1)).retrySync();
    }

    @Test
    public void test_should_not_perform_retry_sync_on_internet_not_connected() throws Exception {
        // Given
        Intent givenIntent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        givenIntent.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true);

        // When
        mobileMessagingConnectivityReceiver.onReceive(contextMock, givenIntent);

        // Then
        verify(core, never()).retrySync();
    }
}
