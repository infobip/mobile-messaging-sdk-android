package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class UserAttributes extends UserCustomAttributeHolder {

    String firstName;
    String lastName;
    String middleName;
    Gender gender;
    String birthday;
    Set<String> tags;

    public enum Gender {
        Male, Female
    }

    public UserAttributes() {
        super();
    }

    public UserAttributes(Map<String, CustomAttributeValue> customAttributes) {
        super(customAttributes);
    }

    public UserAttributes(String firstName,
                          String lastName,
                          String middleName,
                          Gender gender,
                          String birthday,
                          Set<String> tags,
                          Map<String, CustomAttributeValue> customAttributes) {
        super(customAttributes);
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.gender = gender;
        this.birthday = birthday;
        this.tags = tags;
    }

    /**
     * Gets user's tags.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Sets user's tags.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUser(User, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUser(User)
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
        setField(UserAtts.tags, tags);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        setField(UserAtts.firstName, firstName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        setField(UserAtts.lastName, lastName);
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
        setField(UserAtts.middleName, middleName);
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
        setField(UserAtts.gender, gender != null ? gender.name() : null);
    }

    public Date getBirthday() {
        try {
            return DateTimeUtil.dateFromYMDStringLocale(birthday);
        } catch (ParseException e) {
            return null;
        }
    }

    public void setBirthday(Date birthday) {
        this.birthday = DateTimeUtil.dateToYMDString(birthday);
        setField(UserAtts.birthday, this.birthday);
    }

    String getBirthdayString() {
        return birthday;
    }

    void setBirthdayString(String birthday) {
        this.birthday = birthday;
        setField(UserAtts.birthday, birthday);
    }
}
