/*
 * MobileInboxFilterOptionsTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MobileInboxFilterOptionsTest {
    @Test
    public void singularTopicSupportsServerSidePagination() {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                null, null, "Promotions", 20, "cursor");

        assertEquals("Promotions", options.getTopic());
        assertNull(options.getTopics());
        assertEquals("cursor", options.getCursor());
    }

    @Test
    public void nonblankSingletonKeepsClientSideFilteringForEveryLimit() {
        for (Integer limit : Arrays.asList(null, 0, -1, 20)) {
            MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                    null, null, Collections.singletonList("Offers"), limit);

            assertNull(options.getTopic());
            assertEquals(Collections.singletonList("Offers"), options.getTopics());
            assertEquals(limit, options.getLimit());
        }
    }

    @Test
    public void settingSingletonClearsPreviousServerSideTopic() {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(null, null, "Promotions", 20);
        options.setTopics(Collections.singletonList("Offers"));

        assertNull(options.getTopic());
        assertEquals(Collections.singletonList("Offers"), options.getTopics());
    }

    @Test
    public void settingSingularTopicClearsClientSideFiltering() {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(
                null, null, Collections.singletonList("Offers"), 20);
        options.setTopic("Promotions");

        assertEquals("Promotions", options.getTopic());
        assertNull(options.getTopics());
    }

    @Test
    public void blankSingletonsKeepClientSideFiltering() {
        for (String topic : Arrays.asList(null, "", " ", "\t\n")) {
            List<String> topics = Collections.singletonList(topic);
            MobileInboxFilterOptions options = new MobileInboxFilterOptions(null, null, topics, 20);
            assertNull(options.getTopic());
            assertEquals(topics, options.getTopics());
        }
    }

    @Test
    public void settingBlankSingletonClearsPreviousServerSideTopic() {
        MobileInboxFilterOptions options = new MobileInboxFilterOptions(null, null, "Promotions", 20);
        options.setTopics(Collections.singletonList(null));

        assertNull(options.getTopic());
        assertEquals(Collections.singletonList(null), options.getTopics());
    }

    @Test
    public void emptyAndMultipleTopicListsRetainTheirExistingRepresentation() {
        for (List<String> topics : Arrays.asList(Collections.<String>emptyList(), Arrays.asList("one", "two"))) {
            MobileInboxFilterOptions options = new MobileInboxFilterOptions(null, null, topics, 20);
            assertNull(options.getTopic());
            assertEquals(topics, options.getTopics());
        }
    }
}
