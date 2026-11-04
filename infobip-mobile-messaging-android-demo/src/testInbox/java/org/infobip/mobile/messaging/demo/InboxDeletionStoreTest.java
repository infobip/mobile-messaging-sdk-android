/*
 * InboxDeletionStoreTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class InboxDeletionStoreTest {
    private final InboxDeletionStore store = new InboxDeletionStore();

    @Test
    public void recreatedScreenGetsTheSameDeletionState() {
        InboxDeletionStore.UserState firstScreen = store.forUser("app-hash", "user");
        firstScreen.recordDeleted(Collections.singleton("deleted"));

        InboxDeletionStore.UserState replacementScreen = store.forUser("app-hash", "user");
        assertSame(firstScreen, replacementScreen);
        assertEquals(Collections.singleton("deleted"), replacementScreen.snapshot());
    }

    @Test
    public void differentUsersAndApplicationsDoNotShareDeletedIds() {
        store.forUser("app-a", "user-a").recordDeleted(Collections.singleton("same-id"));

        assertTrue(store.forUser("app-a", "user-b").snapshot().isEmpty());
        assertTrue(store.forUser("app-b", "user-a").snapshot().isEmpty());
        assertEquals(Collections.singleton("same-id"), store.forUser("app-a", "user-a").snapshot());
    }

    @Test
    public void scopeKeysDoNotCollideWhenTheirPartsContainSeparators() {
        store.forUser("app:user", "other").recordDeleted(Collections.singleton("id"));
        assertTrue(store.forUser("app", "user:other").snapshot().isEmpty());
    }

    @Test
    public void onlyObserversOfTheOriginatingScopeAreNotified() {
        AtomicInteger originUpdates = new AtomicInteger();
        AtomicInteger otherUserUpdates = new AtomicInteger();
        AtomicInteger otherAppUpdates = new AtomicInteger();
        store.forUser("app-a", "user-a").addListener(originUpdates::incrementAndGet);
        store.forUser("app-a", "user-b").addListener(otherUserUpdates::incrementAndGet);
        store.forUser("app-b", "user-a").addListener(otherAppUpdates::incrementAndGet);

        store.forUser("app-a", "user-a").recordDeleted(Collections.singleton("id"));

        assertEquals(1, originUpdates.get());
        assertEquals(0, otherUserUpdates.get());
        assertEquals(0, otherAppUpdates.get());
    }

    @Test
    public void detachedScreensAreNotNotifiedButCanReconcileOnResume() {
        InboxDeletionStore.UserState state = store.forUser("app", "user");
        AtomicInteger updates = new AtomicInteger();
        Runnable screen = updates::incrementAndGet;
        state.addListener(screen);
        state.removeListener(screen);

        state.recordDeleted(Collections.singleton("id"));

        assertEquals(0, updates.get());
        assertEquals(Collections.singleton("id"), state.snapshot());
        state.addListener(screen);
        state.recordDeleted(Collections.singleton("another-id"));
        assertEquals(1, updates.get());
    }

    @Test
    public void restoringAnOlderSavedSnapshotDoesNotEraseLateDeletionSuccess() {
        InboxDeletionStore.UserState state = store.forUser("app", "user");
        state.recordDeleted(Collections.singleton("before-save"));
        Set<String> saved = state.snapshot();
        state.recordDeleted(Collections.singleton("after-save"));

        store.forUser("app", "user").recordDeleted(saved);

        assertEquals(new HashSet<>(Arrays.asList("before-save", "after-save")), state.snapshot());
    }

    @Test
    public void snapshotsAreDetachedFromFutureChanges() {
        InboxDeletionStore.UserState state = store.forUser("app", "user");
        Set<String> oldSnapshot = state.snapshot();
        Set<String> input = new HashSet<>(Collections.singleton("deleted"));
        state.recordDeleted(input);
        input.clear();

        assertTrue(oldSnapshot.isEmpty());
        assertEquals(Collections.singleton("deleted"), state.snapshot());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void callersCannotMutateRecordedDeletionsThroughSnapshot() {
        store.forUser("app", "user").snapshot().add("id");
    }

    @Test
    public void duplicateSuccessDoesNotTriggerRedundantUiUpdates() {
        InboxDeletionStore.UserState state = store.forUser("app", "user");
        AtomicInteger updates = new AtomicInteger();
        state.addListener(updates::incrementAndGet);
        state.recordDeleted(Collections.singleton("id"));
        state.recordDeleted(Collections.singleton("id"));

        assertEquals(1, updates.get());
    }
}
