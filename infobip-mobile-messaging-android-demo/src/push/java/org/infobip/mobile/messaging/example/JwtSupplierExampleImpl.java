/*
 * JwtSupplierExampleImpl.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.example;

import android.os.Build;

import androidx.annotation.Nullable;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.infobip.mobile.messaging.JwtSupplier;

import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Example of a {@link JwtSupplier} implementation which generates
 * JWT and can be provided to the SDK for testing. For production
 * applications the tokens should be generated and fetched from
 * a backend application.
 */
public class JwtSupplierExampleImpl implements JwtSupplier {
    private String externalUserId;

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }
    @Nullable
    @Override
    public String getJwt() {
        if (this.externalUserId == null) return null;
        try {
            //JWT should be fetched from your backend!
            return generateSignedJwt();
        } catch (Exception e) {
            return "GenerationError: " + e.getMessage();
        }
    }

    private String generateSignedJwt() throws Exception {
        String secretKeyHex = "YOUR_SECRET_KEY_HEX";
        String keyId = "YOUR_KEY_ID";
        String applicationCode = "YOUR_APPLICATION_CODE";
        MACSigner personalizationTokenSigner = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            personalizationTokenSigner = new MACSigner(HexFormat.of().parseHex(secretKeyHex));
        }
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("typ", "Bearer")
                .jwtID(UUID.randomUUID().toString())
                .subject(this.externalUserId)
                .issuer(applicationCode)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusMillis(10000)))
                .claim("infobip-api-key", applicationCode)
                .build();
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).keyID(keyId).build();
        SignedJWT personalizedToken = new SignedJWT(jwsHeader, claimsSet);
        personalizedToken.sign(personalizationTokenSigner);
        return personalizedToken.serialize();
    }
}
