package org.infobip.mobile.messaging.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.PreferenceHelper;

public class PermissionsHelper {
    public PermissionsHelper() {}

    public void checkPermission(@NonNull Activity activity, @NonNull String permission, PermissionsHelper.PermissionsRequestListener listener) {
        if (shouldAskPermission(activity, permission)) {
            if (shouldShowRequestPermissionRationale(activity, permission)) {
                listener.onNeedPermission(activity, permission);
            } else {
                if (isFirstTimeAsking(activity, permission)) {
                    setFirstTimeAsking(activity, permission, false);
                    listener.onNeedPermission(activity, permission);
                } else {
                    listener.onPermissionPreviouslyDeniedWithNeverAskAgain(activity, permission);
                }
            }
        } else {
            listener.onPermissionGranted(activity, permission);
        }
    }

    public boolean shouldShowRequestPermissionRationale(Activity context, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(context, permission);
    }

    public interface PermissionsRequestListener {
        void onNeedPermission(Activity activity, String permission);

        void onPermissionPreviouslyDeniedWithNeverAskAgain(Activity activity, String permission);

        void onPermissionGranted(Activity activity, String permission);
    }

    public boolean hasPermissionInManifest(@NonNull Context context, @NonNull String permissionName) {
        String packageName = context.getPackageName();
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            String[] declaredPermissions = packageInfo.requestedPermissions;
            if (declaredPermissions == null) {
                return false;
            }
            for (String declaredPermission : declaredPermissions) {
                if (declaredPermission.equals(permissionName)) {
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            MobileMessagingLogger.e("[MobileMessaging] Package name not found", e);
        }
        return false;
    }

    private boolean shouldAskPermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    private boolean shouldAskPermission(@NonNull Context context, @NonNull String permission) {
        if (shouldAskPermission()) {
            int permissionResult = ActivityCompat.checkSelfPermission(context, permission);
            return permissionResult != PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public boolean isFirstTimeAsking(@NonNull Context context, @NonNull String permission) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.KEY_PERMISSION_REQUESTED_FIRST_TIME + permission, true);
    }

    public void setFirstTimeAsking(@NonNull Context context, @NonNull String permission, Boolean isFirstTime) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.KEY_PERMISSION_REQUESTED_FIRST_TIME + permission, isFirstTime);
    }

    public boolean isPermissionSettingsDialogShown(@NonNull Context context, @NonNull String permission) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.KEY_PERMISSIONS_SETTINGS_DIALOG_WAS_SHOWN + permission, false);
    }

    public void setPermissionSettingsDialogShown(@NonNull Context context, @NonNull String permission, Boolean shown) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.KEY_PERMISSIONS_SETTINGS_DIALOG_WAS_SHOWN + permission, shown);
    }
}
