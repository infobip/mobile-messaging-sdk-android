/*
 * InboxRequestStateTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import org.junit.Test;

import static org.junit.Assert.*;

public class InboxRequestStateTest {
    @Test
    public void refreshSupersedesAnInFlightPage() {
        InboxRequestState state = new InboxRequestState();
        InboxRequestState.Request oldPage = state.beginPage(true);
        InboxRequestState.Request replacement = state.beginPage(true);

        assertFalse(state.completePage(oldPage, "old-cursor"));
        assertFalse(state.failPage(oldPage));
        assertNull(state.beginPage(false));
        assertTrue(state.completePage(replacement, "new-cursor"));
        assertEquals("new-cursor", state.beginPage(false).cursor);
    }

    @Test
    public void changingFiltersDiscardsOldCursorAndResponses() {
        InboxRequestState state = new InboxRequestState();
        state.completePage(state.beginPage(true), "promotions-cursor");
        InboxRequestState.Request oldPage = state.beginPage(false);
        InboxRequestState.Request oldCounters = state.beginCounters();

        state.reset();
        InboxRequestState.Request notifications = state.beginPage(true);
        assertNull(notifications.cursor);
        assertFalse(state.completePage(oldPage, "old-next"));
        assertFalse(state.acceptsCounters(oldCounters));
        state.failPage(notifications);
        assertFalse(state.canLoadNextPage());
    }

    @Test
    public void failedRefreshPreservesCommittedCursor() {
        InboxRequestState state = new InboxRequestState();
        state.completePage(state.beginPage(true), "page-2");
        InboxRequestState.Request refresh = state.beginPage(true);

        assertNull(refresh.cursor);
        assertTrue(state.failPage(refresh));
        assertEquals("page-2", state.beginPage(false).cursor);
    }

    @Test
    public void successfulRefreshReplacesCommittedCursor() {
        InboxRequestState state = new InboxRequestState();
        state.completePage(state.beginPage(true), "old-page-2");
        state.completePage(state.beginPage(true), "new-page-2");

        assertEquals("new-page-2", state.beginPage(false).cursor);
    }

    @Test
    public void emptyOrShortListLoadsAnotherPageWithoutScrolling() {
        InboxRequestState state = new InboxRequestState();
        state.completePage(state.beginPage(true), "page-2");

        assertTrue(state.shouldLoadNextPage(false, -1, 0));
        assertTrue(state.shouldLoadNextPage(false, 1, 2));
        assertFalse(state.shouldLoadNextPage(true, 3, 20));
        assertTrue(state.shouldLoadNextPage(true, 18, 20));
        InboxRequestState.Request next = state.beginPage(false);
        assertFalse(state.shouldLoadNextPage(false, -1, 0));
        state.completePage(next, null);
        assertFalse(state.shouldLoadNextPage(false, -1, 0));
    }

    @Test
    public void olderCountersCannotOverwriteNewerRequests() {
        InboxRequestState state = new InboxRequestState();
        InboxRequestState.Request first = state.beginCounters();
        InboxRequestState.Request second = state.beginCounters();
        assertFalse(state.acceptsCounters(first));
        assertTrue(state.acceptsCounters(second));

        InboxRequestState.Request page = state.beginPage(true);
        assertFalse(state.acceptsCounters(second));
        assertTrue(state.acceptsCounters(page));
        InboxRequestState.Request counters = state.beginCounters();
        assertFalse(state.acceptsCounters(page));
        assertTrue(state.acceptsCounters(counters));
    }

    @Test
    public void counterRequestsDoNotChangePagination() {
        InboxRequestState state = new InboxRequestState();
        state.completePage(state.beginPage(true), "page-2");
        assertTrue(state.acceptsCounters(state.beginCounters()));
        assertEquals("page-2", state.beginPage(false).cursor);
    }

    @Test
    public void reopenedScreenStartsAtFirstPage() {
        InboxRequestState previousScreen = new InboxRequestState();
        previousScreen.completePage(previousScreen.beginPage(true), "page-2");
        previousScreen.completePage(previousScreen.beginPage(false), "page-3");

        InboxRequestState reopenedScreen = new InboxRequestState();
        assertFalse(reopenedScreen.canLoadNextPage());
        assertNull(reopenedScreen.beginPage(true).cursor);
    }

    @Test
    public void destroyingScreenInvalidatesEveryPendingResponse() {
        InboxRequestState state = new InboxRequestState();
        InboxRequestState.Request page = state.beginPage(true);
        InboxRequestState.Request counters = state.beginCounters();
        state.reset();

        assertFalse(state.completePage(page, "cursor"));
        assertFalse(state.failPage(page));
        assertFalse(state.acceptsCounters(counters));
    }
}
