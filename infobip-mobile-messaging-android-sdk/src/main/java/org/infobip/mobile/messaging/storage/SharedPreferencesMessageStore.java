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

    private static final String INFOBIP_MESSAGE_DATA_SET = "org.infobip.mobile.messaging.store.DATA";
    private static final Object LOCK = new Object();

    public void save(Context context, Message message) {
        addMessage(context, message);
    }

    public List<Message> findAll(Context context) {
        return findAllMatching(context, new String[0]);
    }

    public List<Message> findAllMatching(Context context, String... messageIDs) {
        synchronized (LOCK) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> messageSet = sharedPreferences.getStringSet(INFOBIP_MESSAGE_DATA_SET, new LinkedHashSet<String>());
            List<String> listMessageIDs = Arrays.asList(messageIDs);
            ArrayList<Message> messages = new ArrayList<>();
            for (String s : messageSet) {
                Message message = new Message(deserialize(s));
                if (listMessageIDs.isEmpty() || listMessageIDs.contains(message.getMessageId())) {
                    messages.add(message);
                }
            }
            Collections.sort(messages);
            return messages;
        }
    }

    public void deleteAll(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().remove(INFOBIP_MESSAGE_DATA_SET).apply();
    }

    public long countAll(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> unreportedMessageIdSet = sharedPreferences.getStringSet(INFOBIP_MESSAGE_DATA_SET, new LinkedHashSet<String>());
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

    private void addMessage(Context context, final Message message) {
        SetMutator addMessageMutator = new SetMutator() {
            @Override
            void mutate(Set<String> set) {
                for (String serializedBundle : new HashSet<>(set)) {
                    Message messageInSet = new Message(deserialize(serializedBundle));
                    if (messageInSet.getMessageId().equals(message.getMessageId())) {
                        set.remove(serializedBundle);
                        break;
                    }
                }
                set.add(serialize(message.getBundle()));
            }
        };
        editMessages(context, addMessageMutator);
    }

    private synchronized void editMessages(Context context, SetMutator mutator) {
        synchronized (LOCK) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            final Set<String> messageSet = sharedPreferences.getStringSet(INFOBIP_MESSAGE_DATA_SET, new LinkedHashSet<String>());
            mutator.mutate(messageSet);
            if (messageSet.isEmpty()) {
                sharedPreferences.edit().remove(INFOBIP_MESSAGE_DATA_SET).apply();
                return;
            }
            sharedPreferences.edit().putStringSet(INFOBIP_MESSAGE_DATA_SET, messageSet).apply();
        }
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
