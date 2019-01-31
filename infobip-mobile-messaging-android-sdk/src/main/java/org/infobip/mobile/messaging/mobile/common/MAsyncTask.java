package org.infobip.mobile.messaging.mobile.common;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;
import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationException;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationExceptionWithContent;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendInvalidParameterExceptionWithContent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Wrapper object over native AsyncTask
 * for easier handling of background operations.
 *
 * @author sslavin
 * @since 23/07/2017.
 */
public abstract class MAsyncTask<IN, OUT> extends IMAsyncTask<IN, OUT> {

    private final static Set<String> invalidParameterErrorCodes = new HashSet<String>() {{
        add(ApiErrorCode.INVALID_MSISDN_FORMAT);
        add(ApiErrorCode.INVALID_VALUE);
        add(ApiErrorCode.INVALID_EMAIL_FORMAT);
        add(ApiErrorCode.INVALID_BIRTHDATE_FORMAT);
        add(ApiErrorCode.USER_MERGE_INTERRUPTED);
        add(ApiErrorCode.USER_DATA_RESTRICTED);
        add(ApiErrorCode.PERSONALIZATION_IMPOSSIBLE);
        add(ApiErrorCode.REQUEST_FORMAT_INVALID);
    }};

    @SuppressLint("StaticFieldLeak")
    private final AsyncTask<IN, Void, ResultWrapper<IN, OUT>> asyncTask = new AsyncTask<IN, Void, ResultWrapper<IN, OUT>>() {
        @Override
        protected void onPreExecute() {
            before();
        }

        @Override
        protected ResultWrapper<IN, OUT> doInBackground(IN[] ins) {
            try {
                if (shouldCancel()) {
                    return new ResultWrapper<>(ins, true);
                } else {
                    OUT out = run(ins);
                    afterBackground(out);
                    return new ResultWrapper<>(out);
                }
            } catch (Throwable error) {
                return new ResultWrapper<>(ins, error);
            }
        }

        @Override
        protected void onPostExecute(ResultWrapper<IN, OUT> resultWrapper) {
            MobileMessagingLogger.v("Result wrapper: ", resultWrapper);
            if (resultWrapper.cancelled) {
                cancelled(resultWrapper.inputs);
                return;
            }

            if (resultWrapper.error == null) {
                after(resultWrapper.result);
                return;
            }

            Throwable error = backendErrorToTaskError(resultWrapper.error);
            error(error);
            error(resultWrapper.inputs, error);
        }
    };

    /**
     * Starts execution of background task
     *
     * @param ins input parametes
     */
    @SuppressWarnings({"unused", "unchecked"})
    public void execute(IN... ins) {
        asyncTask.execute(ins);
    }

    /**
     * Starts execution of background task with the provided executor
     *
     * @param executor executor to use for task execution.
     * @param ins      input parameters.
     */
    @SuppressWarnings({"unused", "unchecked"})
    public void execute(Executor executor, IN... ins) {
        asyncTask.executeOnExecutor(executor, ins);
    }

    // region private methods

    private static Throwable backendErrorToTaskError(Throwable originalError) {
        if (isInvalidParameterErrorWithContent(originalError)) {
            return new BackendInvalidParameterExceptionWithContent(originalError.getMessage(), (ApiBackendExceptionWithContent) originalError);
        }

        if (isErrorWithContent(originalError)) {
            return new BackendCommunicationExceptionWithContent(originalError.getMessage(), (ApiBackendExceptionWithContent) originalError);
        }

        if (isInvalidParameterError(originalError)) {
            return new BackendInvalidParameterException(originalError.getMessage(), (ApiIOException) originalError);
        }

        if (isBackendError(originalError)) {
            return new BackendCommunicationException(originalError.getMessage(), (ApiIOException) originalError);
        }

        return originalError;
    }

    private static boolean isBackendError(@NonNull Throwable error) {
        if (!(error instanceof ApiIOException)) {
            return false;
        }

        String errorCode = ((ApiIOException) error).getCode();
        return DefaultApiClient.ErrorCode.UNKNOWN_API_BACKEND_ERROR.getValue().equals(errorCode);
    }

    private static boolean isInvalidParameterError(@NonNull Throwable error) {
        if (!(error instanceof ApiIOException)) {
            return false;
        }

        String code = ((ApiIOException) error).getCode();
        return invalidParameterErrorCodes.contains(code);
    }

    private static boolean isErrorWithContent(@NonNull Throwable error) {
        return error instanceof ApiBackendExceptionWithContent;
    }

    private static boolean isInvalidParameterErrorWithContent(@NonNull Throwable error) {
        return error instanceof ApiBackendExceptionWithContent && isInvalidParameterError(error);
    }

    // endregion
}
