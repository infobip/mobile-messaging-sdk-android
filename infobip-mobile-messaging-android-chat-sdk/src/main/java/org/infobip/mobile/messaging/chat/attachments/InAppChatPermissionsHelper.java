package org.infobip.mobile.messaging.chat.attachments;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.PreferenceHelper;

public class InAppChatPermissionsHelper {

    public static void checkPermission(@NonNull Context context, @NonNull String permission, InAppChatPermissionAskListener listener) {
        if (shouldAskPermission(context, permission)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((AppCompatActivity) context, permission)) {
                listener.onNeedPermission(context, permission);
            } else {
                if (isFirstTimeAsking(context, permission)) {
                    setFirstTimeAsking(context, permission, false);
                    listener.onNeedPermission(context, permission);
                } else {
                    listener.onPermissionPreviouslyDeniedWithNeverAskAgain(context, permission);
                }
            }
        } else {
            listener.onPermissionGranted(context, permission);
        }
    }

    public interface InAppChatPermissionAskListener {
        void onNeedPermission(Context context, String permission);

        void onPermissionPreviouslyDeniedWithNeverAskAgain(Context context, String permission);

        void onPermissionGranted(Context context, String permission);
    }

    public static boolean hasPermissionInManifest(@NonNull Context context, @NonNull String permissionName) {
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
            MobileMessagingLogger.e("[InAppChat] Package name not found", e);
        }
        return false;
    }

    private static boolean shouldAskPermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    private static boolean shouldAskPermission(@NonNull Context context, @NonNull String permission) {
        if (shouldAskPermission()) {
            int permissionResult = ActivityCompat.checkSelfPermission(context, permission);
            return permissionResult != PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    private static boolean isFirstTimeAsking(@NonNull Context context, @NonNull String permission) {
        Boolean isFirstTime = PreferenceHelper.findBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_PERMISSION_FIRST_TIME_ASK.getKey() + permission, true);
        return isFirstTime;
    }

    private static void setFirstTimeAsking(@NonNull Context context, @NonNull String permission, Boolean isFirstTime) {
        PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_PERMISSION_FIRST_TIME_ASK.getKey() + permission, isFirstTime);
    }
}