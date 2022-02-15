package org.infobip.mobile.messaging.chat;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

public class InAppChatErrors {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({INTERNET_CONNECTION_ERROR, CONFIG_SYNC_ERROR, JS_ERROR})
    @interface ChatErrorsDef {
    }

    public static final String INTERNET_CONNECTION_ERROR = "INTERNET_CONNECTION_ERROR";
    public static final String CONFIG_SYNC_ERROR = "CONFIG_SYNC_ERROR";
    public static final String JS_ERROR = "JS_ERROR";

    public InAppChatErrors(OnChangeListener listener) {
        this.listener = listener;
    }

    private OnChangeListener listener;
    private Set<String> errors = new HashSet<>();

    public void insertError(@ChatErrorsDef String error) {
        if (!errors.contains(error)) {
            errors.add(error);
            listener.onErrorsChange(errors, null, error);
        }
    }

    public void removeError(@ChatErrorsDef String error) {
        if (errors.contains(error)) {
            errors.remove(error);
            listener.onErrorsChange(errors, error, null);
        }
    }

    public interface OnChangeListener {
        void onErrorsChange(Set<String> currentErrors, String removedError, String insertedError);
    }
}
