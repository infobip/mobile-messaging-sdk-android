package org.infobip.mobile.messaging.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class PersonalizationCtxTest {

    @Test
    public void should_resolve_default_params() throws JSONException {
        JSONObject json = new JSONObject("{\"userIdentity\":{\"externalUserId\":\"someId\"}}");

        PersonalizationCtx personalizationCtx = PersonalizationCtx.resolvePersonalizationCtx(json);

        assertFalse(personalizationCtx.forceDepersonalize);
        assertFalse(personalizationCtx.keepAsLead);
        assertEquals("someId", personalizationCtx.userIdentity.getExternalUserId());
        assertNull(personalizationCtx.userAttributes);
        assertNull(personalizationCtx.userIdentity.getPhones());
        assertNull(personalizationCtx.userIdentity.getEmails());
    }
}
