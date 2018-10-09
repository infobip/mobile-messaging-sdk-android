package org.infobip.mobile.messaging.storage;

import android.content.Context;
import org.infobip.mobile.messaging.Message;

import java.util.List;

/**
 * You can use the internal storage capabilities by implementing this interface.
 * If no message store class is configured, messages will not be stored!
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
 * @see SQLiteMessageStore
 * @since 29.03.2016.
 */
public interface MessageStore {

    /**
     * Finds all stored messages
     *
     * @param context current context
     * @return all stored messages
     */
    List<Message> findAll(Context context);

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
    void save(Context context, Message... messages);

    /**
     * Deletes all stored messages
     * <br>
     * This is convenience method.
     * Mobile Messaging SDK will never delete messages from the store by itself.
     * @param context current context
     */
    void deleteAll(Context context);
}
