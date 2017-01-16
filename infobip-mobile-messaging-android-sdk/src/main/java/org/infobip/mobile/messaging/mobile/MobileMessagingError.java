package org.infobip.mobile.messaging.mobile;

import android.util.Log;

import org.infobip.mobile.messaging.api.support.ApiIOException;

import java.io.Serializable;
import java.util.Locale;

public class MobileMessagingError implements Serializable {

    private final String code;
    private final String message;
    private final Type type;
    private final Throwable tr;

    public static MobileMessagingError createFrom(Throwable e) {
        return new MobileMessagingError(e);
    }

    public MobileMessagingError(String code, String message) {
        this.code = code;
        this.message = message;
        this.type = Type.SDK_ERROR;
        this.tr = new Throwable(message);
    }

    public MobileMessagingError(String code, String message, Throwable tr) {
        this.code = code;
        this.message = message;
        this.type = Type.SDK_ERROR;
        this.tr = tr;
    }

    private MobileMessagingError(Throwable e) {
        if (e != null && e instanceof ApiIOException) {
            ApiIOException apiIOException = (ApiIOException) e;
            this.code = apiIOException.getCode();
            this.message = apiIOException.getMessage();
            this.type = Type.SERVER_ERROR;
            this.tr = new Throwable(message);

        } else {
            this.code = "-10";
            this.message = e != null ? e.getMessage() : "Something went wrong";
            this.type = Type.UNKNOWN_ERROR;
            this.tr = new Throwable(message);
        }
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Type getType() {
        return type;
    }

    public String getStacktrace() {
        return tr != null ? Log.getStackTraceString(tr) : null;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%s, %s, %s", type.name(), code, message);
    }

    public enum Type {
        UNKNOWN_ERROR,
        SDK_ERROR,
        SERVER_ERROR
    }
}
