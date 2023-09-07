package org.infobip.mobile.messaging.util;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SHA256Test {
    @Test
    public void shouldCalculateSHA256() {
        assertEquals("0bc177ab1ee9c7433e7649b185ae1b8921971a34f17e284bdc5835cb39499ead", SHA256.calc("d404b3e8b1880f773854f47235bdd964-8178fbd4-0e92-46cd-aac6-0dec2b58e5df"));
    }
}