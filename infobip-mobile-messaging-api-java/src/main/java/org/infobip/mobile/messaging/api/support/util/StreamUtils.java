package org.infobip.mobile.messaging.api.support.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * @author mstipanov
 * @since 03.03.2016.
 */
public abstract class StreamUtils {
    private StreamUtils() {
    }

    public static String readToString(InputStream inputStream, String charsetName, int contentLength) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(inputStream, charsetName)) {
            StringBuilder sb = (contentLength != -1)
                    ? new StringBuilder(contentLength)
                    : new StringBuilder();
            char[] buf = new char[1024];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            return sb.toString();
        }
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
