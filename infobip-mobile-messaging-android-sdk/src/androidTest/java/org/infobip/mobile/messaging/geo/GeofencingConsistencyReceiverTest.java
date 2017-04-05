package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by tjuric on 31/03/2017.
 */

public class GeofencingConsistencyReceiverTest extends MobileMessagingTestCase {

    private LocationManager locationManagerMock;
    private GeofencingConsistencyReceiver geofencingConsistencyReceiver;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        locationManagerMock = Mockito.mock(LocationManager.class);
        geofencingConsistencyReceiver = new GeofencingConsistencyReceiver();
    }

    @Test
    public void test_shouldNotProduceSecurityExceptionWithoutPermission_whenProvidersChangedAndGeoDeactivated() {
        // Given
        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.LOCATION_SERVICE))).thenThrow(new SecurityException());
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, false);
        Intent intent = new Intent(LocationManager.PROVIDERS_CHANGED_ACTION);
        Exception exception = null;

        // When
        try {
            geofencingConsistencyReceiver.onReceive(contextMock, intent);
        } catch (Exception ex) {
            exception = ex;
        }

        // Then
        assertNull(exception);
        Mockito.verify(locationManagerMock, Mockito.never()).isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Test
    public void test_shouldProduceSecurityExceptionWithoutPermission_whenProvidersChangedAndGeoActivated() {
        // Given
        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.LOCATION_SERVICE))).thenThrow(new SecurityException());
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, true);
        Intent intent = new Intent(LocationManager.PROVIDERS_CHANGED_ACTION);
        Exception exception = null;

        // When
        try {
            geofencingConsistencyReceiver.onReceive(contextMock, intent);
        } catch (Exception ex) {
            exception = ex;
        }

        // Then
        assertNotNull(exception);
        assertTrue(exception instanceof SecurityException);
        Mockito.verify(locationManagerMock, Mockito.never()).isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Test
    public void test_shouldCallProvidersEnabledWithPermission_whenProvidersChangedAndGeoActivated() {
        // Given
        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.LOCATION_SERVICE))).thenReturn(locationManagerMock);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, true);
        Intent providersChangedIntent = new Intent(LocationManager.PROVIDERS_CHANGED_ACTION);
        Exception exception = null;

        // When
        try {
            geofencingConsistencyReceiver.onReceive(contextMock, providersChangedIntent);
        } catch (Exception e) {
            exception = e;
        }

        // Then
        assertNull(exception);
        Mockito.verify(locationManagerMock, Mockito.atMost(1)).isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
