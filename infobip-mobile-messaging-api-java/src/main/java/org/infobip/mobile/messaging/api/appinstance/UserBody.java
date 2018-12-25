package org.infobip.mobile.messaging.api.appinstance;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User data synchronization response.
 *
 * @author sslavin
 * @since 15/07/16.
 */
@Data
@NoArgsConstructor
public class UserBody {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    private String externalUserId;
    private String firstName;
    private String lastName;
    private String middleName;
    private String birthday;
    private String gender;

    private Set<Email> emails;
    private Set<Gsm> gsms;
    private Set<String> tags;
    private Map<String, Object> customAttributes;
    private List<AppInstanceWithPushRegId> instances;

    public static class Gsm {
        String number;
        Boolean preferred;

        public Gsm(String number) {
            this(number, null);
        }

        public Gsm(String number, Boolean preferred) {
            this.number = number;
            this.preferred = null;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public Boolean getPreferred() {
            return preferred;
        }

        public void setPreferred(Boolean preferred) {
            this.preferred = preferred;
        }

        @Override
        public int hashCode() {
            final int prime = 9;
            int result = 1;
            result = appendToHash(result, prime, number);
            result = appendToHash(result, prime, preferred);
            return result;
        }

        private static int appendToHash(int result, int prime, Object o) {
            return prime * result + (o == null ? 0 : o.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }

            Gsm other = (Gsm) o;
            return org.apache.commons.codec.binary.StringUtils.equals(other.number, this.number);
        }
    }

    public static class Email {
        String address;
        Boolean preferred;

        public Email(String address) {
            this(address, null);
        }

        public Email(String address, Boolean preferred) {
            this.address = address;
            this.preferred = null;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Boolean getPreferred() {
            return preferred;
        }

        public void setPreferred(Boolean preferred) {
            this.preferred = preferred;
        }

        @Override
        public int hashCode() {
            final int prime = 9;
            int result = 1;
            result = appendToHash(result, prime, address);
            result = appendToHash(result, prime, preferred);
            return result;
        }

        private static int appendToHash(int result, int prime, Object o) {
            return prime * result + (o == null ? 0 : o.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }

            Email other = (Email) o;
            return org.apache.commons.codec.binary.StringUtils.equals(other.address, this.address);
        }
    }

    @Override
    public String toString() {
        return serializer.serialize(this);
    }
}
