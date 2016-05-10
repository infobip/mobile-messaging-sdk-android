package org.infobip.mobile.messaging.util;

/**
 * A {@code MissingResourceException} is thrown by ResourceLoader when a
 * resource bundle cannot be found or a resource is missing from a resource
 * bundle.
 *
 * @see java.lang.RuntimeException
 */
public class MissingAndroidResourceException extends RuntimeException {

    private static final long serialVersionUID = -4876345176062000401L;

    String className, key;

    /**
     * Constructs a new {@code MissingResourceException} with the stack trace,
     * message, the class name of the resource bundle and the name of the
     * missing resource filled in.
     *
     * @param detailMessage the detail message for the exception.
     * @param className     the class name of the resource bundle.
     * @param resourceName  the name of the missing resource.
     */
    public MissingAndroidResourceException(String detailMessage, String className,
                                           String resourceName, Exception cause) {
        super(detailMessage, cause);
        this.className = className;
        key = resourceName;
    }

    /**
     * Returns the class name of the resource bundle from which a resource could
     * not be found, or in the case of a missing resource, the name of the
     * missing resource bundle.
     *
     * @return the class name of the resource bundle.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the name of the missing resource, or an empty string if the
     * resource bundle is missing.
     *
     * @return the name of the missing resource.
     */
    public String getKey() {
        return key;
    }

}
