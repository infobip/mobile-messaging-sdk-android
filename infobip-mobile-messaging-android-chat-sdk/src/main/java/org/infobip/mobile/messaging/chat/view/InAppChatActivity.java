package org.infobip.mobile.messaging.chat.view;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils;

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

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = newBase != null ? LocalizationUtils.applyInAppChatLanguage(newBase) : null;
        super.attachBaseContext(context);
    }
}
