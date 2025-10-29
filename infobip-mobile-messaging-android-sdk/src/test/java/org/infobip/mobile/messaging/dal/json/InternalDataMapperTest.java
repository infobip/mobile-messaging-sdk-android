/*
 * InternalDataMapperTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.dal.json;

import org.infobip.mobile.messaging.Message;
import org.junit.Test;

/**
 * @author sslavin
 * @since 05/07/2017.
 */

public class InternalDataMapperTest {

    @Test
    public void should_not_produce_npe_when_silent_but_vibrate_is_not_set() {

        // Given
        String givenInternalData = "{ \"silent\": { } }";
        Message givenMessage = new Message();

        // When
        InternalDataMapper.updateMessageWithInternalData(givenMessage, givenInternalData);
    }
}
