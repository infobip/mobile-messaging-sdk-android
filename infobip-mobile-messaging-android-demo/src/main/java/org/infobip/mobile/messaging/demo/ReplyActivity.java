package org.infobip.mobile.messaging.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingProperty;

public class ReplyActivity extends AppCompatActivity {

    private TextView tvBody;
    private EditText etReply;
    private Button bnClose;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply);

        tvBody = (TextView) findViewById(R.id.tv_body);
        etReply = (EditText) findViewById(R.id.et_reply);
        bnClose = (Button) findViewById(R.id.bn_close);

        Bundle bundle = getIntent().getBundleExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey());
        if (bundle != null) {
            Message message = new Message(bundle);
            tvBody.setText(message.getBody());
        }

        bnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
