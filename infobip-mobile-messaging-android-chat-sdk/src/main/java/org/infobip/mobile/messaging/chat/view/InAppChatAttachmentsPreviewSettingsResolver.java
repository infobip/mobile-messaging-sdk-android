package org.infobip.mobile.messaging.chat.view;

import android.content.Context;

import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.util.ResourceLoader;

/**
 * Contains custom chat view settings that can be changed by user
 */
class InAppChatAttachmentsPreviewSettingsResolver {

    private static final String RES_ID_CHAT_ATTACH_THEME = "IB_AppTheme.ChatAttach";

    private static int chatViewTheme;

    private final Context context;

    InAppChatAttachmentsPreviewSettingsResolver(Context context) {
        this.context = context;
    }

    int getChatAttachPreviewTheme() {
        if (chatViewTheme != 0) {
            return chatViewTheme;
        }

        chatViewTheme = getThemeResourceByName(RES_ID_CHAT_ATTACH_THEME, R.style.IB_AppTheme);
        return chatViewTheme;
    }

    private int getThemeResourceByName(String name, int fallbackResourceId) {
        int resourceId = ResourceLoader.loadResourceByName(context, "style", name);
        if (resourceId == 0) {
            resourceId = fallbackResourceId;
        }
        return resourceId;
    }
}
