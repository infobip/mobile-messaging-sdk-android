/*
 * PermissionsRequestManager.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.permissions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.resources.R;

import java.util.Map;
import java.util.Set;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.collection.ArraySet;
import androidx.fragment.app.Fragment;

public class PermissionsRequestManager {
    public interface PermissionsRequester {

        /**
         * This method will be called when required permissions are granted.
         */
        void onPermissionGranted();

        /**
         * Provide permissions which you need to request.
         * <br>
         * For example:
         * <pre>
         * {@code
         * new String[]{Manifest.permission.CAMERA}
         * </pre>
         **/
        @NonNull
        String[] requiredPermissions();

        /**
         * Should application show the dialog with information that not all required permissions are granted and button which leads to the settings for granting permissions after it was already shown once.
         * Recommendations:
         * - If you are asking for permissions by button tap, better to return true, so user will be informed, why an action can't be done, if the user didn't grant the permissions.
         * - If you are asking for permissions on the application start, without any additional user actions, better to return false not to disturb the user constantly.
         **/
        boolean shouldShowPermissionsNotGrantedDialogIfShownOnce();

        /**
         * This method is for providing custom title for the permissions dialog.
         *
         * @return reference to string resource for permissions dialog title
         */
        @StringRes
        int permissionsNotGrantedDialogTitle();

        /**
         * This method is for providing custom message for the permissions dialog.
         *
         * @return reference to string resource for permissions dialog message
         */
        @StringRes
        int permissionsNotGrantedDialogMessage();
    }

    protected final ComponentActivity context;
    protected PermissionsRequester permissionsRequester;
    protected final ActivityResultLauncher<String[]> activityResultLauncher;
    protected final ActivityResultLauncher<Intent> settingsActivityLauncher;
    protected PermissionsHelper permissionsHelper;

    public PermissionsRequestManager(ComponentActivity activity, @NonNull PermissionsRequester permissionsRequester) {
        this.context = activity;
        activityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onRequestPermissionsResult);
        settingsActivityLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onSettingsResult);
        this.permissionsRequester = permissionsRequester;
        this.permissionsHelper = new PermissionsHelper();
    }

    public PermissionsRequestManager(Fragment fragment, @NonNull PermissionsRequester permissionsRequester) {
        this.context = fragment.getActivity();
        activityResultLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onRequestPermissionsResult);
        settingsActivityLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onSettingsResult);
        this.permissionsRequester = permissionsRequester;
        this.permissionsHelper = new PermissionsHelper();
    }

    public void onRequestPermissionsResult(@NonNull Map<String, Boolean> permissionsResult) {
        for (Map.Entry<String, Boolean> entry : permissionsResult.entrySet()) {
            if (!entry.getValue()) return;
        }
        permissionsRequester.onPermissionGranted();
    }

    public void onSettingsResult(ActivityResult result) {
        MobileMessagingLogger.d("Did open application settings activity with result" + result.getResultCode());
    }

    public boolean isRequiredPermissionsGranted() {
        final Set<String> permissionsToAsk = new ArraySet<>();
        final Set<String> neverAskPermissions = new ArraySet<>();

        for (String permission : permissionsRequester.requiredPermissions()) {
            if (!permissionsHelper.hasPermissionInManifest(context, permission)) {
                return false;
            }
            checkPermission(permission, permissionsToAsk, neverAskPermissions);
        }

        if (neverAskPermissions.size() > 0) {
            showSettingsDialog((dialog, which) -> {
                openSettings();
                dialog.dismiss();
            }, neverAskPermissions.toString());
            return false;
        }
        String[] permissionsToAskArray = new String[permissionsToAsk.size()];
        permissionsToAsk.toArray(permissionsToAskArray);
        if (permissionsToAsk.size() > 0) {
            activityResultLauncher.launch(permissionsToAskArray);
            return false;
        }
        return true;
    }

    protected void checkPermission(String permission, final Set<String> permissionsToAsk, final Set<String> neverAskPermissions) {
        permissionsHelper.checkPermission(context, permission, new PermissionsHelper.PermissionsRequestListener() {
            @Override
            public void onNeedPermission(Activity activity, String permission) {
                permissionsToAsk.add(permission);
            }

            @Override
            public void onPermissionPreviouslyDeniedWithNeverAskAgain(Activity activity, String permission) {
                neverAskPermissions.add(permission);
            }

            @Override
            public void onPermissionGranted(Activity activity, String permission) {
            }
        });
    }

    protected void showSettingsDialog(DialogInterface.OnClickListener onPositiveButtonClick, String permission) {
        if (!permissionsHelper.isPermissionSettingsDialogShown(context, permission) ||
                permissionsRequester.shouldShowPermissionsNotGrantedDialogIfShownOnce()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setMessage(permissionsRequester.permissionsNotGrantedDialogMessage())
                    .setTitle(permissionsRequester.permissionsNotGrantedDialogTitle())
                    .setPositiveButton(R.string.mm_button_settings, onPositiveButtonClick)
                    .setNegativeButton(R.string.mm_button_cancel, (dialog, which) -> dialog.dismiss());
            builder.show();
            permissionsHelper.setPermissionSettingsDialogShown(context, permission, true);
        }
    }

    protected void openSettings() {
        MobileMessagingLogger.d("Will open application settings activity");
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        settingsActivityLauncher.launch(intent);
    }
}
