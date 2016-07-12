package org.infobip.mobile.messaging;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Actionable {

    public static final String CHAT = "chatMessage";
    public static final String COUPON = "chatCoupon";
    public static final String EXTRA_COUPON_URL = "couponUrl";

    @SerializedName("interactive")
    private Interactive interactive;

    public Interactive getInteractive() {
        return interactive;
    }

    public class Interactive {

        @SerializedName("button_actions")
        private ButtonActions buttonActions;

        public ButtonActions getButtonActions() {
            return buttonActions;
        }

        public class ButtonActions {

            @SerializedName("apply")
            private List<Apply> apply;

            public String getCouponUrl() {
                if (apply == null || apply.isEmpty()) {
                    return "";
                }

                return apply.get(0).openUrl;
            }

            private class Apply {
                @SerializedName("open_url")
                private String openUrl;
            }
        }
    }
}
