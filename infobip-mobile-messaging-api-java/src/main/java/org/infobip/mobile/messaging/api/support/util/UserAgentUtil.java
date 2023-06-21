package org.infobip.mobile.messaging.api.support.util;

public class UserAgentUtil {
    private String userAgent;

    public String getUserAgent(String libraryVersion, String[] userAgentAdditions) {
        if (null != userAgent) {
            return userAgent;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Infobip Mobile API Client");
        if (StringUtils.isNotBlank(libraryVersion)) {
            sb.append("/").append(libraryVersion);
        }
        sb.append("(");
        if (null != userAgentAdditions) {
            sb.append(StringUtils.join(";", userAgentAdditions));
        }
        sb.append(")");
        userAgent = sb.toString();
        return userAgent;
    }
}
