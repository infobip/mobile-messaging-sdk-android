package org.infobip.mobile.messaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UserMapperTest  {

    @Test
    public void test_customValueNull() throws Exception {
        Object backendCustomValue = UserMapper.customValueToBackend(null);

        assertNull(backendCustomValue);
    }

    @Test
    public void test_customValueString() throws Exception {
        Object backendCustomValue = UserMapper.customValueToBackend(new CustomAttributeValue("string"));

        assertEquals("string", backendCustomValue);
    }
}
