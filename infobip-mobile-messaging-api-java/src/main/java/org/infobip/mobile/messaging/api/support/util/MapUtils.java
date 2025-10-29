/*
 * MapUtils.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

    @SafeVarargs
    public static <K, V> Map<K, V> concat(Map<K, V>... mps) {
        return concat(null, mps);
    }

    @SafeVarargs
    public static <K, V> Map<K, V> concatOrEmpty(Map<K, V>... mps) {
        return MapUtils.concat(Collections.<K, V>emptyMap(), mps);
    }

    @SafeVarargs
    private static <K, V> Map<K, V> concat(Map<K, V> valueIfEmpty, Map<K, V>... mps) {
        List<Map<K, V>> maps = new LinkedList<>(Arrays.asList(mps));
        for (Iterator<Map<K, V>> iterator = maps.iterator(); iterator.hasNext(); ) {
            Map<K, V> map = iterator.next();
            if (map == null) {
                iterator.remove();
            }
        }

        if (maps.isEmpty()) {
            return valueIfEmpty;
        }

        Map<K, V> result = new HashMap<>();
        for (int i = 0; i < maps.size(); i++) {
            result.putAll(maps.get(i));
        }
        return result;
    }
}
