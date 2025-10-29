/*
 * PermissionsManagerTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.permissions;

import android.Manifest;
import android.content.pm.PackageManager;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import androidx.appcompat.app.AppCompatActivity;

import static org.mockito.Mockito.doReturn;

public class PermissionsManagerTest extends MobileMessagingTestCase {
    private PermissionsHelper.PermissionsRequestListener permissionsRequestListenerMock;
    private PermissionsHelper permissionsHelperMock;
    private AppCompatActivity appCompatActivityMock;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        appCompatActivityMock = Mockito.mock(AppCompatActivity.class);
        permissionsRequestListenerMock = Mockito.mock(PermissionsHelper.PermissionsRequestListener.class);
        permissionsHelperMock = Mockito.spy(PermissionsHelper.class);
    }

    @Test
    public void onPermissionRequestListenerNeedPermissionTest() {
        String permissionToTest = Manifest.permission.ACCESS_FINE_LOCATION;
        doReturn(true).when(permissionsHelperMock).shouldShowRequestPermissionRationale(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        Mockito.when(appCompatActivityMock.checkPermission(Mockito.eq(permissionToTest), Mockito.anyInt(), Mockito.anyInt())).thenReturn(PackageManager.PERMISSION_DENIED);
        doReturn(true).when(permissionsHelperMock).isFirstTimeAsking(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));

        permissionsHelperMock.checkPermission(appCompatActivityMock, permissionToTest, permissionsRequestListenerMock);

        Mockito.verify(permissionsRequestListenerMock, Mockito.times(1)).onNeedPermission(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        Mockito.verify(permissionsRequestListenerMock, Mockito.times(0)).onPermissionGranted(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        Mockito.verify(permissionsRequestListenerMock, Mockito.times(0)).onPermissionPreviouslyDeniedWithNeverAskAgain(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
    }

    @Test
    public void onPermissionRequestListenerPermissionGrantedTest() {
        String permissionToTest = Manifest.permission.ACCESS_FINE_LOCATION;
        doReturn(true).when(permissionsHelperMock).shouldShowRequestPermissionRationale(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        Mockito.when(appCompatActivityMock.checkPermission(Mockito.eq(permissionToTest), Mockito.anyInt(), Mockito.anyInt())).thenReturn(PackageManager.PERMISSION_GRANTED);
        doReturn(true).when(permissionsHelperMock).isFirstTimeAsking(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));

        permissionsHelperMock.checkPermission(appCompatActivityMock, permissionToTest, permissionsRequestListenerMock);

        Mockito.verify(permissionsRequestListenerMock, Mockito.times(0)).onNeedPermission(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        Mockito.verify(permissionsRequestListenerMock, Mockito.times(1)).onPermissionGranted(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        Mockito.verify(permissionsRequestListenerMock, Mockito.times(0)).onPermissionPreviouslyDeniedWithNeverAskAgain(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
    }

    @Test
    public void onPermissionRequestListenerNeverAskTest() {
        String permissionToTest = Manifest.permission.ACCESS_FINE_LOCATION;
        doReturn(false).when(permissionsHelperMock).shouldShowRequestPermissionRationale(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        doReturn(false).when(permissionsHelperMock).isFirstTimeAsking(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        Mockito.when(appCompatActivityMock.checkPermission(Mockito.eq(permissionToTest), Mockito.anyInt(), Mockito.anyInt())).thenReturn(PackageManager.PERMISSION_DENIED);

        permissionsHelperMock.checkPermission(appCompatActivityMock, permissionToTest, permissionsRequestListenerMock);

        Mockito.verify(permissionsRequestListenerMock, Mockito.times(0)).onNeedPermission(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        Mockito.verify(permissionsRequestListenerMock, Mockito.times(0)).onPermissionGranted(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
        Mockito.verify(permissionsRequestListenerMock, Mockito.times(1)).onPermissionPreviouslyDeniedWithNeverAskAgain(Mockito.any(AppCompatActivity.class), Mockito.eq(permissionToTest));
    }
}
