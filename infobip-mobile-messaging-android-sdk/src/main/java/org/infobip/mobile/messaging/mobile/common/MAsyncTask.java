package org.infobip.mobile.messaging.mobile.common;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationException;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendInvalidParameterException;

import java.util.concurrent.Executor;

/**
 * Wrapper object over native AsyncTask
 * for easier handling of background operations.
 *
 * @author sslavin
 * @since 23/07/2017.
 */
public abstract class MAsyncTask<IN, OUT> {

    @SuppressLint("StaticFieldLeak")
    private AsyncTask<IN, Void, ResultWrapper<IN, OUT>> asyncTask = new AsyncTask<IN, Void, ResultWrapper<IN, OUT>>() {
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

            if (isInvalidParameterError(resultWrapper.error)) {
                Throwable error = new BackendInvalidParameterException(resultWrapper.error.getMessage(), (ApiIOException) resultWrapper.error);
                error(error);
                error(resultWrapper.inputs, error);
                return;
            }

            if (isBackendError(resultWrapper.error)) {
                Throwable error = new BackendCommunicationException(resultWrapper.error.getMessage(), (ApiIOException) resultWrapper.error);
                error(error);
                error(resultWrapper.inputs, error);
                return;
            }

            error(resultWrapper.error);
            error(resultWrapper.inputs, resultWrapper.error);
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
     * @param ins original input parameters.
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
        return ApiErrorCode.INVALID_MSISDN_FORMAT.equals(code) ||
                ApiErrorCode.INVALID_VALUE.equals(code);
    }
}
