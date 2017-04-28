package org.infobip.mobile.messaging;

public enum LocalEvent {

    /**
     * It is triggered when geo message is received.
     * <p>
     * Contains the received message information.
     * <pre>
     * {@code
     * Message message = Message.createFrom(intent.getExtras());
     * }
     * </pre>
     *
     * @see Message
     */
    GEO_MESSAGE_RECEIVED("org.infobip.mobile.messaging.GEO_MESSAGE_RECEIVED"),

    /**
     * It is triggered when Application goes in foreground.
     */
    APPLICATION_FOREGROUND("org.infobip.mobile.messaging.APPLICATION_FOREGROUND");

    private final String key;

    LocalEvent(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
