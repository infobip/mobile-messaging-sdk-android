/*
 * JWTUtils.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.util.Base64;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.json.JSONObject;

import java.util.Date;
import java.util.UUID;

public abstract class JWTUtils {

    private JWTUtils(){
    }

    public static String createJwt(JWTSubjectType subjectType, String subject, String widgetId, String secretKeyJson) {
        if (subjectType != null && subject != null && widgetId != null && secretKeyJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(secretKeyJson);
                String keyId = jsonObject.getString("id");
                String keySecret = jsonObject.getString("key");
                MACSigner personalizationTokenSigner = new MACSigner(Base64.decode(keySecret, Base64.DEFAULT));
                String uuid = UUID.randomUUID().toString();

                JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                        .jwtID(uuid)
                        .subject(subject)
                        .issuer(widgetId)
                        .issueTime(new Date())
                        .expirationTime(new Date(System.currentTimeMillis() + 10000))
                        .claim("ski", keyId)
                        .claim("stp", subjectType.stp) //subjectType
                        .claim("sid", uuid) //session id
                        .build();

                SignedJWT personalizedToken = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
                personalizedToken.sign(personalizationTokenSigner);
                return personalizedToken.serialize();
            } catch (Exception e) {
                MobileMessagingLogger.e("Create JWT process failed!", e);
            }
        }
        return null;
    }

}
