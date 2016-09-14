package org.infobip.mobile.messaging.api.messages.v3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author pandric on 09/09/16.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncMessagesBody {
    String[] mIDs;
    String[] drIDs;
}
