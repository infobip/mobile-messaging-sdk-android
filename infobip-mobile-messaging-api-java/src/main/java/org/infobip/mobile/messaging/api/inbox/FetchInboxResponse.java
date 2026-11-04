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

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FetchInboxResponse {

    private int countTotal;
    private int countUnread;
    private Integer countTotalFiltered;
    private Integer countUnreadFiltered;
    private List<MessageResponse> messages;
    private String nextCursor;

    public FetchInboxResponse(int countTotal, int countUnread, Integer countTotalFiltered,
                              Integer countUnreadFiltered, List<MessageResponse> messages) {
        this(countTotal, countUnread, countTotalFiltered, countUnreadFiltered, messages, null);
    }

    public FetchInboxResponse(int countTotal, int countUnread, Integer countTotalFiltered,
                              Integer countUnreadFiltered, List<MessageResponse> messages, String nextCursor) {
        this.countTotal = countTotal;
        this.countUnread = countUnread;
        this.countTotalFiltered = countTotalFiltered;
        this.countUnreadFiltered = countUnreadFiltered;
        this.messages = messages;
        this.nextCursor = nextCursor;
    }
}
