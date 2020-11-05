package org.infobip.mobile.messaging.chat.attachments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;

import org.infobip.mobile.messaging.chat.R;

import java.util.Set;

public abstract class PermissionsRequesterActivity extends AppCompatActivity {
    private static final int IN_APP_CHAT_PERMISSIONS_REQUEST_CODE = 200;
    private static final int OPEN_SETTINGS_INTENT_CODE = 201;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == IN_APP_CHAT_PERMISSIONS_REQUEST_CODE && isPermissionGranted(grantResults)) {
            onPermissionGranted();
        }
    }

    /**
    This method will be called when required permissions granted.
     **/
    public abstract void onPermissionGranted();

    /**
    Provide permissions which you need to request.
    For example: new String[]{Manifest.permission.CAMERA}
     **/
    @NonNull
    public abstract String[] requiredPermissions();

    public boolean isRequiredPermissionsGranted() {
        final Set<String> permissionsToAsk = new ArraySet<String>();
        final Set<String> neverAskPermissions = new ArraySet<String>();

        for (String permission : requiredPermissions()) {
            if (!InAppChatPermissionsHelper.hasPermissionInManifest(this, permission)) {
                return false;
            }
            checkPermission(permission, permissionsToAsk, neverAskPermissions);
        }

        if (neverAskPermissions.size() > 0) {
            showSettingsDialog(neverAskPermissions);
            return false;
        }
        String[] permissionsToAskArray = new String[permissionsToAsk.size()];
        permissionsToAsk.toArray(permissionsToAskArray);
        if (permissionsToAsk.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsToAskArray, IN_APP_CHAT_PERMISSIONS_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void checkPermission(String permission, final Set<String> permissionsToAsk, final Set<String> neverAskPermissions) {
        InAppChatPermissionsHelper.checkPermission(this, permission, new InAppChatPermissionsHelper.InAppChatPermissionAskListener() {
            @Override
            public void onNeedPermission(Context context, String permission) {
                permissionsToAsk.add(permission);
            }

            @Override
            public void onPermissionPreviouslyDeniedWithNeverAskAgain(Context context, String permission) {
                neverAskPermissions.add(permission);
            }

            @Override
            public void onPermissionGranted(Context context, String permission) {
            }
        });
    }

    private void showSettingsDialog(Set<String> permissions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ib_chat_permissions_not_granted_title)
                .setMessage(R.string.ib_chat_permissions_not_granted_message)
                .setPositiveButton(R.string.ib_chat_button_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        openSettings();
                    }
                })
                .setNegativeButton(R.string.mm_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void openSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, OPEN_SETTINGS_INTENT_CODE);
    }

    private boolean isPermissionGranted(@NonNull int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}
