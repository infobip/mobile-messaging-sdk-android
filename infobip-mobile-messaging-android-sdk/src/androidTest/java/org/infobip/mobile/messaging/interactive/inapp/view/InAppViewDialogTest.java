package org.infobip.mobile.messaging.interactive.inapp.view;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.R;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.Executor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private TextView tvMessageTitle = mock(TextView.class);
    private View dialogView = mock(View.class);
    private ImageView image = mock(ImageView.class);
    private AlertDialog alertDialog = mock(AlertDialog.class);
    private AlertDialog.Builder alertDialogBuilder = mock(AlertDialog.Builder.class);

    private Executor syncExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    };


    @Before
    public void before() {
        reset(callback, activityWrapper, rlDialogImage, tvMessageText, tvMessageTitle, dialogView, image);
        when(dialogView.findViewById(R.id.tv_msg_text)).thenReturn(tvMessageText);
        when(dialogView.findViewById(R.id.tv_msg_title)).thenReturn(tvMessageTitle);
        when(dialogView.findViewById(R.id.rl_dialog_image)).thenReturn(rlDialogImage);
        when(dialogView.findViewById(R.id.iv_dialog)).thenReturn(image);
        when(alertDialogBuilder.setOnDismissListener(any(DialogInterface.OnDismissListener.class))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.setView(eq(dialogView))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.setPositiveButton(anyInt(), any(InAppViewDialogClickListener.class))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.setNegativeButton(anyInt(), any(InAppViewDialogClickListener.class))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.setNeutralButton(anyInt(), any(InAppViewDialogClickListener.class))).thenReturn(alertDialogBuilder);
        when(alertDialogBuilder.create()).thenReturn(alertDialog);
        when(activityWrapper.createAlertDialogBuilder(anyBoolean())).thenReturn(alertDialogBuilder);
        when(activityWrapper.inflateView(eq(R.layout.in_app_dialog_image))).thenReturn(dialogView);
        inAppViewDialog = new InAppViewDialog(callback, syncExecutor, activityWrapper);
    }

    @Test
    public void shouldNotShowDialogInNoActions() {
        Message message = message();
        NotificationCategory category = category();

        inAppViewDialog.show(message, category);

        verify(activityWrapper, never()).createAlertDialogBuilder(anyBoolean());
    }

    @Test
    public void shouldSetupViewsAccordingToMessageContents() {
        Message message = message();
        NotificationAction actions[] = actions();
        NotificationCategory category = category(actions);

        inAppViewDialog.showWithImage(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8), message, category, actions);

        verify(tvMessageText, times(1)).setText(message.getBody());
        verify(tvMessageTitle, times(1)).setVisibility(View.VISIBLE);
        verify(rlDialogImage, times(1)).setVisibility(View.VISIBLE);
        verify(image, times(1)).setVisibility(View.VISIBLE);

        verify(activityWrapper, times(1)).createAlertDialogBuilder(eq(true));

        verify(alertDialogBuilder, times(1)).setOnDismissListener(any(InAppViewDialogDismissListener.class));
        verify(alertDialogBuilder, times(1)).setView(eq(dialogView));
        verify(alertDialogBuilder, times(1)).create();
        verify(alertDialog, times(1)).show();
    }

    @Test
    public void shouldSetupViewsAccordingToMessageContentsWithoutImage() {
        Message message = message();
        NotificationAction actions[] = actions();
        NotificationCategory category = category(actions);

        inAppViewDialog.show(message, category, actions);

        verify(tvMessageText, times(1)).setText(message.getBody());
        verify(tvMessageTitle, times(1)).setVisibility(View.VISIBLE);
        verify(rlDialogImage, never()).setVisibility(View.VISIBLE);
        verify(image, never()).setVisibility(View.VISIBLE);

        verify(activityWrapper, times(1)).createAlertDialogBuilder(eq(true));

        verify(alertDialogBuilder, times(1)).setOnDismissListener(any(InAppViewDialogDismissListener.class));
        verify(alertDialogBuilder, times(1)).setView(eq(dialogView));
        verify(alertDialogBuilder, times(1)).create();
        verify(alertDialog, times(1)).show();
    }

    @Test
    public void shouldNotDisplayTitleAndImageFieldsIfDataNotProvided() {
        Message message = message();
        message.setTitle(null);
        message.setContentUrl(null);
        NotificationAction actions[] = actions();
        NotificationCategory category = category(actions);

        inAppViewDialog.show(message, category, actions);

        verify(tvMessageText, times(1)).setText(message.getBody());
        verify(tvMessageTitle, never()).setVisibility(View.VISIBLE);
        verify(rlDialogImage, never()).setVisibility(View.VISIBLE);
        verify(image, never()).setVisibility(View.VISIBLE);

        verify(activityWrapper, times(1)).createAlertDialogBuilder(eq(true));

        verify(alertDialogBuilder, times(1)).setOnDismissListener(any(InAppViewDialogDismissListener.class));
        verify(alertDialogBuilder, times(1)).setView(eq(dialogView));
        verify(alertDialogBuilder, times(1)).create();
        verify(alertDialog, times(1)).show();
    }

    @Test
    public void shouldDisplayDialogWithBuiltInThemeIfDefaultFails() {
        Message message = message();
        message.setTitle(null);
        message.setContentUrl(null);
        NotificationAction actions[] = actions();
        NotificationCategory category = category(actions);

        doThrow(new IllegalStateException("You need to use a Theme.AppCompat theme (or descendant) with this activity.")).doNothing().when(alertDialog).show();

        inAppViewDialog.show(message, category, actions);

        verify(activityWrapper, times(1)).createAlertDialogBuilder(eq(true));
        verify(activityWrapper, times(1)).createAlertDialogBuilder(eq(false));

        verify(alertDialog, times(2)).show();
    }

    @Test
    public void shouldSetupOneButtonForOneAction() {
        Message message = message();
        NotificationAction action = action();
        NotificationCategory category = category(action);

        inAppViewDialog.show(message, category, action);

        verify(alertDialogBuilder, times(1)).setPositiveButton(eq(action.getTitleResourceId()), any(InAppViewDialogClickListener.class));
    }

    @Test
    public void shouldSetupTwoButtonsForTwoActions() {
        Message message = message();
        NotificationAction action1 = action();
        NotificationAction action2 = action();
        NotificationCategory category = category(action1, action2);

        inAppViewDialog.show(message, category, action1, action2);

        verify(alertDialogBuilder, times(1)).setNegativeButton(eq(action1.getTitleResourceId()), any(InAppViewDialogClickListener.class));
        verify(alertDialogBuilder, times(1)).setPositiveButton(eq(action2.getTitleResourceId()), any(InAppViewDialogClickListener.class));
    }

    @Test
    public void shouldSetupThreeButtonsForThreeActions() {
        Message message = message();
        NotificationAction action1 = action();
        NotificationAction action2 = action();
        NotificationAction action3 = action();
        NotificationCategory category = category(action1, action2, action3);

        inAppViewDialog.show(message, category, action1, action2, action3);

        verify(alertDialogBuilder, times(1)).setNegativeButton(eq(action1.getTitleResourceId()), any(InAppViewDialogClickListener.class));
        verify(alertDialogBuilder, times(1)).setNeutralButton(eq(action2.getTitleResourceId()), any(InAppViewDialogClickListener.class));
        verify(alertDialogBuilder, times(1)).setPositiveButton(eq(action3.getTitleResourceId()), any(InAppViewDialogClickListener.class));
    }

    private Message message() {
        return new Message() {{
            setBody("messageBody");
            setContentUrl("contentUrl");
            setTitle("messageTitle");
        }};
    }

    private NotificationCategory category(NotificationAction... actions) {
        return new NotificationCategory("categoryId", actions);
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
