/*
 * MobileMessagingConnectivityReceiverTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tjuric
 * @since 31/08/2017.
 */
@RunWith(AndroidJUnit4.class)
public class MobileMessagingConnectivityReceiverTest {

    private final MobileMessagingCore mmcMock = mock(MobileMessagingCore.class);
    private final Context contextMock = mock(Context.class);
    private final ConnectivityManager connectivityManagerMock = mock(ConnectivityManager.class);
    private MobileMessagingConnectivityReceiver mobileMessagingConnectivityReceiver;

    @Before
    public void setUp() throws Exception {
        reset(mmcMock, contextMock, connectivityManagerMock);

        //noinspection WrongConstant
        when(mmcMock.getApplicationCode()).thenReturn("someApplicationCode");
        when(contextMock.getSystemService(Mockito.eq(Context.CONNECTIVITY_SERVICE))).thenReturn(connectivityManagerMock);
        mobileMessagingConnectivityReceiver = new MobileMessagingConnectivityReceiver(mmcMock);
    }

    @Test
    public void test_should_perform_retry_sync_on_internet_connected() throws Exception {
        // Given
        Intent givenIntent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        givenIntent.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

        // When
        mobileMessagingConnectivityReceiver.onReceive(contextMock, givenIntent);

        // Then
        verify(mmcMock, times(1)).retrySyncOnNetworkAvailable();
    }

    @Test
    public void test_should_not_perform_retry_sync_on_internet_not_connected() throws Exception {
        // Given
        Intent givenIntent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
        givenIntent.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true);

        // When
        mobileMessagingConnectivityReceiver.onReceive(contextMock, givenIntent);

        // Then
        verify(mmcMock, never()).retrySyncOnNetworkAvailable();
    }
}
