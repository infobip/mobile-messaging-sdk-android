package org.infobip.mobile.messaging.chat.view;

import android.content.Context;

import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.util.ResourceLoader;

/**
 * Contains custom chat view settings that can be changed by user
 */
class InAppChatViewSettingsResolver {

    private static final String RES_ID_CHAT_VIEW_TITLE = "ib_in_app_chat_view_title";
    private static final String RES_ID_CHAT_VIEW_THEME = "IB_AppTheme.Chat";
    private static final String RES_ID_CHAT_ATTACH_THEME = "IB_AppTheme.ChatAttach";

    private static String chatViewTitle;
    private static int chatViewTheme;
    private static int chatAttachPreviewTheme;

    private final Context context;

    InAppChatViewSettingsResolver(Context context) {
        this.context = context;
    }

    String getChatViewTitle() {
        if (chatViewTitle != null) {
            return chatViewTitle;
        }

        chatViewTitle = getStringResourceByName(RES_ID_CHAT_VIEW_TITLE, R.string.ib_chat_view_title);
        return chatViewTitle;
    }

    int getChatViewTheme() {
        if (chatViewTheme != 0) {
            return chatViewTheme;
        }

        chatViewTheme = getThemeResourceByName(RES_ID_CHAT_VIEW_THEME, R.style.IB_AppTheme);
        return chatViewTheme;
    }

    int getChatAttachPreviewTheme() {
        if (chatAttachPreviewTheme != 0) {
            return chatAttachPreviewTheme;
        }

        chatAttachPreviewTheme = getThemeResourceByName(RES_ID_CHAT_ATTACH_THEME, R.style.IB_ChatAttachmentPreviewDefaultTheme);
        return chatAttachPreviewTheme;
    }

    // region private methods

    private String getStringResourceByName(String name, int fallbackResourceId) {
        String value = ResourceLoader.loadStringResourceByName(context, name);
        if (value == null) {
            value = context.getString(fallbackResourceId);
        }
        return value;
    }

    private int getThemeResourceByName(String name, int fallbackResourceId) {
        int resourceId = ResourceLoader.loadResourceByName(context, "style", name);
        if (resourceId == 0) {
            resourceId = fallbackResourceId;
        }
        return resourceId;
    }

    // endregion
}
