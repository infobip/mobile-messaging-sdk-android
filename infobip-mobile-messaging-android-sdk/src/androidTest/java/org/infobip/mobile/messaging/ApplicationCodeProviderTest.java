package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;


public class ApplicationCodeProviderTest extends MobileMessagingTestCase {

    private ApplicationCodeProvider appCodeProviderMock;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        appCodeProviderMock = Mockito.mock(ApplicationCodeProvider.class);
        Mockito.when(appCodeProviderMock.resolve()).thenReturn("app_code_from_memory");
        MobileMessagingCore.applicationCodeProvider = appCodeProviderMock;
    }

    @Test
    public void shouldGetApplicationCodeFromMemory() throws InterruptedException {
        // Given
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SAVE_APP_CODE_ON_DISK, false);
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "");
        MobileMessagingCore.applicationCode = null;

        // When
        String applicationCode = MobileMessagingCore.getApplicationCode(context);

        // Then
        Mockito.verify(appCodeProviderMock, Mockito.after(1000).times(1)).resolve();
        assertEquals("app_code_from_memory", applicationCode);
    }

    @Test
    public void shouldGetApplicationCodeFromDisk() throws Exception {
        // Given
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SAVE_APP_CODE_ON_DISK, true);

        // When
        String applicationCode = MobileMessagingCore.getApplicationCode(context);

        // Then
        Mockito.verify(appCodeProviderMock, Mockito.after(1000).never()).resolve();
        assertFalse("app_code_from_memory".equals(applicationCode));
        assertEquals(applicationCode, "TestApplicationCode");
    }

}
