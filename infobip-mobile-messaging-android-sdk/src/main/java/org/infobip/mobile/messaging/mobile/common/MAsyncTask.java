package org.infobip.mobile.messaging.mobile.common;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;
import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient;
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
public abstract class MAsyncTask<IN, OUT> {

    private final static Set<String> invalidParameterErrorCodes = new HashSet<String>() {{
        add(ApiErrorCode.INVALID_MSISDN_FORMAT);
        add(ApiErrorCode.INVALID_VALUE);
        add(ApiErrorCode.INVALID_EMAIL_FORMAT);
        add(ApiErrorCode.INVALID_BIRTHDATE_FORMAT);
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
                return new ResultWrapper<>(run(ins));
            } catch (Throwable error) {
                return new ResultWrapper<>(ins, error);
            }
        }

        @Override
        protected void onPostExecute(ResultWrapper<IN, OUT> resultWrapper) {
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
     * Executed on UI thread before background processing.
     */
    public void before() {
    }

    /**
     * Executed in background thread.
     * If there is any exception in background callback,
     * it will be propagated to {@link MAsyncTask#error(Throwable)}.
     *
     * @param ins input parameters
     * @return result of operation
     */
    public abstract OUT run(IN ins[]);

    /**
     * Executed on UI thread after successful processing.
     * Not executed in case of error.
     *
     * @param out result of background operation.
     */
    public void after(OUT out) {
    }

    /**
     * Executed on UI thread in case of error.
     *
     * @param error error that happened during background execution.
     */
    public void error(Throwable error) {

    }

    /**
     * Executed on UI thread in case of error.
     *
     * @param ins   original input parameters.
     * @param error error that happened during background execution.
     */
    public void error(IN ins[], Throwable error) {

    }

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
