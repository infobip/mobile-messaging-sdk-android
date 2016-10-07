package org.infobip.mobile.messaging.util;

import android.support.annotation.NonNull;

/**
 * @author sslavin
 * @since 04/10/2016.
 */

public class Version implements Comparable<Version> {

    private String version;

    public final String get() {
        return this.version;
    }

    public Version(String version) {
        if(version == null)
            throw new IllegalArgumentException("Version can not be null");
        if(!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\-?\\D*"))
            throw new IllegalArgumentException("Invalid version format");
        this.version = version;
    }

    @Override
    public int compareTo(@NonNull Version that) {
        String[] thisParts = this.get().split("[\\.\\-]");
        String[] thatParts = that.get().split("[\\.\\-]");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            try {
                int thisPart = i < thisParts.length ?
                        Integer.parseInt(thisParts[i]) : 0;
                int thatPart = i < thatParts.length ?
                        Integer.parseInt(thatParts[i]) : 0;
                if (thisPart < thatPart)
                    return -1;
                if (thisPart > thatPart)
                    return 1;
            } catch (NumberFormatException ne) {
                String thisPart = i < thisParts.length ? thisParts[i] : "";
                String thatPart = i < thatParts.length ? thatParts[i] : "";
                if (thisPart.isEmpty() && !thatPart.isEmpty()) {
                    return 1;
                }
                if (!thisPart.isEmpty() && thatPart.isEmpty()) {
                    return -1;
                }
                int comparison = thisPart.compareTo(thatPart);
                if (comparison != 0) {
                    return comparison;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object that) {
        if(this == that) {
            return true;
        }
        if(that == null) {
            return false;
        }
        if(this.getClass() != that.getClass()) {
            return false;
        }
        return this.compareTo((Version) that) == 0;
    }
}
