/*
 * WebViewSettingsResolver.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.app;

import android.content.Context;

import org.infobip.mobile.messaging.R;
import org.infobip.mobile.messaging.util.ResourceLoader;

public class WebViewSettingsResolver {

    private static final String RES_ID_WEBVIEW_THEME = "IB_AppTheme.WebView";

    private static int webViewTheme;

    private final Context context;

    public WebViewSettingsResolver(Context context) {
        this.context = context;
    }

    public int getWebViewTheme() {
        if (webViewTheme != 0) {
            return webViewTheme;
        }

        webViewTheme = getThemeResourceByName(RES_ID_WEBVIEW_THEME, R.style.IB_WebViewTheme);
        return webViewTheme;
    }

    // region private methods

    private int getThemeResourceByName(String name, int fallbackResourceId) {
        int resourceId = ResourceLoader.loadResourceByName(context, "style", name);
        if (resourceId == 0) {
            resourceId = fallbackResourceId;
        }
        return resourceId;
    }

    // endregion
}

