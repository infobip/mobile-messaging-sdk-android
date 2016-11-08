package org.infobip.mobile.messaging.api.data;

import java.util.Map;

import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class UserDataReport {
    Map<String, Object> predefinedUserData;
    Map<String, CustomUserDataValueReport> customUserData;
}
