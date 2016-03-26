package org.infobip.mobile.messaging.api.support.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public abstract class MapUtils {
    public static Map<String, Collection<Object>> map(Object... kvargs) {
        HashMap<String, Collection<Object>> hashMap = new HashMap<>();
        for (int i = 0; i < kvargs.length; i += 2) {
            hashMap.put(kvargs[i].toString(), Collections.singleton(kvargs[i + 1]));
        }
        return hashMap;
    }
}
