package org.infobip.mobile.messaging.api.chat;

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
    private long maxUploadContentSize;
    private boolean multiThread;
    private boolean multiChannelConversationEnabled;
    private boolean callsEnabled;
    private List<String> themeNames;
}