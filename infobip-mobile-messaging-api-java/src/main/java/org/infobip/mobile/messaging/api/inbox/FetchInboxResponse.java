/*
 * FetchInboxResponse.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.inbox;

import org.infobip.mobile.messaging.api.messages.MessageResponse;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FetchInboxResponse {

    private int countTotal;
    private int countUnread;
    private Integer countTotalFiltered;
    private Integer countUnreadFiltered;

    private List<MessageResponse> messages;
}
