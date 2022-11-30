package org.infobip.mobile.messaging.permissions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.infobip.mobile.messaging.R;

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

    private static final int OPEN_SETTINGS_INTENT_CODE = 201;

    protected final FragmentActivity context;
    protected PermissionsRequester permissionsRequester;
    protected final ActivityResultLauncher<String[]> activityResultLauncher;
    protected PermissionsHelper permissionsHelper;

    public PermissionsRequestManager(AppCompatActivity activity, @NonNull PermissionsRequester permissionsRequester) {
        this.context = activity;
        activityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onRequestPermissionsResult);
        this.permissionsRequester = permissionsRequester;
        this.permissionsHelper = new PermissionsHelper();
    }

    public PermissionsRequestManager(Fragment fragment, @NonNull PermissionsRequester permissionsRequester) {
        this.context = fragment.getActivity();
        activityResultLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onRequestPermissionsResult);
        this.permissionsRequester = permissionsRequester;
        this.permissionsHelper = new PermissionsHelper();
    }

    public void onRequestPermissionsResult(@NonNull Map<String, Boolean> permissionsResult) {
        for (Map.Entry<String, Boolean> entry : permissionsResult.entrySet()) {
            if (!entry.getValue()) return;
        }
        permissionsRequester.onPermissionGranted();
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
            showSettingsDialog(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openSettings();
                    dialog.dismiss();
                }
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
                    .setNegativeButton(R.string.mm_button_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
            permissionsHelper.setPermissionSettingsDialogShown(context, permission, true);
        }
    }

    protected void openSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivityForResult(intent, OPEN_SETTINGS_INTENT_CODE);
    }
}
