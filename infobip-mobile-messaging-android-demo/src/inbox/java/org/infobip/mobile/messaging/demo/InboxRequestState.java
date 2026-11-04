/*
 * InboxRequestState.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

/** Main-thread request bookkeeping, independent of the Activity lifecycle and views. */
final class InboxRequestState {
    static final class Request {
        final int generation;
        final long id;
        final boolean startOver;
        final String cursor;

        private Request(int generation, long id, boolean startOver, String cursor) {
            this.generation = generation;
            this.id = id;
            this.startOver = startOver;
            this.cursor = cursor;
        }
    }

    private int generation;
    private long requestId;
    private long latestCounterRequestId;
    private Request activePage;
    private String nextCursor;

    Request beginPage(boolean startOver) {
        if (!startOver && (activePage != null || nextCursor == null)) {
            return null;
        }
        if (startOver) {
            generation++;
        }
        // Keep the committed cursor until a refresh succeeds, so failure is recoverable.
        activePage = new Request(generation, ++requestId, startOver, startOver ? null : nextCursor);
        latestCounterRequestId = activePage.id;
        return activePage;
    }

    boolean completePage(Request request, String cursor) {
        if (!failPage(request)) {
            return false;
        }
        nextCursor = cursor;
        return true;
    }

    boolean failPage(Request request) {
        if (activePage != request || request.generation != generation) {
            return false;
        }
        activePage = null;
        return true;
    }

    Request beginCounters() {
        Request request = new Request(generation, ++requestId, false, null);
        latestCounterRequestId = request.id;
        return request;
    }

    boolean acceptsCounters(Request request) {
        return request.generation == generation && request.id == latestCounterRequestId;
    }

    void reset() {
        generation++;
        activePage = null;
        nextCursor = null;
    }

    boolean canLoadNextPage() {
        return activePage == null && nextCursor != null;
    }

    boolean shouldLoadNextPage(boolean canScrollDown, int lastVisiblePosition, int messageCount) {
        return canLoadNextPage() && (!canScrollDown || lastVisiblePosition >= messageCount - 3);
    }
}
