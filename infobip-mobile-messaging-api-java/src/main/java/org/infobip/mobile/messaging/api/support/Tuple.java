package org.infobip.mobile.messaging.api.support;

import lombok.Data;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
@Data
public class Tuple<L,R> {
    private final L left;
    private final R right;
}
