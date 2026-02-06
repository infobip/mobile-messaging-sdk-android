/*
 * DebouncingGuardTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi;

import static org.infobip.mobile.messaging.mobileapi.DebouncingGuard.OperationType.depersonalizeById;
import static org.infobip.mobile.messaging.mobileapi.DebouncingGuard.OperationType.personalize;
import static org.infobip.mobile.messaging.mobileapi.DebouncingGuard.OperationType.repersonalize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.infobip.mobile.messaging.platform.TimeProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DebouncingGuardTest {

    private static class MockTimeProvider implements TimeProvider {
        private long currentTime = 0;

        void setTime(long time) {
            this.currentTime = time;
        }

        @Override
        public long now() {
            return currentTime;
        }
    }

    private MockTimeProvider timeProvider;
    private DebouncingGuard guard;

    private final long DEBOUNCE_WINDOW_MS = 1000;

    @Before
    public void setUp() {
        timeProvider = new MockTimeProvider();
        guard = new DebouncingGuard(DEBOUNCE_WINDOW_MS, timeProvider); // 1 second window
    }

    @Test
    public void test_allows_first_call() {
        assertTrue(guard.shouldAllow(personalize, "data1"));
    }

    @Test
    public void test_blocks_duplicate_within_window() {
        guard.shouldAllow(personalize, "data1");
        assertFalse(guard.shouldAllow(personalize, "data1"));
    }

    @Test
    public void test_allows_different_data_within_window() {
        guard.shouldAllow(personalize, "data1");
        assertTrue(guard.shouldAllow(personalize, "data2"));
    }

    @Test
    public void test_allows_same_data_after_window() {
        timeProvider.setTime(0);
        guard.shouldAllow(personalize, "data1");

        timeProvider.setTime(DEBOUNCE_WINDOW_MS + 100);
        assertTrue(guard.shouldAllow(personalize, "data1"));
    }

    @Test
    public void test_independent_operation_types() {
        guard.shouldAllow(personalize, "data");
        assertTrue(guard.shouldAllow(depersonalizeById, "data"));
    }

    @Test
    public void test_handles_null_data() {
        assertTrue(guard.shouldAllow(personalize, null));

        assertFalse(guard.shouldAllow(personalize, null));
    }

    @Test
    public void test_handles_empty_data() {
        assertTrue(guard.shouldAllow(personalize, ""));
        assertFalse(guard.shouldAllow(personalize, ""));
    }

    @Test
    public void test_deep_HashMap_with_nested_maps() {
        Map<String, Object> map1 = new HashMap<>();
        Map<String, Object> nested1 = new HashMap<>();
        nested1.put("key", "value");
        map1.put("nested", nested1);

        Map<String, Object> map2 = new HashMap<>();
        Map<String, Object> nested2 = new HashMap<>();
        nested2.put("key", "value");
        map2.put("nested", nested2);

        guard.shouldAllow(personalize, map1);
        assertFalse(guard.shouldAllow(personalize, map2));
    }

    @Test
    public void test_different_nested_maps() {
        Map<String, Object> map1 = new HashMap<>();
        Map<String, Object> nested1 = new HashMap<>();
        nested1.put("key", "value1");
        map1.put("nested", nested1);

        Map<String, Object> map2 = new HashMap<>();
        Map<String, Object> nested2 = new HashMap<>();
        nested2.put("key", "value2");
        map2.put("nested", nested2);

        guard.shouldAllow(personalize, map1);
        assertTrue(guard.shouldAllow(personalize, map2));
    }

    @Test
    public void test_emptyMapVsNullMap() {
        Map<String, Object> emptyMap = new HashMap<>();

        guard.shouldAllow(personalize, emptyMap);
        assertFalse(guard.shouldAllow(personalize, new HashMap<>()));
    }

    @Test
    public void test_sequential_updates_with_different_data() {
        timeProvider.setTime(0);
        guard.shouldAllow(personalize, "data1");

        timeProvider.setTime(DEBOUNCE_WINDOW_MS / 2); // Within window
        guard.shouldAllow(personalize, "data2"); // Different data - allowed

        timeProvider.setTime(DEBOUNCE_WINDOW_MS / 2 + DEBOUNCE_WINDOW_MS / 10); // Still within 1 second of last call
        assertFalse(guard.shouldAllow(personalize, "data2"));
    }

    @Test
    public void test_multiple_operation_types_simultaneously() {
        timeProvider.setTime(0);

        assertTrue(guard.shouldAllow(personalize, "user1"));
        assertTrue(guard.shouldAllow(depersonalizeById, "user1"));
        assertTrue(guard.shouldAllow(repersonalize, "user1"));

        // All should block duplicates independently
        assertFalse(guard.shouldAllow(personalize, "user1"));
        assertFalse(guard.shouldAllow(depersonalizeById, "user1"));
        assertFalse(guard.shouldAllow(repersonalize, "user1"));
    }
}
