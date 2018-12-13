package org.infobip.mobile.messaging.api.appinstance;

import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User data synchronization response.
 *
 * @author sslavin
 * @since 15/07/16.
 */
@Data
@NoArgsConstructor
public class UserBody {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    private String externalUserId;
    private String firstName;
    private String lastName;
    private String middleName;
    private String birthday;
    private String gender;

    private List<Object> emails;
    private List<Object> gsms;
    private Set<String> tags;
    private Map<String, Object> customAttributes;
    private List<AppInstanceWithPushRegId> instances;

    @Override
    public String toString() {
        return serializer.serialize(this);
    }
}
