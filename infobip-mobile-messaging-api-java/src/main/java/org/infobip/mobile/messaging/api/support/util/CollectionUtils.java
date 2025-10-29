/*
 * CollectionUtils.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CollectionUtils {
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection collection) {
        return !isEmpty(collection);
    }

    @SafeVarargs
    public static <C extends Collection<T>, T> C concat(C... cs) {
        List<C> collections = new LinkedList<>(Arrays.asList(cs));
        for (Iterator<C> iterator = collections.iterator(); iterator.hasNext(); ) {
            C collection = iterator.next();
            if (collection == null) {
                iterator.remove();
            }
        }

        if (collections.isEmpty()) {
            return null;
        }

        C result = collections.get(0);
        for (int i = 1; i < collections.size(); i++) {
            result.addAll(collections.get(i));
        }
        return result;
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... ts) {
        return new HashSet<>(Arrays.asList(ts));
    }
}
