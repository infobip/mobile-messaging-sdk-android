package org.infobip.mobile.messaging.api.support.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
}
