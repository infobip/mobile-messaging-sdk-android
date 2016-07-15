package org.infobip.mobile.messaging;

public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String detailMessage) {
        super(detailMessage);
    }

    public ConfigurationException(Reason reason) {
        super(reason.message());
    }

    public ConfigurationException(Throwable throwable) {
        super(throwable);
    }

    public ConfigurationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    enum Reason {
        MISSING_REQUIRED_SERVICE("Missing required service in AndroidManifest.xml, add: %s");

        private String message;

        Reason(String message) {
            this.message = message;
        }

        public String message() {
            return this.message;
        }

    }
}
