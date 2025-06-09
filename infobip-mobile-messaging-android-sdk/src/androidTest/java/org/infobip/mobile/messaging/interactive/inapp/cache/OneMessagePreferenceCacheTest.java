package org.infobip.mobile.messaging.interactive.inapp.cache;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author sslavin
 * @since 18/04/2018.
 */
public class OneMessagePreferenceCacheTest {

    private static final String MSG_KEY = "org.infobip.mobile.messaging.interactive.inapp.cache.MESSAGE";
    private static final String ENABLED_KEY = MobileMessagingProperty.SAVE_USER_DATA_ON_DISK.getKey();

    private OneMessagePreferenceCache oneMessageCache;

    private PreferenceHelperWrapper preferenceHelperWrapper = mock(PreferenceHelperWrapper.class);
    private JsonSerializer jsonSerializer = mock(JsonSerializer.class);

    @Before
    public void before() {
        oneMessageCache = new OneMessagePreferenceCache(preferenceHelperWrapper, jsonSerializer);
        oneMessageCache.getAndRemove(); // make sure nothing is stored in ram
        reset(preferenceHelperWrapper, jsonSerializer);
        when(preferenceHelperWrapper.get(eq(ENABLED_KEY), eq(true))).thenReturn(true);
    }

    @Test
    public void shouldSaveMessage() {
        Message message = message();
        String serializedMessage = "serializedMessage";
        when(jsonSerializer.serialize(eq(message))).thenReturn(serializedMessage);

        oneMessageCache.save(message);

        verify(preferenceHelperWrapper, times(1)).set(eq(MSG_KEY), eq(serializedMessage));
    }

    @Test
    public void shouldNotSaveMessageToPreferencesIfNotSavingUserData() {
        Message message = message();
        when(preferenceHelperWrapper.get(eq(ENABLED_KEY), eq(true))).thenReturn(false);

        oneMessageCache.save(message);

        verifyNoMoreInteractions(jsonSerializer);
        verify(preferenceHelperWrapper, never()).set(eq(MSG_KEY), anyString());
    }

    @Test
    public void shouldGetSavedMessageFromMemoryIfNotSavingUserData() {
        Message message = message();
        when(preferenceHelperWrapper.get(eq(ENABLED_KEY), eq(true))).thenReturn(false);

        oneMessageCache.save(message);
        Message ret = oneMessageCache.getAndRemove();

        verifyNoMoreInteractions(jsonSerializer);
        verify(preferenceHelperWrapper, never()).set(eq(MSG_KEY), anyString());
        verify(preferenceHelperWrapper, times(1)).remove(eq(MSG_KEY));
        assertEquals(message, ret);
    }

    @Test
    public void shouldRetrieveMessageFromMemory() {
        String serializedMessage = "serializedMessage";
        Message message = message();
        when(jsonSerializer.serialize(eq(message))).thenReturn(serializedMessage);

        oneMessageCache.save(message);
        Message ret = oneMessageCache.getAndRemove();

        assertEquals(message, ret);
        verify(jsonSerializer, times(1)).serialize(eq(message));
        verify(preferenceHelperWrapper, times(1)).set(eq(MSG_KEY), eq(serializedMessage));
        verify(preferenceHelperWrapper, times(1)).remove(eq(MSG_KEY));
        verify(preferenceHelperWrapper, never()).getAndRemove(eq(MSG_KEY));
    }

    @Test
    public void shouldRetrieveMessageFromPreferencesIfNotPresentInMemory() {
        String serializedMessage = "serializedMessage";
        Message message = message();
        when(preferenceHelperWrapper.getAndRemove(eq(MSG_KEY))).thenReturn(serializedMessage);
        when(jsonSerializer.deserialize(eq(serializedMessage), eq(Message.class))).thenReturn(message);

        Message ret = oneMessageCache.getAndRemove();

        assertEquals(message, ret);
        verify(preferenceHelperWrapper, times(1)).getAndRemove(eq(MSG_KEY));
        verify(preferenceHelperWrapper, never()).remove(eq(MSG_KEY));
        verify(jsonSerializer, times(1)).deserialize(eq(serializedMessage), eq(Message.class));
    }

    @Test
    public void shouldRemoveMessageFromMemoryIfIdsMatch() {
        Message message = message();
        when(preferenceHelperWrapper.get(eq(ENABLED_KEY), eq(true))).thenReturn(false);

        // store to memory
        oneMessageCache.save(message);

        oneMessageCache.remove(message);

        assertNull(oneMessageCache.getAndRemove());
    }

    @Test
    public void shouldRemoveMessageFromPreferencesIfIdsMatch() {
        Message message = message();
        String serializedMessage = "serializedMessage";
        when(preferenceHelperWrapper.getAndRemove(eq(MSG_KEY))).thenReturn(serializedMessage);
        when(jsonSerializer.deserialize(eq(serializedMessage), eq(Message.class))).thenReturn(message);

        oneMessageCache.remove(message);

        verify(preferenceHelperWrapper, times(1)).getAndRemove(eq(MSG_KEY));
    }

    @Test
    public void shouldKeepMessageIfIdsDoNotMatch() {
        Message newMessage = message();
        Message storedMessage = message();
        String serializedMessage = "serializedMessage";
        when(preferenceHelperWrapper.getAndRemove(eq(MSG_KEY))).thenReturn(serializedMessage);
        when(jsonSerializer.deserialize(eq(serializedMessage), eq(Message.class))).thenReturn(storedMessage);
        when(jsonSerializer.serialize(eq(storedMessage))).thenReturn(serializedMessage);

        oneMessageCache.remove(newMessage);

        verify(preferenceHelperWrapper, times(1)).getAndRemove(eq(MSG_KEY));
        verify(preferenceHelperWrapper, times(1)).set(eq(MSG_KEY), eq(serializedMessage));
    }

    private Message message() {
        return new Message();
    }
}
