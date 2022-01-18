package org.infobip.mobile.messaging.chat.attachments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;
import androidx.fragment.app.Fragment;

import org.infobip.mobile.messaging.chat.R;

import java.util.Map;
import java.util.Set;

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
    }

    private static final int OPEN_SETTINGS_INTENT_CODE = 201;

    private final Activity context;
    private final PermissionsRequester permissionsRequester;
    private final ActivityResultLauncher<String[]> activityResultLauncher;

    public PermissionsRequestManager(Fragment fragment, @NonNull PermissionsRequester permissionsRequester) {
        this.context = fragment.getActivity();
        this.permissionsRequester = permissionsRequester;
        activityResultLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onRequestPermissionsResult);
    }

    public PermissionsRequestManager(AppCompatActivity activity, @NonNull PermissionsRequester permissionsRequester) {
        this.context = activity;
        this.permissionsRequester = permissionsRequester;
        activityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onRequestPermissionsResult);
    }

    public void onRequestPermissionsResult(@NonNull Map<String, Boolean> permissionsResult) {
        for (Map.Entry<String, Boolean> entry: permissionsResult.entrySet()) {
            if (!entry.getValue()) return;
        }
        permissionsRequester.onPermissionGranted();
    }

    public boolean isRequiredPermissionsGranted() {
        final Set<String> permissionsToAsk = new ArraySet<>();
        final Set<String> neverAskPermissions = new ArraySet<>();

        for (String permission : permissionsRequester.requiredPermissions()) {
            if (!InAppChatPermissionsHelper.hasPermissionInManifest(context, permission)) {
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
            activityResultLauncher.launch(permissionsToAskArray);
            return false;
        }
        return true;
    }

    private void checkPermission(String permission, final Set<String> permissionsToAsk, final Set<String> neverAskPermissions) {
        InAppChatPermissionsHelper.checkPermission(context, permission, new InAppChatPermissionsHelper.InAppChatPermissionAskListener() {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivityForResult(intent, OPEN_SETTINGS_INTENT_CODE);
    }
}
