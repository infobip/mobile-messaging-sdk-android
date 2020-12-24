package org.infobip.mobile.messaging.chat.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import org.infobip.mobile.messaging.chat.R;

public class InAppChatActivity extends AppCompatActivity implements InAppChatFragment.InAppChatActionBarProvider {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ib_activity_chat);
    }

    /* InAppChatActionBarProvider */
    @Override
    @Nullable
    public ActionBar getOriginalSupportActionBar() {
        return getSupportActionBar();
    }

    @Override
    public void onInAppChatBackPressed() {
        onBackPressed();
    }
}
