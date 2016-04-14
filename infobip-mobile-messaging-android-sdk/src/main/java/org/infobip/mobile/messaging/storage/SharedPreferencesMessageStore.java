package org.infobip.mobile.messaging.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Base64;
import org.infobip.mobile.messaging.Message;

import java.util.*;

/**
 * Stores messages in shared preferences.
 * <pre>
 * {@code
 * public class MyActivity extends AppCompatActivity {
 *        protected void onCreate(Bundle savedInstanceState) {
 *            super.onCreate(savedInstanceState);
 *
 *            new MobileMessaging.Builder(this)
 *                .withMessageStore(SharedPreferencesMessageStore.class);
 *                .build();
 *        }
 *    }}
 * </pre>
 *
 * @author mstipanov
 * @since 29.03.2016.
 */
public class SharedPreferencesMessageStore implements MessageStore {

    private static final String INFOBIP_UNREPORTED_MESSAGE_IDS = "org.infobip.mobile.messaging.store.DATA";

    public void save(Context context, Message message) {
        addUnreportedMessageIds(context, message);
    }

    public List<Message> findAll(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> unreportedMessageIdSet = sharedPreferences.getStringSet(INFOBIP_UNREPORTED_MESSAGE_IDS, new LinkedHashSet<String>());
        ArrayList<Message> messages = new ArrayList<>();
        for (String s : unreportedMessageIdSet) {
            Message message = new Message(deserialize(s));
            messages.add(message);
        }
        Collections.sort(messages);
        return messages;
    }

    public void deleteAll(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().remove(INFOBIP_UNREPORTED_MESSAGE_IDS).apply();
    }

    public long countAll(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> unreportedMessageIdSet = sharedPreferences.getStringSet(INFOBIP_UNREPORTED_MESSAGE_IDS, new LinkedHashSet<String>());
        return unreportedMessageIdSet.size();
    }

    public List<Message> bind(final Context context) {
        return new AbstractList<Message>() {
            @Override
            public Message get(int location) {
                return findAll(context).get(location);
            }

            @Override
            public int size() {
                return findAll(context).size();
            }
        };
    }

    private void addUnreportedMessageIds(Context context, final Message message) {
        SetMutator mutator = new SetMutator() {
            @Override
            void mutate(Set<String> set) {
                set.add(serialize(message.getBundle()));
            }
        };
        editUnreportedMessageIds(context, mutator);
    }

    private synchronized void editUnreportedMessageIds(Context context, SetMutator mutator) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final Set<String> unreportedMessageIdSet = sharedPreferences.getStringSet(INFOBIP_UNREPORTED_MESSAGE_IDS, new LinkedHashSet<String>());
        mutator.mutate(unreportedMessageIdSet);
        if (unreportedMessageIdSet.isEmpty()) {
            sharedPreferences.edit().remove(INFOBIP_UNREPORTED_MESSAGE_IDS).apply();
            return;
        }
        sharedPreferences.edit().putStringSet(INFOBIP_UNREPORTED_MESSAGE_IDS, unreportedMessageIdSet).apply();
    }


    private String serialize(Bundle in) {
        Parcel parcel = Parcel.obtain();
        try {
            in.writeToParcel(parcel, 0);
            return Base64.encodeToString(parcel.marshall(), 0);
        } finally {
            parcel.recycle();
        }
    }

    private Bundle deserialize(String serialized) {
        if (serialized == null) {
            return null;
        }

        Parcel parcel = Parcel.obtain();
        try {
            byte[] data = Base64.decode(serialized, 0);
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            return parcel.readBundle();
        } finally {
            parcel.recycle();
        }
    }

    private abstract class SetMutator {
        abstract void mutate(Set<String> set);
    }
}
