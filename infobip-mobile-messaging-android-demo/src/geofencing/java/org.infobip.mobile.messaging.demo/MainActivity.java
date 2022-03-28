package org.infobip.mobile.messaging.demo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.infobip.mobile.messaging.geo.MobileGeo;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.PreferenceHelper;

public class MainActivity extends AppCompatActivity {
    private final String KEY_GEO_PERMISSION_DIALOG_ACCEPTED = "KEY_GEO_PERMISSION_DIALOG_ACCEPTED";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(this.<Toolbar>findViewById(R.id.toolbar));

        // In order to be able to request permissions automatically provide activity or fragment which will be used for requesting permissions.
        // Activity or fragment for requesting permissions should be provided before your fragment or activity is created
        MobileGeo.getInstance(this).setContextForRequestingPermissions(this);

        // Checking that dialog isn't yet accepted, it'll be displayed only if it's not accepted.
        if (isGeoPermissionDialogAccepted(this)) {

            //This code will start permissions request process in case required permissions aren't yet accepted.
            MobileGeo.getInstance(MainActivity.this).activateGeofencingWithAutomaticPermissionsRequest(false);
            return;
        }

        //Show Geo Permissions dialog, until it's accepted.
        showGeofencingDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setGeoPermissionDialogAccepted(MainActivity.this, true);

                //This code will start permissions request process in case required permissions aren't yet accepted.
                MobileGeo.getInstance(MainActivity.this).activateGeofencingWithAutomaticPermissionsRequest(false);
            }
        });
    }

    // As per Google requirements we should show prominent disclosure dialog, before asking for geo permissions.
    private void showGeofencingDialog(DialogInterface.OnClickListener onPositiveButtonClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.geofencing_alert_message)
                .setPositiveButton(org.infobip.mobile.messaging.R.string.mm_button_accept, onPositiveButtonClick)
                .setNegativeButton(org.infobip.mobile.messaging.R.string.mm_button_decline, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private boolean isGeoPermissionDialogAccepted(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_GEO_PERMISSION_DIALOG_ACCEPTED, false);
    }

    private void setGeoPermissionDialogAccepted(Context context, boolean accepted) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_GEO_PERMISSION_DIALOG_ACCEPTED, accepted).apply();
    }
}
