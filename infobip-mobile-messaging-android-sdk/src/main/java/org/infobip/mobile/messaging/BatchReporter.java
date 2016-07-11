package org.infobip.mobile.messaging;

import android.content.Context;

import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author sslavin
 * @since 07/07/16.
 */
public class BatchReporter {

    private long delay;
    private Timer timer = new Timer();
    private TimerTask timerTask;

    public BatchReporter(Context context) {
        delay = PreferenceHelper.findLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY);
    }

    public synchronized void put(final Runnable task) {
        if (this.timerTask != null) {
            timerTask.cancel();
            timer.purge();
        }
        timerTask = new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        };
        timer.schedule(timerTask, delay);
    }
}
