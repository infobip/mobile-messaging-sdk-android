/*
 * UserSessionEventBody.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.appinstance;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionEventBody {

    AppInstance systemData; //no custom atts!
    Set<String> sessionStarts; //session start dates in yyyy-MM-dd'T'HH:mm:ssZ format
    Map<String, String> sessionBounds; //map of session start/end dates in yyyy-MM-dd'T'HH:mm:ssZ format
}
