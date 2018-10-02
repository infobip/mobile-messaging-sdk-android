package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author sslavin
 * @since 05/10/2017.
 */

public class ModuleLoader {

    private final Context context;

    public ModuleLoader(Context context) {
        this.context = context;
    }

    public <T> Map<String, T> loadModulesFromManifest(Class<T> cls) {
        Map<String, T> modules = new HashMap<>();
        for (Class<T> implementation : loadModuleClassesFromManifest(cls)) {
            T module;
            try {
                module = implementation.newInstance();
            } catch (Exception e) {
                MobileMessagingLogger.e("Cannot create module for class: " + cls.getName());
                continue;
            }
            modules.put(implementation.getName(), module);
        }
        return modules;
    }

    public <T> T createModule(Class<T> cls) {
        try {
            return cls.newInstance();
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot create module for class: " + cls.getName());
            return null;
        }
    }

    private <T> Set<Class<T>> loadModuleClassesFromManifest(Class<T> cls) {
        Bundle metaData;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData == null) {
                throw new RuntimeException("Meta data is null");
            }
            metaData = ai.metaData;
        } catch (Exception e) {
            MobileMessagingLogger.e("Failed to read meta data of application: " + e.getMessage());
            return new HashSet<>();
        }

        HashSet<Class<T>> classes = new HashSet<>();
        for (String key : metaData.keySet()) {
            try {
                Object o = metaData.get(key);
                if (!(o instanceof String)) {
                    continue;
                }

                String className = (String) o;
                if (!cls.getName().equals(className)) {
                    continue;
                }

                //noinspection unchecked
                classes.add((Class<T>) Class.forName(key));
            } catch (Exception ignored) {
                MobileMessagingLogger.e("Cannot create class for: " + key);
            }
        }
        return classes;
    }
}
