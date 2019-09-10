package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
public abstract class PreferenceHelper {
    private static final String MM_PREFS_PREFIX = "org.infobip.mobile.messaging";
    private static final Object LOCK = new Object();
    private static Cryptor cryptor = null;
    private static Boolean usePrivateSharedPrefs = null;

    protected PreferenceHelper() {
    }

    private static Cryptor getCryptor(Context context) {
        if (cryptor != null) {
            return cryptor;
        }

        cryptor = new Cryptor(DeviceInformation.getDeviceID(context));
        return cryptor;
    }

    public static SharedPreferences getDefaultMMSharedPreferences(Context context) {
        if (usePrivateSharedPrefs == null) {
            usePrivateSharedPrefs = getPrivateMMSharedPreferences(context).getBoolean(MobileMessagingProperty.USE_PRIVATE_SHARED_PREFS.getKey(), false);
        }
        if (usePrivateSharedPrefs) {
            return getPrivateMMSharedPreferences(context);
        } else {
            return getPublicSharedPreferences(context);
        }
    }

    public static SharedPreferences getPrivateMMSharedPreferences(Context context) {
        return context.getSharedPreferences("MobileMessagingSDK", Context.MODE_PRIVATE);
    }

    public static SharedPreferences getPublicSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String findString(Context context, MobileMessagingProperty property) {
        return findString(context, property.getKey(), (String) property.getDefaultValue(), property.isEncrypted());
    }

    public static String findString(Context context, String key, String defaultValue) {
        return findString(context, key, defaultValue, false);
    }

    public static String findString(Context context, String key, String defaultValue, boolean encrypted) {
        SharedPreferences sharedPreferences = getDefaultMMSharedPreferences(context);
        if (!encrypted) {
            return sharedPreferences.getString(key, defaultValue);
        }

        String encryptedKey = getCryptor(context).encrypt(key);
        String encryptedValue = sharedPreferences.getString(encryptedKey, defaultValue);
        return getCryptor(context).decrypt(encryptedValue);
    }

    public static void saveString(Context context, MobileMessagingProperty property, String value) {
        saveString(context, property.getKey(), value, property.isEncrypted());
    }

    public static void saveString(Context context, String key, String value, boolean encrypted) {
        if (!encrypted) {
            saveString(context, key, value);
            return;
        }

        String encryptedKey = getCryptor(context).encrypt(key);
        String encryptedValue = getCryptor(context).encrypt(value);
        saveString(context, encryptedKey, encryptedValue);
    }

    public static void saveString(Context context, String key, String value) {
        SharedPreferences sharedPreferences = getDefaultMMSharedPreferences(context);
        if (null == value) {
            remove(context, key);
            return;
        }
        sharedPreferences.edit().putString(key, value).apply();
    }

    public static long findLong(Context context, MobileMessagingProperty property) {
        Object defaultValue = property.getDefaultValue();
        if (null == defaultValue) {
            defaultValue = 0L;
        }
        return findLong(context, property.getKey(), (Long) defaultValue);
    }

    public static long findLong(Context context, String key, long defaultValue) {
        SharedPreferences sharedPreferences = getDefaultMMSharedPreferences(context);
        String string = sharedPreferences.getString(key, String.valueOf(defaultValue));
        if (StringUtils.isBlank(string)) {
            return 0;
        }
        return Long.parseLong(string);
    }

    public static void saveLong(Context context, MobileMessagingProperty property, long value) {
        saveLong(context, property.getKey(), value);
    }

    public static void saveLong(Context context, String key, long value) {
        SharedPreferences sharedPreferences = getDefaultMMSharedPreferences(context);
        sharedPreferences.edit().putString(key, String.valueOf(value)).apply();
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
        String value = aClass.getName();
        saveString(context, key, value);
    }

    public static boolean findBoolean(Context context, MobileMessagingProperty property) {
        return findBoolean(context, property.getKey(), Boolean.TRUE.equals(property.getDefaultValue()));
    }

    public static boolean privatePrefsFindBoolean(Context context, MobileMessagingProperty property) {
        return findBoolean(context, property.getKey(), Boolean.TRUE.equals(property.getDefaultValue()), true);
    }

    public static boolean findBoolean(Context context, String key, boolean defaultValue) {
        return getDefaultMMSharedPreferences(context).getBoolean(key, defaultValue);
    }

    public static boolean findBoolean(Context context, String key, boolean defaultValue, boolean privatePrefs) {
        if (privatePrefs) {
            return getPrivateMMSharedPreferences(context).getBoolean(key, defaultValue);
        } else return getDefaultMMSharedPreferences(context).getBoolean(key, defaultValue);
    }

