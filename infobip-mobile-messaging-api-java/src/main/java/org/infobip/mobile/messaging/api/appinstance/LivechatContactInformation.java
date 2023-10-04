package org.infobip.mobile.messaging.api.appinstance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivechatContactInformation {

    private LivechatDestination[] liveChatDestinations;

}
