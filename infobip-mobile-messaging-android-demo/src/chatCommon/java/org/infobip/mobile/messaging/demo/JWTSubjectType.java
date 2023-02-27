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
