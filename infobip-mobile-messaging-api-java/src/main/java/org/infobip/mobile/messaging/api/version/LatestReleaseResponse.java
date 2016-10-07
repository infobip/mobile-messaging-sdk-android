package org.infobip.mobile.messaging.api.version;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Latest release version response.
 *
 * @author sslavin
 * @see MobileApiVersion
 * @see MobileApiVersion#getLatestRelease()
 * @since 03.10.2016.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatestReleaseResponse {
    String platformType;
    String libraryVersion;
    String updateUrl;
}
