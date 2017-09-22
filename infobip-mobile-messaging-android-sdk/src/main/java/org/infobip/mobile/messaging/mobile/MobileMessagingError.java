package org.infobip.mobile.messaging.mobile;

import android.util.Log;

import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendBaseException;

import java.io.Serializable;
import java.util.Locale;

public class MobileMessagingError implements Serializable {

    private final String code;
    private final String message;
    private final Type type;
    private final Throwable tr;

    public static MobileMessagingError createFrom(Throwable e) {
        if (e instanceof BackendBaseException) {
            return ((BackendBaseException) e).getError();
        } else if (e instanceof ApiIOException) {
            return new MobileMessagingError((ApiIOException) e);
        }
        return new MobileMessagingError(e);
    }

    public MobileMessagingError(String code, String message) {
        this(code, message, Type.SDK_ERROR, new Throwable(message));
    }

    private MobileMessagingError(ApiIOException apiException) {
        this(apiException.getCode(), apiException.getMessage(), Type.SERVER_ERROR, new Throwable(apiException.getMessage()));
    }

    private MobileMessagingError(Throwable e) {
        this("-10", e != null ? e.getMessage() : "Something went wrong", Type.UNKNOWN_ERROR, new Throwable(e != null ? e.getMessage() : "Something went wrong"));
    }

    private MobileMessagingError(String code, String message, Type type, Throwable tr) {
        this.code = code;
        this.message = message;
        this.type = type;
        this.tr = tr;
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
