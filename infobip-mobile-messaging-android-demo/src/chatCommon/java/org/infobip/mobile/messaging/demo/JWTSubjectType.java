/*
 * JWTSubjectType.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

enum JWTSubjectType {
   EMAIL("email"),
   PHONE_NUMBER("msisdn"),
   EXTERNAL_PERSON_ID("externalPersonId");

   public final String stp;

   private JWTSubjectType(String stp){
      this.stp = stp;
   }
}
