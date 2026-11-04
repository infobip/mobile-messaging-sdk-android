/*
 * InboxDeletionStore.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Process-local deletion suppression for the demo, shared across screen instances.
 * Stores application-code hashes, not application codes. Callers must detach screen listeners.
 * Successful deletions are retained across refreshes to tolerate eventually consistent fetches.
 */
final class InboxDeletionStore {
    static final InboxDeletionStore SHARED = new InboxDeletionStore();

    private final Map<Scope, UserState> users = new HashMap<>();

    synchronized UserState forUser(String applicationCodeHash, String externalUserId) {
        Scope scope = new Scope(applicationCodeHash, externalUserId);
        UserState state = users.get(scope);
        if (state == null) {
            state = new UserState();
            users.put(scope, state);
        }
        return state;
    }

    static final class UserState {
        private final Set<String> deletedIds = new HashSet<>();
        private final Set<Runnable> listeners = new HashSet<>();

        // Listeners run on the calling thread. The demo records SDK results on the main thread.
        void recordDeleted(Collection<String> messageIds) {
            List<Runnable> observers;
            synchronized (this) {
                if (!deletedIds.addAll(messageIds)) {
                    return;
                }
                observers = new ArrayList<>(listeners);
            }
            for (Runnable observer : observers) {
                observer.run();
            }
        }

        synchronized Set<String> snapshot() {
            return Collections.unmodifiableSet(new HashSet<>(deletedIds));
        }

        synchronized void addListener(Runnable listener) {
            listeners.add(listener);
        }

        synchronized void removeListener(Runnable listener) {
            listeners.remove(listener);
        }
    }

    private static final class Scope {
        private final String applicationCodeHash;
        private final String externalUserId;

        Scope(String applicationCodeHash, String externalUserId) {
            this.applicationCodeHash = Objects.requireNonNull(applicationCodeHash);
            this.externalUserId = Objects.requireNonNull(externalUserId);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Scope)) {
                return false;
            }
            Scope scope = (Scope) other;
            return applicationCodeHash.equals(scope.applicationCodeHash) && externalUserId.equals(scope.externalUserId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(applicationCodeHash, externalUserId);
        }
    }
}
