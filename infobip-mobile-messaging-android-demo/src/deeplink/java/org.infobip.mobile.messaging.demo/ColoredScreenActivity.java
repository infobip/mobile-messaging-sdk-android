/*
 * ColoredScreenActivity.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.infobip.mobile.messaging.Message;

/**
 * @author sslavin
 * @since 31/08/2017.
 */

abstract class ColoredScreenActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colored_screen);

        View layout = findViewById(R.id.rl_screen);
        layout.setBackgroundColor(getBackgroundColor());

        Message message = Message.createFrom(getIntent().getExtras());
        TextView tv = findViewById(R.id.tv_text);
        tv.setText(message.getBody());
    }

    protected abstract int getBackgroundColor();
}
