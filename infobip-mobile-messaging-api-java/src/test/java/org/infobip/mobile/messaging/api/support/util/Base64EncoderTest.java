package org.infobip.mobile.messaging.api.support.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Base64EncoderTest {

    @Test
    public void should_encode() {
        String s = " !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

        assertThat(Base64Encoder.encode(s))
                .isEqualTo("ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj9AQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVpbXF1eX2BhYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ent8fX4=");
    }
}
