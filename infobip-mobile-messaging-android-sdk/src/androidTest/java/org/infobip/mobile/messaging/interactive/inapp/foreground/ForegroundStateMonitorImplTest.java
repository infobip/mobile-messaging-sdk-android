/*
 * ForegroundStateMonitorImplTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.foreground;

import android.app.Activity;
import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author sslavin
 * @since 07/05/2018.
 */
public class ForegroundStateMonitorImplTest {

    private ForegroundStateMonitorImpl foregroundStateMonitor;

    private Context context = mock(Context.class);
    private MobileMessagingCore mobileMessagingCore = mock(MobileMessagingCore.class);
    private ActivityLifecycleMonitor activityLifecycleMonitor = mock(ActivityLifecycleMonitor.class);
    private Activity activity = mock(Activity.class);

    @Before
    public void before() {
        reset(context, mobileMessagingCore, activityLifecycleMonitor);
        foregroundStateMonitor = new ForegroundStateMonitorImpl(context);
    }

    @Test
    public void shouldReturnBackgroundIfActivityLifecycleMonitorIsNull() {
        when(mobileMessagingCore.getActivityLifecycleMonitor()).thenReturn(null);

        ForegroundState foregroundState = foregroundStateMonitor.isInForeground(mobileMessagingCore);

        assertFalse(foregroundState.isForeground());
        assertNull(foregroundState.getForegroundActivity());
    }

    @Test
    public void shouldReturnBackgroundIfActivityLifecycleMonitorReturnsNoActivity() {
        when(mobileMessagingCore.getActivityLifecycleMonitor()).thenReturn(activityLifecycleMonitor);
        when(activityLifecycleMonitor.getForegroundActivity()).thenReturn(null);

        ForegroundState foregroundState = foregroundStateMonitor.isInForeground(mobileMessagingCore);

        assertFalse(foregroundState.isForeground());
        assertNull(foregroundState.getForegroundActivity());
    }

    @Test
    public void shouldReturnForegroundIfActivityLifecycleMonitorReturnsActivity() {
        when(mobileMessagingCore.getActivityLifecycleMonitor()).thenReturn(activityLifecycleMonitor);
        when(activityLifecycleMonitor.getForegroundActivity()).thenReturn(activity);

        ForegroundState foregroundState = foregroundStateMonitor.isInForeground(mobileMessagingCore);

        assertTrue(foregroundState.isForeground());
        assertEquals(activity, foregroundState.getForegroundActivity());
    }
}
