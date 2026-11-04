/*
 * InboxDeleteResultListenerTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class InboxDeleteResultListenerTest {
    private final InboxDeletionStore store = new InboxDeletionStore();

    @Test
    public void successAfterOldScreenIsGoneUpdatesTheReplacementScreen() {
        InboxDeletionStore.UserState origin = store.forUser("app", "user");
        AtomicInteger oldScreenUpdates = new AtomicInteger();
        Runnable oldScreen = oldScreenUpdates::incrementAndGet;
        origin.addListener(oldScreen);
        InboxDeleteResultListener callback = new InboxDeleteResultListener(
                origin, Collections.singleton("deleted"), null);
        origin.removeListener(oldScreen);

        InboxDeletionStore.UserState replacement = store.forUser("app", "user");
        List<String> visibleIds = new ArrayList<>(Arrays.asList("deleted", "remaining"));
        replacement.addListener(() -> visibleIds.removeIf(replacement.snapshot()::contains));

        callback.onResult(new Result<>((Void) null));

        assertEquals(0, oldScreenUpdates.get());
        assertEquals(Collections.singletonList("remaining"), visibleIds);
        assertEquals(Collections.singleton("deleted"), replacement.snapshot());
    }

    @Test
    public void successWithoutAnyScreenIsAvailableWhenInboxReopens() {
        InboxDeleteResultListener callback = new InboxDeleteResultListener(
                store.forUser("app", "user"), Arrays.asList("first", "second"), null);

        callback.onResult(new Result<>((Void) null));

        assertEquals(new HashSet<>(Arrays.asList("first", "second")), store.forUser("app", "user").snapshot());
    }

    @Test
    public void lateSuccessAfterUserOrApplicationChangeOnlyAffectsOriginalInbox() {
        InboxDeleteResultListener callback = new InboxDeleteResultListener(
                store.forUser("app-a", "user-a"), Collections.singleton("same-id"), null);
        InboxDeletionStore.UserState otherUser = store.forUser("app-a", "user-b");
        InboxDeletionStore.UserState otherApp = store.forUser("app-b", "user-a");
        AtomicInteger unrelatedUpdates = new AtomicInteger();
        otherUser.addListener(unrelatedUpdates::incrementAndGet);
        otherApp.addListener(unrelatedUpdates::incrementAndGet);

        callback.onResult(new Result<>((Void) null));

        assertEquals(Collections.singleton("same-id"), store.forUser("app-a", "user-a").snapshot());
        assertTrue(otherUser.snapshot().isEmpty());
        assertTrue(otherApp.snapshot().isEmpty());
        assertEquals(0, unrelatedUpdates.get());
    }

    @Test
    public void pendingDeletionCapturesIdsBeforeSelectionChanges() {
        InboxDeletionStore.UserState state = store.forUser("app", "user");
        Set<String> selected = new HashSet<>(Collections.singleton("deleted"));
        InboxDeleteResultListener callback = new InboxDeleteResultListener(state, selected, null);
        selected.clear();
        selected.add("new-selection");

        callback.onResult(new Result<>((Void) null));

        assertEquals(Collections.singleton("deleted"), state.snapshot());
    }

    @Test
    public void failureDoesNotHideMessagesAndReportsToAvailableScreen() {
        InboxDeletionStore.UserState state = store.forUser("app", "user");
        AtomicInteger failures = new AtomicInteger();
        InboxDeleteResultListener.FailureListener screen = failures::incrementAndGet;
        InboxDeleteResultListener callback = new InboxDeleteResultListener(state, Collections.singleton("id"), screen);

        callback.onResult(new Result<>(new MobileMessagingError("error", "Delete failed")));

        assertTrue(state.snapshot().isEmpty());
        assertEquals(1, failures.get());
    }

    @Test
    public void failureAfterScreenIsGoneDoesNotHideMessagesOrCrash() {
        InboxDeletionStore.UserState state = store.forUser("app", "user");
        InboxDeleteResultListener callback = new InboxDeleteResultListener(state, Collections.singleton("id"), null);

        callback.onResult(new Result<>(new MobileMessagingError("error", "Delete failed")));

        assertTrue(state.snapshot().isEmpty());
    }

    @Test
    public void deletingAllVisibleMessagesStillAllowsLoadingTheNextPage() {
        InboxDeletionStore.UserState state = store.forUser("app", "user");
        List<String> visibleIds = new ArrayList<>(Arrays.asList("first", "second"));
        state.addListener(() -> visibleIds.removeIf(state.snapshot()::contains));
        InboxRequestState pages = new InboxRequestState();
        pages.completePage(pages.beginPage(true), "next-cursor");
        InboxDeleteResultListener callback = new InboxDeleteResultListener(state, visibleIds, null);

        callback.onResult(new Result<>((Void) null));

        assertTrue(visibleIds.isEmpty());
        assertTrue(pages.shouldLoadNextPage(false, -1, visibleIds.size()));
        assertEquals("next-cursor", pages.beginPage(false).cursor);
    }
}
