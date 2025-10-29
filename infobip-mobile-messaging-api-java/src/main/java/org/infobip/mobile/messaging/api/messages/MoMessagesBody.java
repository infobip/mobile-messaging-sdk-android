/*
 * MoMessagesBody.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sslavin
 * @since 21/07/16.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoMessagesBody {
    String from;
    MoMessage[] messages;
}
