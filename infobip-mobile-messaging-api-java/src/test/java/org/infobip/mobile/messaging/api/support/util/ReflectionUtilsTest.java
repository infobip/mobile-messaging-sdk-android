/*
 * ReflectionUtilsTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.util;

import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO make this work on gradle!
 *
 * @author mstipanov
 * @since 21.03.2016.
 */
@Ignore
public class ReflectionUtilsTest {

    @Test
    public void loadPackageInfo_fromFile() throws Exception {
        Package p = Package.getPackage("org.infobip.mobile.messaging.api");
        assertThat(p).isNull();
        ReflectionUtils.loadPackageInfo("org.infobip.mobile.messaging.api");
        p = Package.getPackage("org.infobip.mobile.messaging.api");
        assertThat(p).isNotNull();
    }

    @Test
    public void loadPackageInfo_fromJar() throws Exception {
        Package p = Package.getPackage("lombok.experimental");
        assertThat(p).isNull();
        ReflectionUtils.loadPackageInfo("lombok.experimental");
        p = Package.getPackage("lombok.experimental");
        assertThat(p).isNotNull();
    }
}