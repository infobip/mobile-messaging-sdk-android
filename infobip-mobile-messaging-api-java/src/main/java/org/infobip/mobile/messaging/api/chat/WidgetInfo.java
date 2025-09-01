package org.infobip.mobile.messaging.api.chat;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WidgetInfo {

    private String id;

    private String title;

    private String primaryColor;

    private String backgroundColor;

    private String primaryTextColor;

    private boolean multiThread;

    private boolean multiChannelConversationEnabled;

    private boolean callsEnabled;

    private List<String> themeNames;

    @SerializedName("attachments")
    private WidgetAttachmentConfig attachmentConfig;

}