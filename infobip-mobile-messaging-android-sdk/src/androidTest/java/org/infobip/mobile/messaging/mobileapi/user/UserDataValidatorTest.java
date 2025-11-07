/*
 * UserDataValidatorTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.user;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.ListCustomAttributeItem;
import org.infobip.mobile.messaging.ListCustomAttributeValue;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserDataValidatorTest {

    @Test
    public void test_validate_null_user_succeeds() {
        UserDataValidator.validate((User) null);
    }

    @Test
    public void test_validate_null_userIdentity_succeeds() {
        UserDataValidator.validate((UserIdentity) null);
    }

    @Test
    public void test_validate_null_userAttributes_succeeds() {
        UserDataValidator.validate((UserAttributes) null);
    }

    @Test
    public void test_validate_valid_user_succeeds() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setMiddleName("M");
        user.setExternalUserId("user123");
        user.setEmails(CollectionUtils.setOf("test@infobip.com"));
        user.setPhones(CollectionUtils.setOf("+1234567890"));

        UserDataValidator.validate(user);
    }

    @Test
    public void test_validate_firstName_exceeds_max_length() {
        User user = new User();
        user.setFirstName(createStringOfLength(256));

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("firstName"));
            // Check for numbers in the device's locale format
            String expected255 = String.format("%d", 255);
            String expected256 = String.format("%d", 256);
            assertTrue("Message should contain max length: " + expected255, e.getMessage().contains(expected255));
            assertTrue("Message should contain actual length: " + expected256, e.getMessage().contains(expected256));
        }
    }

    @Test
    public void test_validate_lastName_exceeds_max_length() {
        User user = new User();
        user.setLastName(createStringOfLength(256));

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("lastName"));
            String expected255 = String.format("%d", 255);
            assertTrue(e.getMessage().contains(expected255));
        }
    }

    @Test
    public void test_validate_middleName_exceeds_max_length() {
        User user = new User();
        user.setMiddleName(createStringOfLength(51));

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("middleName"));
            String expected50 = String.format("%d", 50);
            String expected51 = String.format("%d", 51);
            assertTrue(e.getMessage().contains(expected50));
            assertTrue(e.getMessage().contains(expected51));
        }
    }

    @Test
    public void test_validate_externalUserId_exceeds_max_length() {
        User user = new User();
        user.setExternalUserId(createStringOfLength(257));

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("externalUserId"));
            String expected256 = String.format("%d", 256);
            String expected257 = String.format("%d", 257);
            assertTrue(e.getMessage().contains(expected256));
            assertTrue(e.getMessage().contains(expected257));
        }
    }

    @Test
    public void test_validate_emails_exceeds_max_count() {
        User user = new User();
        Set<String> emails = new HashSet<>();
        for (int i = 0; i < 101; i++) {
            emails.add("email" + i + "@infobip.com");
        }
        user.setEmails(emails);

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("emails"));
            String expected100 = String.format("%d", 100);
            String expected101 = String.format("%d", 101);
            assertTrue(e.getMessage().contains(expected100));
            assertTrue(e.getMessage().contains(expected101));
        }
    }

    @Test
    public void test_validate_phones_exceeds_max_count() {
        User user = new User();
        Set<String> phones = new HashSet<>();
        for (int i = 0; i < 101; i++) {
            phones.add("+123456789" + i);
        }
        user.setPhones(phones);

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("phones"));
            String expected100 = String.format("%d", 100);
            String expected101 = String.format("%d", 101);
            assertTrue(e.getMessage().contains(expected100));
            assertTrue(e.getMessage().contains(expected101));
        }
    }

    @Test
    public void test_validate_customAttribute_exceeds_max_length() {
        User user = new User();
        Map<String, CustomAttributeValue> customAttributes = new HashMap<>();
        customAttributes.put("longValue", new CustomAttributeValue(createStringOfLength(4097)));
        user.setCustomAttributes(customAttributes);

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("customAttribute"));
            assertTrue(e.getMessage().contains("longValue"));
            String expected4096 = String.format("%d", 4096);
            String expected4097 = String.format("%d", 4097);
            assertTrue(e.getMessage().contains(expected4096));
            assertTrue(e.getMessage().contains(expected4097));
        }
    }

    @Test
    public void test_validate_multiple_errors_reported() {
        User user = new User();
        user.setFirstName(createStringOfLength(256));
        user.setMiddleName(createStringOfLength(51));
        user.setExternalUserId(createStringOfLength(257));

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            String message = e.getMessage();
            assertNotNull(message);
            // Should contain all three errors
            assertTrue(message.contains("firstName"));
            assertTrue(message.contains("middleName"));
            assertTrue(message.contains("externalUserId"));
        }
    }

    @Test
    public void test_validate_userIdentity_with_invalid_externalUserId() {
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setExternalUserId(createStringOfLength(257));

        try {
            UserDataValidator.validate(userIdentity);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("externalUserId"));
        }
    }

    @Test
    public void test_validate_userIdentity_with_too_many_emails() {
        UserIdentity userIdentity = new UserIdentity();
        Set<String> emails = new HashSet<>();
        for (int i = 0; i < 101; i++) {
            emails.add("email" + i + "@infobip.com");
        }
        userIdentity.setEmails(emails);

        try {
            UserDataValidator.validate(userIdentity);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("emails"));
        }
    }

    @Test
    public void test_validate_userAttributes_with_invalid_firstName() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFirstName(createStringOfLength(256));

        try {
            UserDataValidator.validate(userAttributes);
            fail("Expected UserDataValidation Exception");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("firstName"));
        }
    }

    @Test
    public void test_validate_max_length_fields_at_boundary() {
        User user = new User();
        user.setFirstName(createStringOfLength(255));
        user.setLastName(createStringOfLength(255));
        user.setMiddleName(createStringOfLength(50));
        user.setExternalUserId(createStringOfLength(256));

        Set<String> emails = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            emails.add("email" + i + "@infobip.com");
        }
        user.setEmails(emails);

        Set<String> phones = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            phones.add("+123456789" + i);
        }
        user.setPhones(phones);

        Map<String, CustomAttributeValue> customAttributes = new HashMap<>();
        customAttributes.put("maxLength", new CustomAttributeValue(createStringOfLength(4096)));
        user.setCustomAttributes(customAttributes);

        UserDataValidator.validate(user);
    }

    @Test
    public void test_validate_error_message_contains_docs_url() {
        User user = new User();
        user.setFirstName(createStringOfLength(256));

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("https://www.infobip.com/docs/api"));
        }
    }

    @Test
    public void test_validate_customAttribute_with_null_value() {
        User user = new User();
        Map<String, CustomAttributeValue> customAttributes = new HashMap<>();
        customAttributes.put("nullValue", null);
        user.setCustomAttributes(customAttributes);
        
        UserDataValidator.validate(user);
    }

    @Test
    public void test_validate_customAttribute_number_value() {
        User user = new User();
        Map<String, CustomAttributeValue> customAttributes = new HashMap<>();
        customAttributes.put("numberValue", new CustomAttributeValue(12345));
        user.setCustomAttributes(customAttributes);

        UserDataValidator.validate(user);
    }

    @Test
    public void test_validate_customAttribute_boolean_value() {
        User user = new User();
        Map<String, CustomAttributeValue> customAttributes = new HashMap<>();
        customAttributes.put("boolValue", new CustomAttributeValue(true));
        user.setCustomAttributes(customAttributes);

        UserDataValidator.validate(user);
    }

    @Test
    public void test_validate_email_exceeds_max_length() {
        User user = new User();
        String longEmail = createStringOfLength(256) + "@infobip.com";
        user.setEmails(CollectionUtils.setOf(longEmail));

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("email"));
            String expected255 = String.format("%d", 255);
            assertTrue(e.getMessage().contains(expected255));
        }
    }

    @Test
    public void test_validate_email_at_max_length() {
        User user = new User();
        // Create an email that's exactly 255 characters
        String emailPrefix = createStringOfLength(243); // 243 + "@" + "infobip.com" = 255
        String email = emailPrefix + "@infobip.com";
        user.setEmails(CollectionUtils.setOf(email));

        UserDataValidator.validate(user);
    }

    @Test
    public void test_validate_multiple_emails_one_too_long() {
        User user = new User();
        Set<String> emails = new HashSet<>();
        emails.add("valid@infobip.com");
        emails.add(createStringOfLength(256) + "@infobip.com");
        emails.add("another.valid@infobip.com");
        user.setEmails(emails);

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("email"));
            String expected255 = String.format("%d", 255);
            assertTrue(e.getMessage().contains(expected255));
        }
    }

    @Test
    public void test_validate_customList_exceeds_max_length() {
        User user = new User();
        Map<String, CustomAttributeValue> customAttributes = new HashMap<>();

        // Create a CustomList that when serialized will exceed 4096 characters
        List<ListCustomAttributeItem> items = new ArrayList<>();
        // Each item will be around 500 characters when serialized
        for (int i = 0; i < 10; i++) {
            ListCustomAttributeItem item = ListCustomAttributeItem.builder()
                    .putString("field" + i, createStringOfLength(500))
                    .build();
            items.add(item);
        }

        user.setListCustomAttribute("largeList", new ListCustomAttributeValue(items));

        try {
            UserDataValidator.validate(user);
            fail("Expected UserDataValidationException");
        } catch (UserDataValidationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("customAttribute"));
            assertTrue(e.getMessage().contains("largeList"));
            String expected4096 = String.format("%d", 4096);
            assertTrue(e.getMessage().contains(expected4096));
        }
    }

    @Test
    public void test_validate_customList_valid_size() {
        User user = new User();
        Map<String, CustomAttributeValue> customAttributes = new HashMap<>();

        // Create a small CustomList that won't exceed 4096 characters
        List<ListCustomAttributeItem> items = new ArrayList<>();
        ListCustomAttributeItem item = ListCustomAttributeItem.builder()
                .putString("name", "John Doe")
                .putNumber("age", 30)
                .putBoolean("active", true)
                .build();
        items.add(item);

        user.setListCustomAttribute("smallList", new ListCustomAttributeValue(items));

        UserDataValidator.validate(user);
    }

    @Test
    public void test_validate_customList_at_max_boundary() {
        User user = new User();

        // Create a CustomList that when serialized is close to but under 4096 characters
        List<ListCustomAttributeItem> items = new ArrayList<>();
        // Create items that will serialize to approximately 4000 characters
        ListCustomAttributeItem item = ListCustomAttributeItem.builder()
                .putString("data", createStringOfLength(3900))
                .build();
        items.add(item);

        user.setListCustomAttribute("boundaryList", new ListCustomAttributeValue(items));

        UserDataValidator.validate(user);
    }

    private String createStringOfLength(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}
