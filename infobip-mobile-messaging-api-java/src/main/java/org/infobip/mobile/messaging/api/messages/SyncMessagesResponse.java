package org.infobip.mobile.messaging.api.messages;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author pandric on 09/09/16.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncMessagesResponse {
    List<MessageResponse> payloads;

    public List<MessageResponse> getPayloads() {
        return payloads;
    }
}
