package org.infobip.mobile.messaging.chat;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * A simple participant of a chat
 *
 * @author sslavin
 * @since 05/10/2017.
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class ChatParticipant {
    /**
     * Unique ID of a participant
     */
    private String id;

    /**
     * First name
     */
    private String firstName;

    /**
     * Last name
     */
    private String lastName;

    /**
     * Middle name
     */
    private String middleName;

    /**
     * Email
     */
    private String email;

    /**
     * GSM
     */
    private String gsm;

    /**
     * Any custom data that can be attached to a participant
     */
    private JSONObject customData;

    // region Boilerplate code

    public ChatParticipant(String id, String firstName, String lastName, String middleName, String email, String gsm, JSONObject customData) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.email = email;
        this.gsm = gsm;
        this.customData = customData;
    }

    public ChatParticipant(String id) {
        this(id, null, null, null, null, null, null);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getUserName() {
        String userName;
        if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName)) {
            userName = firstName.trim() + " " + lastName.trim();
        } else {
            userName = !TextUtils.isEmpty(firstName) ? firstName.trim()
                    : !TextUtils.isEmpty(lastName) ? lastName.trim() : "";
        }

        return userName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGsm() {
        return gsm;
    }

    public void setGsm(String gsm) {
        this.gsm = gsm;
    }

    public JSONObject getCustomData() {
        return customData;
    }

    public void setCustomData(JSONObject customData) {
        this.customData = customData;
    }

    // endregion
}
