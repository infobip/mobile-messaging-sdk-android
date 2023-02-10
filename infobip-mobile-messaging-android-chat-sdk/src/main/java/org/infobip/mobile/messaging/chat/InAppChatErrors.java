package org.infobip.mobile.messaging.chat;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class InAppChatErrors {
    public static class Error {
        private final String type;
        private final String message;

        public Error(@ChatErrorsDef String type, String message) {
            this.type = type;
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Error error = (Error) o;
            return Objects.equals(type, error.type) && Objects.equals(message, error.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, message);
        }

        @NonNull
        @Override
        public String toString() {
            return "Error{" +
                    "type='" + type + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

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

    private final OnChangeListener listener;
    private final Set<Error> errors = new HashSet<>();

    public void insertError(Error error) {
        if (!errors.contains(error)) {
            errors.add(error);
            listener.onErrorsChange(errors, null, error);
        }
    }

    public boolean removeError(@ChatErrorsDef @NonNull String errorType) {
        for (Error error : errors) {
            if (errorType.equals(error.getType())) {
                errors.remove(error);
                listener.onErrorsChange(errors, error, null);
                return true;
            }
        }
        return false;
    }

    public interface OnChangeListener {
        void onErrorsChange(Set<Error> currentErrors, Error removedError, Error insertedError);
    }
}
