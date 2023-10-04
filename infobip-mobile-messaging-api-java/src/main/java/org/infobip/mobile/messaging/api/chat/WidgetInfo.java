package org.infobip.mobile.messaging.api.chat;

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
    private long maxUploadContentSize;
    private String language;
    private boolean multiThread;
    private boolean callsAvailable = true;
}