package org.infobip.mobile.messaging.api.chat;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WidgetAttachmentConfig {
   private long maxSize;
   private boolean isEnabled;
   private Set<String> allowedExtensions;
}
