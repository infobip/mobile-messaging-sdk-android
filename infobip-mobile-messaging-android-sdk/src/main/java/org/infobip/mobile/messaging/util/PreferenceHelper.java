package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
public abstract class PreferenceHelper {
    private static final Object LOCK = new Object();

    private PreferenceHelper() {
    }

    public static String findString(Context context, MobileMessagingProperty property) {
        return findString(context, property.getKey(), (String) property.getDefaultValue());
    }

    public static String findString(Context context, String key, String defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(key, defaultValue);
    }

    public static void saveString(Context context, MobileMessagingProperty property, String value) {
        saveString(context, property.getKey(), value);
    }

    public static void saveString(Context context, String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (null == value) {
            sharedPreferences.edit().remove(key).apply();
            return;
        }
        sharedPreferences.edit().putString(key, value).apply();
    }

    public static <T> Class<T> findClass(Context context, MobileMessagingProperty property) {
        return findClass(context, property.getKey(), (Class<T>) property.getDefaultValue());
    }

    public static <T> Class<T> findClass(Context context, String key, Class<T> defaultValue) {
        String callbackActivityClassName = findString(context, key, null);
        if (StringUtils.isBlank(callbackActivityClassName)) {
            return defaultValue;
        }
        //TODO cache
        try {
            return (Class<T>) Class.forName(callbackActivityClassName);
        } catch (ClassNotFoundException e) {
            //TODO log
            return null;
        }
    }

    public static void saveClass(Context context, MobileMessagingProperty property, Class<?> aClass) {
        saveClass(context, property.getKey(), aClass);
    }

    public static void saveClass(Context context, String key, Class<?> aClass) {
        String value = null != aClass.getName() ? aClass.getName() : null;
        saveString(context, key, value);
    }

    public static boolean findBoolean(Context context, MobileMessagingProperty property) {
        return findBoolean(context, property.getKey(), Boolean.TRUE.equals(property.getDefaultValue()));
    }

    public static boolean findBoolean(Context context, String key, boolean defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public static void saveBoolean(Context context, MobileMessagingProperty property, boolean value) {
        saveBoolean(context, property.getKey(), value);
    }

    public static void saveBoolean(Context context, String key, boolean value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public static int findInt(Context context, MobileMessagingProperty property) {
        Object defaultValue = property.getDefaultValue();
        int defaultInt = 0;
        if (null != defaultValue) {
            defaultInt = (int) defaultValue;
        }
        return findInt(context, property.getKey(), defaultInt);
    }

    public static int findInt(Context context, String key, int defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(key, defaultValue);
    }

    public static void saveInt(Context context, MobileMessagingProperty property, int value) {
        saveInt(context, property.getKey(), value);
    }

    public static void saveInt(Context context, String key, int value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public static long[] findLongArray(Context context, MobileMessagingProperty property) {
        return findLongArray(context, property.getKey(), (long[]) property.getDefaultValue());
    }

    public static long[] findLongArray(Context context, String key, long[] defaultValue) {
        String vibrate = findString(context, key, null);
        if (null == vibrate) {
            return defaultValue;
        }
        //TODO cache
        try {
            JSONArray jsonArray = new JSONArray(vibrate);
            long[] vibratePattern = new long[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                vibratePattern[i] = jsonArray.getLong(i);
            }
            return vibratePattern;
        } catch (JSONException e) {
            //TODO log
            return defaultValue;
        }
    }

    public static void saveLongArray(Context context, MobileMessagingProperty property, long[] value) {
        saveLongArray(context, property.getKey(), value);
    }

    public static void saveLongArray(Context context, String key, long[] value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (null == value) {
            sharedPreferences.edit().remove(key).apply();
            return;
        }
        JSONArray jsonArray = new JSONArray();
        for (long aValue : value) {
            jsonArray.put(aValue);
        }
        sharedPreferences.edit().putString(key, jsonArray.toString()).apply();
    }

    public static String[] findStringArray(Context context, MobileMessagingProperty property) {
        return findStringArray(context, property.getKey(), (String[]) property.getDefaultValue());
    }

    public static String[] findStringArray(Context context, String key, String[] defaultValue) {
        synchronized (LOCK) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> value = sharedPreferences.getStringSet(key, null);
            if (null == value) {
                return defaultValue;
            }
            return value.toArray(new String[value.size()]);
        }
    }

    public static void appendToStringArray(Context context, MobileMessagingProperty property, String... strings) {
        appendToStringArray(context, property.getKey(), strings);
    }

    public static void appendToStringArray(Context context, String key, final String... strings) {
        SetMutator mutator = new SetMutator() {
            @Override
            void mutate(Set<String> set) {
                set.addAll(Arrays.asList(strings));
            }
        };
        editSet(context, key, mutator);
    }

    public static void deleteFromStringArray(Context context, MobileMessagingProperty property, String... strings) {
        deleteFromStringArray(context, property.getKey(), strings);
    }

    public static void deleteFromStringArray(Context context, String key, final String... strings) {
        SetMutator mutator = new SetMutator() {
            @Override
            void mutate(Set<String> set) {
                set.removeAll(Arrays.asList(strings));
            }
        };
        editSet(context, key, mutator);
    }

    private static void editSet(Context context, String key, SetMutator mutator) {
        synchronized (LOCK) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            final Set<String> set = sharedPreferences.getStringSet(key, new HashSet<String>());
            mutator.mutate(set);
            if (set.isEmpty()) {
                sharedPreferences.edit().remove(key).apply();
                return;
            }
            sharedPreferences.edit().putStringSet(key, set).apply();
        }
    }

    public static void registerOnSharedPreferenceChangeListener(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(listener);
    }

    private static abstract class SetMutator {
        abstract void mutate(Set<String> set);
    }
}
