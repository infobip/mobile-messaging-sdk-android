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
