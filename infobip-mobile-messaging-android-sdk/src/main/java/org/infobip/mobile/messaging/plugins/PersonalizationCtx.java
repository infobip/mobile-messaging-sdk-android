package org.infobip.mobile.messaging.plugins;

import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * PersonalizationContext data mapper for JSON conversion in plugins
 */
public class PersonalizationCtx {
    public UserIdentity userIdentity;
    public UserAttributes userAttributes;
    public boolean forceDepersonalize;
    public boolean keepAsLead;

    public static PersonalizationCtx resolvePersonalizationCtx(JSONObject args) throws JSONException, IllegalArgumentException {
        if (args == null) {
            throw new IllegalArgumentException("Cannot resolve personalization context from arguments");
        }

        PersonalizationCtx ctx = new PersonalizationCtx();
        ctx.forceDepersonalize = args.optBoolean("forceDepersonalize", false);
        ctx.userIdentity = UserJson.userIdentityFromJSON(args.getJSONObject("userIdentity"));
        ctx.userAttributes = UserJson.userAttributesFromJSON(args.optJSONObject("userAttributes"));
        ctx.keepAsLead = args.optBoolean("keepAsLead", false);
        return ctx;
    }
}
