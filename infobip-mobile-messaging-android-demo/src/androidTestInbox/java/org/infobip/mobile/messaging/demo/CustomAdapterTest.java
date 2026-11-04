/*
 * CustomAdapterTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomAdapterTest {
    @Test
    public void deletingSelectedMessageRemovesInvisibleSelection() {
        CustomAdapter adapter = new CustomAdapter(Collections.emptyList(), null);
        adapter.toggleSelection("deleted");
        adapter.removeSelection(Collections.singleton("deleted"));

        assertTrue(adapter.getSelectedMessageIds().isEmpty());
    }

    @Test
    public void deletionKeepsSelectionsMadeWhileRequestWasPending() {
        CustomAdapter adapter = new CustomAdapter(Collections.emptyList(), null);
        adapter.toggleSelection("deleted");
        adapter.toggleSelection("new-selection");
        adapter.removeSelection(Collections.singleton("deleted"));

        assertEquals(Collections.singleton("new-selection"), adapter.getSelectedMessageIds());
    }
}
