/*
 * AuthData.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.os.Parcel;
import android.os.Parcelable;

import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import androidx.annotation.NonNull;

class AuthData implements Parcelable {
    private final String name;
    private final JWTSubjectType jwtSubjectType;
    private final String subject;

    public AuthData(String name, JWTSubjectType jwtSubjectType, String subject) {
        this.name = name;
        this.jwtSubjectType = jwtSubjectType;
        this.subject = subject;
    }

    public UserIdentity getUserIdentity() {
        UserIdentity userIdentity = new UserIdentity();
        switch (jwtSubjectType) {
            case EMAIL:
                userIdentity.setEmails(new HashSet<>(Collections.singletonList(subject)));
                break;
            case PHONE_NUMBER:
                userIdentity.setPhones(new HashSet<>(Collections.singletonList(subject)));
                break;
            case EXTERNAL_PERSON_ID:
                userIdentity.setExternalUserId(subject);
                break;
        }
        return userIdentity;
    }

    public UserAttributes getUserAttributes() {
        UserAttributes userAttributes = new UserAttributes();
        String[] nameParts = name.split(" ");
        if (nameParts.length == 1) {
            userAttributes.setFirstName(nameParts[0]);
        } else if (nameParts.length == 2) {
            userAttributes.setFirstName(nameParts[0]);
            userAttributes.setLastName(nameParts[1]);
        } else {
            userAttributes.setFirstName(nameParts[0]);
            String middleName = String.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length - 2));
            userAttributes.setMiddleName(middleName);
            userAttributes.setLastName(nameParts[nameParts.length - 1]);
        }
        return userAttributes;
    }

    public JWTSubjectType getJwtSubjectType() {
        return jwtSubjectType;
    }

    public String getSubject() {
        return subject;
    }

    public static final Parcelable.Creator<AuthData> CREATOR = new Parcelable.Creator<AuthData>() {
        public AuthData createFromParcel(Parcel in) {
            String name = in.readString();
            JWTSubjectType jwtSubjectType = JWTSubjectType.values()[in.readInt()];
            String subject = in.readString();
            return new AuthData(name, jwtSubjectType, subject);
        }

        public AuthData[] newArray(int size) {
            return new AuthData[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(this.name);
        parcel.writeInt(this.jwtSubjectType.ordinal());
        parcel.writeString(this.subject);
    }

    @Override
    public String toString() {
        return "AuthData{" +
                "name='" + name + '\'' +
                ", jwtSubjectType=" + jwtSubjectType +
                ", subject='" + subject + '\'' +
                '}';
    }
}
