/*
 * StringUtilsTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mstipanov
 * @since 18.03.2016.
 */
public class StringUtilsTest {

    @Test
    public void join_nullSeparator_noStrings_mustReturn_null() throws Exception {
        assertThat(StringUtils.join(null)).isNull();
    }

    @Test
    public void join_emptySeparator_noStrings_mustReturn_null() throws Exception {
        assertThat(StringUtils.join("")).isNull();
    }

    @Test
    public void join_emptySeparator_noString_mustReturn_empty() throws Exception {
        assertThat(StringUtils.join("", "")).isEmpty();
    }

    @Test
    public void join_noSeparatorsInStrings() throws Exception {
        assertThat(StringUtils.join("/", "a")).isEqualTo("a");
        assertThat(StringUtils.join("/", "a", "b")).isEqualTo("a/b");
    }

    @Test
    public void join_endsWithSeparator() throws Exception {
        assertThat(StringUtils.join("/", "a", "/")).isEqualTo("a/");
    }

    @Test
    public void join_manySeparators() throws Exception {
        assertThat(StringUtils.join("/", "a/", "/b", "/c/", "d/")).isEqualTo("a/b/c/d/");
        assertThat(StringUtils.join("/", "/", "a/", "/b", "/c/", "d/")).isEqualTo("/a/b/c/d/");
    }
}