    public static void saveBoolean(Context context, MobileMessagingProperty property, boolean value) {
        saveBoolean(context, property.getKey(), value, false);
    }

    public static boolean isUsingPrivateSharedPrefs(Context context) {
        return PreferenceHelper.privatePrefsFindBoolean(context, MobileMessagingProperty.USE_PRIVATE_SHARED_PREFS);
    }

    public static boolean wasUsingPublicSharedPrefs(Context context) {
        return !PreferenceHelper.publicPrefsContains(context, MobileMessagingProperty.APPLICATION_CODE)
                && PreferenceHelper.publicPrefsContains(context, MobileMessagingProperty.SENDER_ID);
    }

    public static void saveUsePrivateSharedPrefs(Context context, boolean value) {
        usePrivateSharedPrefs = value;
        saveBoolean(context, MobileMessagingProperty.USE_PRIVATE_SHARED_PREFS.getKey(), value, true);
    }

    public static void saveBoolean(Context context, String key, boolean value) {
        saveBoolean(context, key, value, false);
    }

    public static void saveBoolean(Context context, String key, boolean value, boolean privatePrefs) {
        if (privatePrefs) {
            getPrivateMMSharedPreferences(context).edit().putBoolean(key, value).apply();
        } else {
            getDefaultMMSharedPreferences(context).edit().putBoolean(key, value).apply();
        }
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
        return getDefaultMMSharedPreferences(context).getInt(key, defaultValue);
    }

    public static void saveInt(Context context, MobileMessagingProperty property, int value) {
        saveInt(context, property.getKey(), value);
    }

    public static void saveInt(Context context, String key, int value) {
        getDefaultMMSharedPreferences(context).edit().putInt(key, value).apply();
    }

    public static String[] findAndRemoveStringArray(Context context, MobileMessagingProperty property) {
        final List<String> strings = new ArrayList<>();
        editSet(context, property.getKey(), new SetMutator() {
            @Override
            public void mutate(Set<String> set) {
                strings.addAll(set);
                set.clear();
            }
        });
        return strings.toArray(new String[strings.size()]);
    }

    public static String[] findStringArray(Context context, MobileMessagingProperty property) {
        return findStringArray(context, property.getKey(), (String[]) property.getDefaultValue());
    }

    public static String[] findStringArray(Context context, String key, String[] defaultValue) {
        return find(context, key, defaultValue, new SetConverter<String[]>() {
            @Override
            public String[] convert(Set<String> set) {
                return set.toArray(new String[set.size()]);
            }
        });
    }

    public static <T> T find(Context context, String key, T defaultValue, SetConverter<T> converter) {
        synchronized (LOCK) {
            Set<String> value = getDefaultMMSharedPreferences(context).getStringSet(key, null);
            if (null == value) {
                return defaultValue;
            }
            return converter.convert(value);
        }
    }

    public static void appendToStringArray(Context context, MobileMessagingProperty property, String... strings) {
        appendToStringArray(context, property.getKey(), strings);
    }

