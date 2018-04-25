package org.infobip.mobile.messaging.interactive.inapp.view;

import android.content.DialogInterface;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sslavin
 * @since 25/04/2018.
 */
public class InAppViewDialogDismissListenerTest {

    private InAppViewDialogDismissListener inAppViewDialogDismissListener;

    private final InAppView inAppView = mock(InAppView.class);
    private final InAppView.Callback callback = mock(InAppView.Callback.class);

    @Before
    public void before() {
        reset(callback, inAppView);
        inAppViewDialogDismissListener = new InAppViewDialogDismissListener(inAppView, callback);
    }

    @Test
    public void shouldCallDismissMethodWhenDismissed() {
        DialogInterface dialogInterface = mock(DialogInterface.class);

        inAppViewDialogDismissListener.onDismiss(dialogInterface);

        verify(callback, times(1)).dismissed(eq(inAppView));
    }
}
