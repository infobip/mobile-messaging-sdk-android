package org.infobip.mobile.messaging.storage;

import android.content.Context;
import org.infobip.mobile.messaging.Message;

import java.util.List;

/**
 * You can use the internal storage capabilities by implementing this interface.
 * If ne message store class is configured, messages will not me stored!
 * <pre>
 * {@code
 * public class MyActivity extends AppCompatActivity {
 *        protected void onCreate(Bundle savedInstanceState) {
 *            super.onCreate(savedInstanceState);
 *
 *            new MobileMessaging.Builder(this)
 *                .withMessageStore(MyMessageStore.class);
 *                .build();
 *        }
 *    }}
 * </pre>
 *
 * @author mstipanov
 * @see SharedPreferencesMessageStore
 * @since 29.03.2016.
 */
public interface MessageStore {

    List<Message> bind(final Context context);

    /**
     * Finds all stored messages
     *
     * @param context current context
     * @return all stored messages
     */
    List<Message> findAll(Context context);

    /**
     * Finds all stored messages that match specified ids
     *
     * @param context current context
     * @param messageIDs ids of messages to find
     * @return all stored messages
     */
    List<Message> findAllMatching(Context context, String... messageIDs);

    /**
     * Counts all stored messages
     *
     * @param context current context
     * @return all stored messages count
     */
    long countAll(Context context);

    /**
     * Saves the messages in the store
     *
     * @param context current context
     */
    void save(Context context, Message message);

    /**
     * Deletes all stored messages
     *
     * @param context current context
     */
    void deleteAll(Context context);
}
