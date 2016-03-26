package org.infobip.mobile.messaging.api.registration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {
    private String deviceApplicationInstanceId;
}
