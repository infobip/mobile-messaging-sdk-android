package org.infobip.mobile.messaging.dal.bundle;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.notification.InteractiveCategory;


public class InteractiveCategoryBundleMapper extends BundleMapper {

    private static final String BUNDLED_CATEGORY_TAG = InteractiveCategoryBundleMapper.class.getName() + ".category";

    /**
     * De-serializes interactive category object from bundle
     *
     * @param bundle where to load data from
     * @return new interactive category object
     */
    public static
    @Nullable
    InteractiveCategory interactiveCategoryFromBundle(@NonNull Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_CATEGORY_TAG, InteractiveCategory.class);
    }

    /**
     * Serializes interactive category object into bundle
     *
     * @param interactiveCategory object to serialize
     * @return bundle with interactive category
     */
    public static
    @NonNull
    Bundle interactiveCategoryToBundle(@NonNull InteractiveCategory interactiveCategory) {
        return objectToBundle(interactiveCategory, BUNDLED_CATEGORY_TAG);
    }
}
