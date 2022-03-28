package org.infobip.mobile.messaging.geo.permissions;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;
import androidx.fragment.app.Fragment;

import org.infobip.mobile.messaging.geo.R;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.permissions.PermissionsRequestManager;

import java.util.Set;

public class GeoPermissionsRequestManager extends PermissionsRequestManager {
    public GeoPermissionsRequestManager(AppCompatActivity activity, @NonNull PermissionsRequester permissionsRequester) {
       super(activity, permissionsRequester);
    }

    public GeoPermissionsRequestManager(Fragment fragment, @NonNull PermissionsRequester permissionsRequester) {
        super(fragment, permissionsRequester);
    }

    @Override
    public boolean isRequiredPermissionsGranted() {
        if (context == null) {
            MobileMessagingLogger.e("[Geofencing] context wasn't set");
            return false;
        }
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
            //This is one edge case for permission "Manifest.permission.ACCESS_BACKGROUND_LOCATION" for Android 10+,
            // it will show settings screen without any normal text to understand what's going on, that's why we will show dialog before, to make process more understandable.
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && permissionsToAsk.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                showUpdateLocationSettingsDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activityResultLauncher.launch(permissionsToAskArray);
                        dialog.dismiss();
                    }
                }, permissionsToAsk.toString());
            } else {
                activityResultLauncher.launch(permissionsToAskArray);
            }
            return false;
        }
        return true;
    }

    private void showUpdateLocationSettingsDialog(DialogInterface.OnClickListener onPositiveButtonClick, String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage(R.string.geofencing_update_location_settings_message)
                .setTitle(R.string.geofencing_update_location_settings_title)
                .setPositiveButton(org.infobip.mobile.messaging.R.string.mm_button_settings, onPositiveButtonClick)
                .setNegativeButton(org.infobip.mobile.messaging.R.string.mm_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }
}
