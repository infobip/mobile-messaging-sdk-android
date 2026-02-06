/*
 * DebouncingGuard.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2026 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi;

import org.infobip.mobile.messaging.platform.SystemTimeProvider;
import org.infobip.mobile.messaging.platform.TimeProvider;

import java.util.HashMap;
import java.util.Map;

public class DebouncingGuard {

    public enum OperationType {
        userSessionReport,
        personalize,
        depersonalizeById,
        repersonalize,
        patch,
        fetch,
    }

    private final long debounceWindowMs;
    private final TimeProvider timeProvider;

    private final Map<OperationType, OperationSnapshot> lastOperations = new HashMap<>();

    /**
     * Snapshot of an operation at a point in time
     */
    private static class OperationSnapshot {
        final long timestamp;
        final int dataHashCode;

        OperationSnapshot(long timestamp, int dataHashCode) {
            this.timestamp = timestamp;
            this.dataHashCode = dataHashCode;
        }
    }

    public DebouncingGuard(long debounceWindowMs) {
        this(debounceWindowMs, new SystemTimeProvider());
    }

    public DebouncingGuard(long debounceWindowMs, TimeProvider timeProvider) {
        this.debounceWindowMs = debounceWindowMs;
        this.timeProvider = timeProvider;
    }

    /**
     * Check if operation should be allowed based on debouncing rules.
     *
     * @param operationType unique identifier for operation type
     * @param data          operation data to compare (can be null for operations without data)
     * @return true if operation should proceed
     */
    public synchronized boolean shouldAllow(OperationType operationType, Object data) {
        long now = timeProvider.now();
        int dataHash = computeDataHash(data);

        OperationSnapshot lastOp = lastOperations.get(operationType);

        if (lastOp == null) {
            lastOperations.put(operationType, new OperationSnapshot(now, dataHash));
            return true;
        }

        long timeSinceLastOp = now - lastOp.timestamp;

        if (timeSinceLastOp >= debounceWindowMs) {
            lastOperations.put(operationType, new OperationSnapshot(now, dataHash));
            return true;
        }

        if (lastOp.dataHashCode == dataHash) {
            return false;
        }

        lastOperations.put(operationType, new OperationSnapshot(now, dataHash));
        return true;
    }

    /**
     * Reset tracking for a specific operation type.
     */
    public synchronized void reset(OperationType operationType) {
        lastOperations.remove(operationType);
    }

    /**
     * Reset all operation tracking.
     */
    public synchronized void resetAll() {
        lastOperations.clear();
    }

    /**
     * Compute stable hash code for operation data.
     */
    private int computeDataHash(Object data) {
        if (data == null) {
            return 0;
        }

        // For Maps and similar objects
        if (data instanceof Map) {
            return deepHashMap((Map<?, ?>) data);
        }

        return data.hashCode();
    }

    /**
     * Deep hash computation for Maps
     */
    private int deepHashMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return 0;
        }

        int hash = 1;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            int keyHash = (key == null) ? 0 : key.hashCode();
            int valueHash;

            if (value instanceof Map) {
                valueHash = deepHashMap((Map<?, ?>) value);
            } else {
                valueHash = (value == null) ? 0 : value.hashCode();
            }

            hash = 31 * hash + (keyHash ^ valueHash);
        }

        return hash;
    }
}
