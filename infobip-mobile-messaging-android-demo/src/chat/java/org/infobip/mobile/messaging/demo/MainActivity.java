package org.infobip.mobile.messaging.demo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.InAppChat;
import org.infobip.mobile.messaging.chat.view.InAppChatFragment;

/**
 * @author sslavin
 * @since 13/11/2017.
 */

public class MainActivity extends AppCompatActivity implements InAppChatFragment.InAppChatActionBarProvider {

    /* InAppChatActionBarProvider */
    @Nullable
    @Override
    public ActionBar getOriginalSupportActionBar() {
        return getSupportActionBar();
    }

    @Override
    public void onInAppChatBackPressed() {
        InAppChat.getInstance(MainActivity.this).hideInAppChatFragment(getSupportFragmentManager());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InAppChat.getInstance(this).activate();
        setContentView(R.layout.activity_main);
        setSupportActionBar(this.findViewById(R.id.toolbar));

        Button btn = findViewById(R.id.btn_open);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Uncomment one of the following variants
                //1. Shows in-app chat as Fragment
//                InAppChat.getInstance(MainActivity.this).showInAppChatFragment(getSupportFragmentManager(), R.id.fragmentContainer);

                //2. Shows in-app chat as Activity
                InAppChat.getInstance(MainActivity.this).inAppChatView().show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_id) {
            copyRegistrationIdToClipboard();
            Toast.makeText(this, R.string.toast_registration_id_copy, Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getGroupId() == R.id.languages) {
            String language = langMenuIdToLocale(item.getItemId());
            //change language of chat view
            InAppChat.getInstance(MainActivity.this).setLanguage(language);
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyRegistrationIdToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Registration ID", MobileMessaging.getInstance(this).getInstallation().getPushRegistrationId());
        clipboard.setPrimaryClip(clip);
    }

    public String langMenuIdToLocale(@IdRes int menuId) {
        if (menuId == R.id.english)
            return "en-US";
        else if (menuId == R.id.turkish)
            return "tr-TR";
        else if (menuId == R.id.korean)
            return "ko-KR";
        else if (menuId == R.id.russian)
            return "ru-RU";
        else if (menuId == R.id.chinese)
            return "zh-TW";
        else if (menuId == R.id.spanish)
            return "es-ES";
        else if (menuId == R.id.portuguese)
            return "pt-PT";
        else if (menuId == R.id.polish)
            return "pl-PL";
        else if (menuId == R.id.romanian)
            return "ro-RO";
        else if (menuId == R.id.arabic)
            return "ar-AE";
        else if (menuId == R.id.bosnian)
            return "bs-BA";
        else if (menuId == R.id.croatian)
            return "hr-HR";
        else if (menuId == R.id.greek)
            return "el-GR";
        else if (menuId == R.id.swedish)
            return "sv-SE";
        else if (menuId == R.id.thai)
            return "th-TH";
        else if (menuId == R.id.lithuanian)
            return "lt-LT";
        else if (menuId == R.id.danish)
            return "da-DK";
        else if (menuId == R.id.latvian)
            return "lv-LV";
        else if (menuId == R.id.hungarian)
            return "hu-HU";
        else if (menuId == R.id.italian)
            return "it-IT";
        else if (menuId == R.id.french)
            return "fr-FR";
        else if (menuId == R.id.slovenian)
            return "sl-SI";
        else if (menuId == R.id.ukrainian)
            return "uk-UA";
        else if (menuId == R.id.japanese)
            return "ja-JP";
        else return null;
    }
}
