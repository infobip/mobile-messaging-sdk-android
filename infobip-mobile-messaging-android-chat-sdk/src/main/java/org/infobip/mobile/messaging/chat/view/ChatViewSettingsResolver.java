package org.infobip.mobile.messaging.chat.view;

import android.content.Context;

import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.util.ResourceLoader;

/**
 * Contains custom chat view settings that can be changed by user
 *
 * @author sslavin
 * @since 07/11/2017.
 */

public class ChatViewSettingsResolver {

    private static final String RES_ID_CHAT_VIEW_TITLE = "mm_chat_view_title";
    private static final String RES_ID_CHAT_VIEW_EMPTY_STATE_TEXT = "mm_chat_view_empty_state_text";
    private static final String RES_ID_CHAT_VIEW_THEME = "mm_chat_view_theme";

    private static String chatViewTitle;
    private static String chatViewEmptyStateText;
    private static int chatViewTheme;

    private final Context context;

    public ChatViewSettingsResolver(Context context) {
        this.context = context;
    }

    public String getChatViewTitle() {
        if (chatViewTitle != null) {
            return chatViewTitle;
        }

        chatViewTitle = getStringResourceByName(RES_ID_CHAT_VIEW_TITLE, R.string.IB_chat_view_title);
        return chatViewTitle;
    }

    public String getChatViewEmptyStateText() {
        if (chatViewEmptyStateText != null) {
            return chatViewEmptyStateText;
        }

        chatViewEmptyStateText = getStringResourceByName(RES_ID_CHAT_VIEW_EMPTY_STATE_TEXT, R.string.IB_chat_view_empty_state_text);
        return chatViewEmptyStateText;
    }

    public int getChatViewTheme() {
        if (chatViewTheme != 0) {
            return chatViewTheme;
        }

        chatViewTheme = getThemeResourceByName(RES_ID_CHAT_VIEW_THEME, R.style.IB_AppTheme);
        return chatViewTheme;
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
