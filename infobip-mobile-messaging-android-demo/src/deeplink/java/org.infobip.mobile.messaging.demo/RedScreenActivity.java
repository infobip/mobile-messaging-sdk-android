package org.infobip.mobile.messaging.demo;

/**
 * @author sslavin
 * @since 31/08/2017.
 */

public class RedScreenActivity extends ColoredScreenActivity {
    @Override
    protected int getBackgroundColor() {
        return getResources().getColor(R.color.red);
    }
}
