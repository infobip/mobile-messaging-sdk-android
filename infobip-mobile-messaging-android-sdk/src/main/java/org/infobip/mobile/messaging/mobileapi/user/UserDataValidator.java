package org.infobip.mobile.messaging.mobileapi.user;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.CustomAttributesMapper;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validator for user data fields against People API limits.
 * Validates fields before sending to the backend to ensure compliance with API constraints.
 *
 * @see <a href="https://www.infobip.com/docs/api/customer-engagement/people/update-a-person">People API Documentation</a>
 */
public class UserDataValidator {

    // Field length limits based on People API specification
    private static final int MAX_FIRST_NAME_LENGTH = 255;
    private static final int MAX_LAST_NAME_LENGTH = 255;
    private static final int MAX_MIDDLE_NAME_LENGTH = 50;
    private static final int MAX_EXTERNAL_USER_ID_LENGTH = 256;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_EMAIL_COUNT = 100;
    private static final int MAX_PHONE_COUNT = 100;
    private static final int MAX_CUSTOM_ATTRIBUTE_VALUE_LENGTH = 4096;

    private static final String DOCS_URL = "https://www.infobip.com/docs/api/customer-engagement/people/update-a-person";

    private static final JsonSerializer jsonSerializer = new JsonSerializer();

    /**
     * Validates user data against People API field limits.
     *
     * @param user the user object to validate
     * @throws UserDataValidationException if validation fails
     */
    public static void validate(User user) throws UserDataValidationException {
        if (user == null) {
            return;
        }

        List<String> errors = new ArrayList<>();

        validateExternalUserId(user.getExternalUserId(), errors);
        validateEmails(user.getEmails(), errors);
        validatePhones(user.getPhones(), errors);
        validateFirstName(user.getFirstName(), errors);
        validateLastName(user.getLastName(), errors);
        validateMiddleName(user.getMiddleName(), errors);
        validateCustomAttributes(user.getCustomAttributes(), errors);

        if (!errors.isEmpty()) {
            throwValidationException(errors);
        }
    }

    /**
     * Validates user identity data against People API field limits.
     *
     * @param userIdentity the user identity object to validate
     * @throws UserDataValidationException if validation fails
     */
    public static void validate(UserIdentity userIdentity) throws UserDataValidationException {
        if (userIdentity == null) {
            return;
        }

        List<String> errors = new ArrayList<>();

        validateExternalUserId(userIdentity.getExternalUserId(), errors);
        validateEmails(userIdentity.getEmails(), errors);
        validatePhones(userIdentity.getPhones(), errors);

        if (!errors.isEmpty()) {
            throwValidationException(errors);
        }
    }

    /**
     * Validates user attributes data against People API field limits.
     *
     * @param userAttributes the user attributes object to validate
     * @throws UserDataValidationException if validation fails
     */
    public static void validate(UserAttributes userAttributes) throws UserDataValidationException {
        if (userAttributes == null) {
            return;
        }

        List<String> errors = new ArrayList<>();

        validateFirstName(userAttributes.getFirstName(), errors);
        validateLastName(userAttributes.getLastName(), errors);
        validateMiddleName(userAttributes.getMiddleName(), errors);
        validateCustomAttributes(userAttributes.getCustomAttributes(), errors);

        if (!errors.isEmpty()) {
            throwValidationException(errors);
        }
    }

    private static void validateExternalUserId(String externalUserId, List<String> errors) {
        if (externalUserId != null && externalUserId.length() > MAX_EXTERNAL_USER_ID_LENGTH) {
            errors.add(String.format("externalUserId exceeds maximum length of %d characters (actual: %d)",
                MAX_EXTERNAL_USER_ID_LENGTH, externalUserId.length()));
        }
    }

    private static void validateFirstName(String firstName, List<String> errors) {
        if (firstName != null && firstName.length() > MAX_FIRST_NAME_LENGTH) {
            errors.add(String.format("firstName exceeds maximum length of %d characters (actual: %d)",
                MAX_FIRST_NAME_LENGTH, firstName.length()));
        }
    }

    private static void validateLastName(String lastName, List<String> errors) {
        if (lastName != null && lastName.length() > MAX_LAST_NAME_LENGTH) {
            errors.add(String.format("lastName exceeds maximum length of %d characters (actual: %d)",
                MAX_LAST_NAME_LENGTH, lastName.length()));
        }
    }

    private static void validateMiddleName(String middleName, List<String> errors) {
        if (middleName != null && middleName.length() > MAX_MIDDLE_NAME_LENGTH) {
            errors.add(String.format("middleName exceeds maximum length of %d characters (actual: %d)",
                MAX_MIDDLE_NAME_LENGTH, middleName.length()));
        }
    }

    private static void validateEmails(Set<String> emails, List<String> errors) {
        if (emails == null) {
            return;
        }

        if (emails.size() > MAX_EMAIL_COUNT) {
            errors.add(String.format("emails count exceeds maximum of %d (actual: %d)",
                MAX_EMAIL_COUNT, emails.size()));
        }

        // Validate individual email lengths
        for (String email : emails) {
            if (email != null && email.length() > MAX_EMAIL_LENGTH) {
                errors.add(String.format("email '%s' exceeds maximum length of %d characters (actual: %d)",
                    email, MAX_EMAIL_LENGTH, email.length()));
            }
        }
    }

    private static void validatePhones(Set<String> phones, List<String> errors) {
        if (phones != null && phones.size() > MAX_PHONE_COUNT) {
            errors.add(String.format("phones count exceeds maximum of %d (actual: %d)",
                MAX_PHONE_COUNT, phones.size()));
        }
    }

    private static void validateCustomAttributes(Map<String, CustomAttributeValue> customAttributes, List<String> errors) {
        if (customAttributes == null) {
            return;
        }

        for (Map.Entry<String, CustomAttributeValue> entry : customAttributes.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            String valueAsString;
            CustomAttributeValue customAttributeValue = entry.getValue();

            // For CustomList types, serialize to JSON to get the actual length that will be sent to the API
            if (customAttributeValue.getType() == CustomAttributeValue.Type.CustomList) {
                try {
                    Object backendValue = CustomAttributesMapper.customValueToBackend(customAttributeValue);
                    valueAsString = jsonSerializer.serialize(backendValue);
                } catch (Exception e) {
                    // If serialization fails, log error and skip validation for this attribute
                    MobileMessagingLogger.e("Failed to serialize CustomList attribute '" + entry.getKey() + "': " + e.getMessage());
                    continue;
                }
            } else {
                valueAsString = customAttributeValue.toString();
            }

            if (valueAsString != null && valueAsString.length() > MAX_CUSTOM_ATTRIBUTE_VALUE_LENGTH) {
                errors.add(String.format("customAttribute '%s' value exceeds maximum length of %d characters (actual: %d)",
                    entry.getKey(), MAX_CUSTOM_ATTRIBUTE_VALUE_LENGTH, valueAsString.length()));
            }
        }
    }

    private static void throwValidationException(List<String> errors) throws UserDataValidationException {
        StringBuilder message = new StringBuilder("User data validation failed:\n");
        for (String error : errors) {
            message.append("  - ").append(error).append("\n");
        }
        message.append("\nPlease check the field limits at: ").append(DOCS_URL);

        String errorMessage = message.toString();
        MobileMessagingLogger.e(errorMessage);

        throw new UserDataValidationException(errorMessage);
    }
}
