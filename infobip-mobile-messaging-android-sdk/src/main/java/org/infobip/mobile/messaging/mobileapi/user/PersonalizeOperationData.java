/*
 * PersonalizeOperationData.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2026 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.user;

import java.util.Map;
import java.util.Objects;

class PersonalizeOperationData {
    private final Map<String, Object> userIdentity;
    private final Map<String, Object> userAttributes;
    private final Boolean forceDepersonalize;
    private final Boolean keepAsLead;

    PersonalizeOperationData(
            Map<String, Object> userIdentity,
            Map<String, Object> userAttributes,
            Boolean forceDepersonalize,
            Boolean keepAsLead) {
        this.userIdentity = userIdentity;
        this.userAttributes = userAttributes;
        this.forceDepersonalize = forceDepersonalize;
        this.keepAsLead = keepAsLead;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                deepHashMap(userIdentity),
                deepHashMap(userAttributes),
                forceDepersonalize,
                keepAsLead
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PersonalizeOperationData)) return false;

        PersonalizeOperationData other = (PersonalizeOperationData) obj;
        return Objects.equals(forceDepersonalize, other.forceDepersonalize) &&
                Objects.equals(keepAsLead, other.keepAsLead) &&
                deepEquals(userIdentity, other.userIdentity) &&
                deepEquals(userAttributes, other.userAttributes);
    }

    private static int deepHashMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return 0;

        int hash = 1;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            int keyHash = key.hashCode();
            int valueHash = (value == null) ? 0 : value.hashCode();

            hash = 31 * hash + (keyHash ^ valueHash);
        }
        return hash;
    }

    private static boolean deepEquals(Map<String, Object> m1, Map<String, Object> m2) {
        if (m1 == m2) return true;
        if (m1 == null || m2 == null) return false;
        if (m1.size() != m2.size()) return false;

        for (Map.Entry<String, Object> entry : m1.entrySet()) {
            String key = entry.getKey();
            if (!m2.containsKey(key)) return false;
            if (!Objects.equals(entry.getValue(), m2.get(key))) return false;
        }
        return true;
    }
}
