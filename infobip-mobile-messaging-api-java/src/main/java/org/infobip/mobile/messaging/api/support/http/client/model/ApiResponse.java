package org.infobip.mobile.messaging.api.support.http.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private ApiError requestError;

    public ApiResponse(String code, String text) {
        requestError = new ApiError(new ApiServiceException(code, text));
    }
}
