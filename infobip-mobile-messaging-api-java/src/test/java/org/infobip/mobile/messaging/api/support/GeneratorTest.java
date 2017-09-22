package org.infobip.mobile.messaging.api.support;

import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author mstipanov
 * @since 18.03.2016.
 */
public class GeneratorTest {

    private Generator generator;

    @Before
    public void setUp() throws Exception {
        generator = new Generator.Builder().withBaseUrl("X").build();
    }

    @Test
    public void build_noBaseUrl_default() throws Exception {
        Generator build = new Generator.Builder().build();
        assertEquals("https://oneapi.infobip.com/", build.getBaseUrl());
    }

    @Test(expected = NullPointerException.class)
    public void build_nullBaseUrl_shouldThrow() throws Exception {
        new Generator.Builder().withBaseUrl(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void build_nullProperties_shouldThrow() throws Exception {
        new Generator.Builder().withBaseUrl("X").withProperties(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void build_emptyBaseUrl_shouldThrow() throws Exception {
        new Generator.Builder().withBaseUrl("").build();
    }

    @Test(expected = NoHttpRequestAnnotation.class)
    public void invoke_nonAnnotatedMethod_shouldThrow() throws Exception {
        generator.create(WrongClass.class).bar();
    }

    private interface WrongClass {
        @HttpRequest
        String foo();

        String bar();
    }
}