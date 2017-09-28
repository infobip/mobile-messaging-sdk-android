package org.infobip.mobile.messaging.demo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * @author sslavin
 * @since 22/09/16.
 */

class Utils {
    static void saveToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", text);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, R.string.saved_to_clipboard, Toast.LENGTH_SHORT).show();
    }
}
