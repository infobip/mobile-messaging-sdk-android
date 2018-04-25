package org.infobip.mobile.messaging.interactive.inapp.cache;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

/**
 * @author sslavin
 * @since 16/04/2018.
 */
public class OneMessagePreferenceCache implements OneMessageCache {

    private static final String MESSAGE_KEY = "org.infobip.mobile.messaging.interactive.inapp.cache.MESSAGE";

    private static volatile Message runtimeCachedMessage = null;

    private final PreferenceHelperWrapper preferenceHelperWrapper;
    private final JsonSerializer jsonSerializer;

    public OneMessagePreferenceCache(Context context) {
        this.preferenceHelperWrapper = new PreferenceHelperWrapper(context);
        this.jsonSerializer = new JsonSerializer(false);
    }

    @VisibleForTesting
    OneMessagePreferenceCache(PreferenceHelperWrapper preferenceHelperWrapper, JsonSerializer jsonSerializer) {
        this.preferenceHelperWrapper = preferenceHelperWrapper;
        this.jsonSerializer = jsonSerializer;
    }

    @Override
    public void save(Message message) {
        runtimeCachedMessage = message;
        if (canStoreOnDisk()) {
            preferenceHelperWrapper.set(MESSAGE_KEY, jsonSerializer.serialize(message));
        }
    }

    @Override
    public void remove(Message message) {
        Message storedMessage = getAndRemove();
        if (storedMessage == null) {
            return;
        }

        if (!storedMessage.getMessageId().equals(message.getMessageId())) {
            save(storedMessage);
        }
    }

    @Override
    public Message getAndRemove() {
        Message message = runtimeCachedMessage;
        if (message != null) {
            runtimeCachedMessage = null;
            preferenceHelperWrapper.remove(MESSAGE_KEY);
            return message;
        }

        return getFromPreferences();
    }

    @Nullable
    private Message getFromPreferences() {
        String value = preferenceHelperWrapper.getAndRemove(MESSAGE_KEY);
        if (TextUtils.isEmpty(value)) {
            return null;
        }

        return jsonSerializer.deserialize(value, Message.class);
    }

    private boolean canStoreOnDisk() {
        return preferenceHelperWrapper.get(MobileMessagingProperty.SAVE_USER_DATA_ON_DISK.getKey(), true);
    }
}
