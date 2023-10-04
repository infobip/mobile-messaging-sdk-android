package org.infobip.mobile.messaging.api.appinstance;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivechatDestination {

   @SerializedName("applicationId")
   private String widgetId;

   @SerializedName("userId")
   private String registrationId;

}
