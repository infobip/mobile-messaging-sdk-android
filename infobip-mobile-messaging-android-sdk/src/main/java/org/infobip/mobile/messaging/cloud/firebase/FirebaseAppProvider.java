package org.infobip.mobile.messaging.cloud.firebase;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * FirebaseAppProvider provides FirebaseApp either by taking FirebaseOptions from google-services.json file or from Resource strings.
 *  Resource string keys are:
 * `ib_project_id`
 * `ib_google_api_key`
 * `ib_google_app_id`
 */
public class FirebaseAppProvider {

    public Context getContext() {
        return context;
    }

    private Context context;

    public FirebaseAppProvider(Context context) {
        this.context = context;
    }

    public FirebaseApp getFirebaseApp() {
        FirebaseApp firebaseApp;
        try {
            //Trying to get instance of FirebaseApp with FirebaseOptions from google-services.json file
            firebaseApp = FirebaseApp.getInstance();
        } catch (Exception e) {
            //Trying to get instance of FirebaseApp with FirebaseOptions from resources
            firebaseApp = FirebaseApp.initializeApp(getContext(), loadFirebaseOptions(getContext()));
        }
        return firebaseApp;
    }

    public FirebaseOptions loadFirebaseOptions(Context context) {
        FirebaseOptions firebaseOptions = FirebaseOptions.fromResource(context);
        if (firebaseOptions == null) {
            String projectId = ResourceLoader.loadStringResourceByName(context, "ib_project_id");
            String apiKey = ResourceLoader.loadStringResourceByName(context, "ib_google_api_key");
            String applicationId = ResourceLoader.loadStringResourceByName(context, "ib_google_app_id");
            if (StringUtils.isNotBlank(projectId) && StringUtils.isNotBlank(apiKey) && StringUtils.isNotBlank(applicationId)) {
                firebaseOptions = new FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(applicationId)
                        .setProjectId(projectId)
                        .build();
            }
        }

        if (firebaseOptions == null) {
            throw new IllegalArgumentException("FirebaseOptions aren't provided");
        }
        return firebaseOptions;
    }
}
