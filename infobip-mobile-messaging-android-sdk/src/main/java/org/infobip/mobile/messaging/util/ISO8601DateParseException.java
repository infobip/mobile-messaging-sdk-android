package org.infobip.mobile.messaging.util;

/**
 * This exception is thrown when {@code Date} cannot be parsed to ISO 8601 date format, or if {@code Date} is invalid.
 *
 * @author pandric
 * @since sdk v1.3.14
 */
public class ISO8601DateParseException extends RuntimeException {
    public ISO8601DateParseException() {
        super();
    }

    public ISO8601DateParseException(String message) {
        super(message);
    }

    public ISO8601DateParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ISO8601DateParseException(Throwable cause) {
        super(cause);
    }

    public ISO8601DateParseException(Reason reason, Throwable throwable) {
        super(reason.message(), throwable);
    }

    public enum Reason {
        DATE_PARSE_EXCEPTION("Value cannot be parsed to java.util.Date. dateValue() should only be called if java.util.Date is expected.");

        private String message;

        Reason(String message) {
            this.message = message;
        }

        public String message() {
            return this.message;
        }

    }
}
