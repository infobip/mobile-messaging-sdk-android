package org.infobip.mobile.messaging.demo.screens;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.demo.R;

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
        TextView tv = (TextView) findViewById(R.id.tv_text);
        if (message != null) {
            tv.setText(message.getBody());
        } else {
            tv.setText(R.string.no_message_provided);
        }

    }

    protected abstract int getBackgroundColor();
}
