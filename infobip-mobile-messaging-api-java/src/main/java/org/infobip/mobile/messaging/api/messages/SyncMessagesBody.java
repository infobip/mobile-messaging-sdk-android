package org.infobip.mobile.messaging.api.messages;

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

    static public SyncMessagesBody makeNullableBody(String[] mIDs, String[] drIDs) {
        if (mIDs.length == 0 && drIDs.length == 0) {
            return null;
        }
        SyncMessagesBody result = new SyncMessagesBody();
        result.mIDs = mIDs.length > 0 ? mIDs : null;
        result.drIDs = drIDs.length > 0 ? drIDs : null;
        return result;
    }
}
