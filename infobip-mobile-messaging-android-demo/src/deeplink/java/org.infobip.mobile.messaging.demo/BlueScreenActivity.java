/*
 * BlueScreenActivity.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import org.infobip.mobile.messaging.demo.R;

/**
 * @author sslavin
 * @since 31/08/2017.
 */

public class BlueScreenActivity extends ColoredScreenActivity {
    @Override
    protected int getBackgroundColor() {
        return getResources().getColor(R.color.blue);
    }
}
