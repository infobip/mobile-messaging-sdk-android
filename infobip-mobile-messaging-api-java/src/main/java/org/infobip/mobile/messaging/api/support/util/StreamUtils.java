package org.infobip.mobile.messaging.api.support.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mstipanov
 * @since 03.03.2016.
 */
public abstract class StreamUtils {
    private StreamUtils() {
    }

    public static String readToString(InputStream inputStream, String charsetName, long length) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = inputStream.read();
        long count = 1;
        while (result != -1) {
            byte b = (byte) result;
            buf.write(b);
            if (count == length) {
                break;
            }
            result = inputStream.read();
            count++;
        }

        String stringFromStream = buf.toString(charsetName);
        if (stringFromStream.length() < 1) {
            return "";
        }
        return stringFromStream;
    }

    public static long write(String s, OutputStream outputStream, String charsetName) throws IOException {
        return write(s.getBytes(charsetName), outputStream);
    }

    private static long write(byte[] bytes, OutputStream outputStream) throws IOException {
        outputStream.write(bytes);
        return bytes.length;
    }

    public static void closeSafely(OutputStream outputStream) {
        if (null == outputStream) {
            return;
        }

        try {
            outputStream.close();
        } catch (IOException e) {
            //ignore
        }
    }
}