    public static void appendToStringArray(Context context, String key, final String... strings) {
        SetMutator mutator = new SetMutator() {
            @Override
            public void mutate(Set<String> set) {
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
            public void mutate(Set<String> set) {
                set.removeAll(Arrays.asList(strings));
            }
        };
        editSet(context, key, mutator);
    }

    public static void saveStringArray(Context context, MobileMessagingProperty property, String... strings) {
        saveStringArray(context, property.getKey(), strings);
    }

    public static void saveStringArray(Context context, String key, final String... strings) {
        SharedPreferences sharedPreferences = getDefaultMMSharedPreferences(context);
        final HashSet<String> stringSet = new HashSet<String>() {{
            addAll(Arrays.asList(strings));
        }};
        sharedPreferences.edit().putStringSet(key, stringSet).apply();
    }

    public static Set<String> findStringSet(Context context, MobileMessagingProperty property) {
        return findStringSet(context, property.getKey(), (Set<String>) property.getDefaultValue());
    }

    public static Set<String> findStringSet(Context context, String key, Set<String> defaultValue) {
        return find(context, key, defaultValue, new SetConverter<Set<String>>() {
            @Override
            public Set<String> convert(Set<String> set) {
                return set;
            }
        });
    }

    public static void saveStringSet(Context context, MobileMessagingProperty property, final Set<String> set) {
        saveStringSet(context, property.getKey(), set);
    }

    public static void saveStringSet(Context context, String key, final Set<String> set) {
        SetMutator mutator = new SetMutator() {
            @Override
            public void mutate(Set<String> innerSet) {
                innerSet.clear();
                innerSet.addAll(set);
            }
        };
        editSet(context, key, mutator);
    }

    public static void editSet(Context context, String key, SetMutator mutator) {
        synchronized (LOCK) {
            SharedPreferences sharedPreferences = getDefaultMMSharedPreferences(context);
            final Set<String> set = new HashSet<>(sharedPreferences.getStringSet(key, new HashSet<String>()));
            mutator.mutate(set);
            if (set.isEmpty()) {
                remove(context, key);
                return;
            }
            sharedPreferences.edit().putStringSet(key, set).apply();
        }
    }

    public static void remove(Context context, MobileMessagingProperty property) {
        String key = property.getKey();
        if (property.isEncrypted()) {
            key = getCryptor(context).encrypt(key);
        }
        remove(context, key);
    }

    public static void remove(Context context, String key) {
        getDefaultMMSharedPreferences(context)
                .edit()
                .remove(key)
                .apply();
    }

    public static boolean contains(Context context, MobileMessagingProperty property) {
        String key = property.getKey();
        if (property.isEncrypted()) {
            key = getCryptor(context).encrypt(key);
        }
        return contains(context, key);
    }

    public static boolean contains(Context context, String key) {
        return getDefaultMMSharedPreferences(context).contains(key);
    }

    public static boolean publicPrefsContains(Context context, MobileMessagingProperty property) {
        String key = property.getKey();
        if (property.isEncrypted()) {
            key = getCryptor(context).encrypt(key);
        }
        return getPublicSharedPreferences(context).contains(key);
    }

    public static void migrateToPrivatePrefs(Context context) {
        SharedPreferences.Editor publicPrefsEditor = PreferenceHelper.getPublicSharedPreferences(context).edit();
        SharedPreferences.Editor privatePrefsEditor = PreferenceHelper.getPrivateMMSharedPreferences(context).edit();
        Set<? extends Map.Entry<String, ?>> allPublicPrefEntries = PreferenceHelper.getPublicSharedPreferences(context).getAll().entrySet();
        for (Map.Entry<String, ?> pref : allPublicPrefEntries) {
            final String key = pref.getKey();
            if (key.startsWith(MM_PREFS_PREFIX)) {
                final Object value = pref.getValue();
                try {
                    if (value instanceof String) {
                        privatePrefsEditor.putString(key, (String) value);
                    } else if (value instanceof Integer) {
                        privatePrefsEditor.putInt(key, (Integer) value);
                    } else if (value instanceof Long) {
                        privatePrefsEditor.putLong(key, (Long) value);
                    } else if (value instanceof Boolean) {
                        privatePrefsEditor.putBoolean(key, (Boolean) value);
                    } else if (value instanceof Float) {
                        privatePrefsEditor.putFloat(key, (Float) value);
                    } else if (value instanceof Set) {
                        privatePrefsEditor.putStringSet(key, (Set<String>) value);
                    }
                } catch (Exception ignored) {
                    MobileMessagingLogger.w(String.format("Failed to migrate key %s with value %s", key, value));
                }
                publicPrefsEditor.remove(key);
            }
        }
        publicPrefsEditor.apply();
        privatePrefsEditor.apply();

        migrateCryptedEntriesFromPublicToPrivatePrefs(context,
                MobileMessagingProperty.INFOBIP_REGISTRATION_ID,
                MobileMessagingProperty.APPLICATION_CODE,
                MobileMessagingProperty.SENDER_ID,
                MobileMessagingProperty.CLOUD_TOKEN);
    }

    private static void migrateCryptedEntriesFromPublicToPrivatePrefs(Context context, MobileMessagingProperty... properties) {
        SharedPreferences.Editor editor = getPublicSharedPreferences(context).edit();
        for (MobileMessagingProperty property : properties) {
            String encryptedKey = getCryptor(context).encrypt(property.getKey());
            String encryptedValue = getPublicSharedPreferences(context).getString(encryptedKey, (String) property.getDefaultValue());
            saveString(context, encryptedKey, encryptedValue);
            // remove only app code as a required public property to keep backwards compatibility over push reg ID
            if (property == MobileMessagingProperty.APPLICATION_CODE) {
                editor.remove(encryptedKey).apply();
            }
        }
    }

    public static void registerOnSharedPreferenceChangeListener(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        getDefaultMMSharedPreferences(context).registerOnSharedPreferenceChangeListener(listener);
    }

    public static <Result> Result runTransaction(Transaction<Result> transaction) {
        synchronized (LOCK) {
            return transaction.run();
        }
    }

    public interface SetMutator {
        void mutate(Set<String> set);
    }

    public interface SetConverter<T> {
        T convert(Set<String> set);
    }

    public interface Transaction<Result> {
        Result run();
    }
}
