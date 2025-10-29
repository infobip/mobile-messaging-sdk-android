/*
 * BatchReporter.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi;

import org.infobip.mobile.messaging.platform.SystemTimeProvider;
import org.infobip.mobile.messaging.platform.TimeProvider;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author sslavin
 * @since 07/07/16.
 */
public class BatchReporter {

    private final long delay;
    private final TimeProvider timeProvider;
    private final Timer timer = new Timer();
    private volatile TimerTask timerTask = null;
    private volatile long lastSubmitted = 0;

    public BatchReporter(Long batchReportingDelay, TimeProvider timeProvider) {
        this.delay = batchReportingDelay;
        this.timeProvider = timeProvider;
    }

    public BatchReporter(Long batchReportingDelay) {
        this(batchReportingDelay, new SystemTimeProvider());
    }

    public synchronized void put(final Runnable task) {
        if (timerTask != null) {
            timerTask.cancel();
            timer.purge();
        }

        long now = timeProvider.now();
        if (now - lastSubmitted >= delay) {
            runNow(task);
            return;
        }

        long after = lastSubmitted + delay - now;
        scheduleWithDelay(task, after);
    }

    private void runNow(Runnable task) {
        lastSubmitted = timeProvider.now();
        task.run();
    }

    private void scheduleWithDelay(final Runnable task, long delay) {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runNow(task);
            }
        };
        timer.schedule(timerTask, delay);
    }
}
