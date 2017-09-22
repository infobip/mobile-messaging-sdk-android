package org.infobip.mobile.messaging.api.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomUserDataValueReport {
    private Object value;
    private String type;
}
