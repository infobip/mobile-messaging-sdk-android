package org.infobip.mobile.messaging.util;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author sslavin
 * @since 17/08/2018.
 */
public class SHA1Test {

    @Test
    public void shouldCalculateSHA1() {
        assertEquals("0066265d53fd2a9e46c73abddb4393ed25b94c05", SHA1.calc("d404b3e8b1880f773854f47235bdd964-8178fbd4-0e92-46cd-aac6-0dec2b58e5df"));
    }
}
