package org.infobip.mobile.messaging.geo;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.test.mock.MockContentResolver;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author tjuric
 * @since 26/09/17.
 */

public class GeoEnabledConsistencyReceiverTest extends MobileMessagingTestCase {

    private LocationManager locationManagerMock;
    private GeoEnabledConsistencyReceiver geoEnabledConsistencyReceiverWithMock;
    private GeoEnabledConsistencyReceiver geoEnabledConsistencyReceiverWithSpy;
    private GeofencingHelper geoHelperMock;
    private GeofencingHelper geoHelperSpy;
    private Intent providersChangedIntent;
    private Exception exception = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        providersChangedIntent = new Intent(LocationManager.PROVIDERS_CHANGED_ACTION);

        locationManagerMock = Mockito.mock(LocationManager.class);
        geoHelperMock = Mockito.mock(GeofencingHelper.class);
        geoEnabledConsistencyReceiverWithMock = new GeoEnabledConsistencyReceiver(geoHelperMock);
        geoHelperSpy = Mockito.spy(GeofencingHelper.class);
        geoEnabledConsistencyReceiverWithSpy = new GeoEnabledConsistencyReceiver(geoHelperSpy);

        AlarmManager alarmManagerMock = Mockito.mock(AlarmManager.class);
        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.ALARM_SERVICE))).thenReturn(alarmManagerMock);
    }

    @Test
    public void test_shouldNotProduceSecurityExceptionWithoutPermission_whenGeoDeactivated() {
        // Given
        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.LOCATION_SERVICE))).thenThrow(new SecurityException());
        Mockito.when(geoHelperMock.isLocationEnabled(contextMock)).thenReturn(false);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, false);

        // When
        try {
            geoEnabledConsistencyReceiverWithMock.onReceive(contextMock, providersChangedIntent);
        } catch (Exception ex) {
            exception = ex;
        }

        // Then
        assertNull(exception);
        Mockito.verify(geoHelperMock, Mockito.never()).isLocationEnabled(context);
        Mockito.verify(locationManagerMock, Mockito.never()).isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Test
    public void test_shouldProduceSecurityExceptionWithoutPermission_whenGeoActivated() {
        // Given
        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.LOCATION_SERVICE))).thenThrow(new SecurityException());
        Mockito.when(geoHelperSpy.isKitKatOrAbove()).thenReturn(false);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        // When
        try {
            geoEnabledConsistencyReceiverWithSpy.onReceive(contextMock, providersChangedIntent);
        } catch (Exception ex) {
            exception = ex;
        }

        // Then
        assertNotNull(exception);
        assertTrue(exception instanceof SecurityException);
        Mockito.verify(geoHelperSpy, Mockito.times(1)).isLocationEnabled(contextMock);
        Mockito.verify(geoHelperSpy, Mockito.times(1)).isNetworkProviderAvailable(contextMock);
        Mockito.verify(locationManagerMock, Mockito.never()).isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Test
    public void test_shouldCallProvidersEnabledWithPermission_whenGeoActivated() {
        // Given
        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.LOCATION_SERVICE))).thenReturn(locationManagerMock);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        // When
        geoEnabledConsistencyReceiverWithMock.onReceive(contextMock, providersChangedIntent);

        // Then
        assertNull(exception);
        Mockito.verify(locationManagerMock, Mockito.atMost(1)).isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Test
    public void test_shouldFirstCallIsLocationModeOn_whenVersionKitKatOrAboveAndGeoActivated() throws Settings.SettingNotFoundException {
        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.LOCATION_SERVICE))).thenReturn(locationManagerMock);
        MockContentResolver mockContentResolver = new MockContentResolver();
        Mockito.when(contextMock.getContentResolver()).thenReturn(mockContentResolver);
        Mockito.when(geoHelperSpy.isLocationModeOn(context)).thenReturn(true);
        Mockito.when(geoHelperSpy.isKitKatOrAbove()).thenReturn(true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        // When
        geoEnabledConsistencyReceiverWithSpy.onReceive(context, providersChangedIntent);

        // Then
        Mockito.verify(geoHelperSpy, Mockito.times(1)).isLocationModeOn(context);
        Mockito.verify(geoHelperSpy, Mockito.never()).isNetworkProviderAvailable(context);
    }

    @Test
    public void test_shouldOnlyCallIsNetworkProviderAvailable_whenVersionBelowKitKatAndGeoActivated() throws Settings.SettingNotFoundException {
        //noinspection WrongConstant
        Mockito.when(contextMock.getSystemService(Mockito.eq(Context.LOCATION_SERVICE))).thenReturn(locationManagerMock);
        Mockito.when(geoHelperSpy.isKitKatOrAbove()).thenReturn(false);

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        // When
        geoEnabledConsistencyReceiverWithSpy.onReceive(contextMock, providersChangedIntent);

        // Then
        Mockito.verify(geoHelperSpy, Mockito.times(1)).isLocationEnabled(contextMock);
        Mockito.verify(geoHelperSpy, Mockito.times(1)).isNetworkProviderAvailable(contextMock);
        Mockito.verify(geoHelperSpy, Mockito.never()).isLocationModeOn(contextMock);
    }
}
