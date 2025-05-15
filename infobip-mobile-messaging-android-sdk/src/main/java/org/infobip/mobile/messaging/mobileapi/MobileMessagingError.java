package org.infobip.mobile.messaging.mobileapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendBaseException;
import org.infobip.mobile.messaging.util.JwtExpirationException;
import org.infobip.mobile.messaging.util.JwtStructureValidationException;

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
        } else if (e instanceof JwtStructureValidationException) {
            return InternalSdkError.JWT_TOKEN_STRUCTURE_INVALID.getError(e.getMessage());
        } else if (e instanceof JwtExpirationException) {
            return InternalSdkError.JWT_TOKEN_EXPIRED.getError(e.getMessage());
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

    @NonNull
    public String getCode() {
        return code;
    }

    @NonNull
    public String getMessage() {
        if (message == null) return "";
        return message;
    }

    @NonNull
    public Type getType() {
        return type;
    }

    @Nullable
    public String getStacktrace() {
        return tr != null ? Log.getStackTraceString(tr) : null;
    }

    @NonNull
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
