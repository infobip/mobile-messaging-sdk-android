package org.infobip.mobile.messaging.interactive.inapp.view;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.R;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.Executor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author sslavin
 * @since 24/04/2018.
 */
public class InAppViewDialogTest {

    private InAppViewDialog inAppViewDialog;

    private InAppView.Callback callback = mock(InAppView.Callback.class);
    private ActivityWrapper activityWrapper = mock(ActivityWrapper.class);

    private RelativeLayout rlDialogImage = mock(RelativeLayout.class);
    private TextView tvMessageText = mock(TextView.class);
    private ProgressBar pbDialog = mock(ProgressBar.class);
    private View dialogView = mock(View.class);
    private ImageView image = mock(ImageView.class);
    private AlertDialog alertDialog = mock(AlertDialog.class);
    private AlertDialog.Builder alertDialogBuilder = mock(AlertDialog.Builder.class);
    private Activity baseActivity = mock(Activity.class);

    private Executor syncExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    };


    @Before
    public void before() {
        reset(callback, activityWrapper, rlDialogImage, tvMessageText, pbDialog, dialogView, image);
        when(dialogView.findViewById(R.id.tv_msg_text)).thenReturn(tvMessageText);
        when(dialogView.findViewById(R.id.rl_dialog_image)).thenReturn(rlDialogImage);
        when(dialogView.findViewById(R.id.pb_dialog)).thenReturn(pbDialog);
        when(dialogView.findViewById(R.id.iv_dialog)).thenReturn(image);
        when(alertDialogBuilder.setOnDismissListener(any(DialogInterface.OnDismissListener.class))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.setView(eq(dialogView))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.setPositiveButton(anyInt(), any(InAppViewDialogClickListener.class))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.setNegativeButton(anyInt(), any(InAppViewDialogClickListener.class))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.setNeutralButton(anyInt(), any(InAppViewDialogClickListener.class))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.create()).thenReturn(alertDialog);
        when(activityWrapper.createAlertDialogBuilder()).thenReturn(alertDialogBuilder);
        when(activityWrapper.inflateView(eq(R.layout.in_app_dialog_image))).thenReturn(dialogView);
        inAppViewDialog = new InAppViewDialog(callback, syncExecutor, activityWrapper);
    }

    @Test
    public void shoudNotShowDialogInNoActions() {
        Message message = message();

        inAppViewDialog.show(message);

        verify(activityWrapper, never()).createAlertDialogBuilder();
    }

    @Test
    public void shouldSetupViewsAccordingToMessageContents() {
        Message message = message();
        NotificationAction actions[] = actions();

        inAppViewDialog.show(message, actions);

        verify(tvMessageText, times(1)).setText(message.getBody());
        verify(rlDialogImage, times(1)).setVisibility(View.VISIBLE);
        verify(pbDialog, times(1)).setVisibility(View.VISIBLE);
        verify(image, times(1)).setVisibility(View.VISIBLE);

        verify(activityWrapper, times(1)).createAlertDialogBuilder();

        verify(alertDialogBuilder, times(1)).setOnDismissListener(any(InAppViewDialogDismissListener.class));
        verify(alertDialogBuilder, times(1)).setView(eq(dialogView));
        verify(alertDialogBuilder, times(1)).create();
        verify(alertDialog, times(1)).show();
    }

    @Test
    public void shouldSetupOneButtonForOneAction() {
        Message message = message();
        NotificationAction action = action();

        inAppViewDialog.show(message, action);

        verify(alertDialogBuilder, times(1)).setPositiveButton(eq(action.getTitleResourceId()), any(InAppViewDialogClickListener.class));
    }

    @Test
    public void shouldSetupTwoButtonsForTwoActions() {
        Message message = message();
        NotificationAction action1 = action();
        NotificationAction action2 = action();

        inAppViewDialog.show(message, action1, action2);

        verify(alertDialogBuilder, times(1)).setNegativeButton(eq(action1.getTitleResourceId()), any(InAppViewDialogClickListener.class));
        verify(alertDialogBuilder, times(1)).setPositiveButton(eq(action2.getTitleResourceId()), any(InAppViewDialogClickListener.class));
    }

    @Test
    public void shouldSetupThreeButtonsForThreeActions() {
        Message message = message();
        NotificationAction action1 = action();
        NotificationAction action2 = action();
        NotificationAction action3 = action();

        inAppViewDialog.show(message, action1, action2, action3);

        verify(alertDialogBuilder, times(1)).setNegativeButton(eq(action1.getTitleResourceId()), any(InAppViewDialogClickListener.class));
        verify(alertDialogBuilder, times(1)).setNeutralButton(eq(action2.getTitleResourceId()), any(InAppViewDialogClickListener.class));
        verify(alertDialogBuilder, times(1)).setPositiveButton(eq(action3.getTitleResourceId()), any(InAppViewDialogClickListener.class));
    }

    private Message message() {
        return new Message() {{
            setBody("messageBody");
            setContentUrl("contentUrl");
        }};
    }

    private NotificationAction[] actions() {
        return new NotificationAction[]{action()};
    }

    private NotificationAction action() {
        NotificationAction action = mock(NotificationAction.class);
        when(action.getTitleResourceId()).thenReturn(new Random().nextInt());
        return action;
    }
}
