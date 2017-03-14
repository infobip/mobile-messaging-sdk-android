package org.infobip.mobile.messaging.platform;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * @author sslavin
 * @since 14/03/2017.
 */

public class TimeTest {
    private class StubTimeProvider implements TimeProvider {

        private long time = 0;

        @Override
        public long now() {
            return time;
        }

        private void reset(long time) {
            this.time = time;
        }
    }

    private StubTimeProvider timeProvider;

    @Before
    public void setUp() throws Exception {
        timeProvider = new StubTimeProvider();
        Time.reset(timeProvider);
    }

    @Test
    public void test_should_set_time() {
        // When
        timeProvider.reset(12345L);

        // Then
        Assert.assertEquals(12345L, Time.now());
    }
}
