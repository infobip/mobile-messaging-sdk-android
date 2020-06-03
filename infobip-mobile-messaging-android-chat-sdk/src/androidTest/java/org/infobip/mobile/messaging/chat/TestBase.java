package org.infobip.mobile.messaging.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.Event;
import org.mockito.ArgumentMatcher;

import java.util.Objects;
import java.util.Set;

import static org.mockito.Matchers.argThat;

/**
 * @author sslavin
 * @since 10/10/2017.
 */

public abstract class TestBase {

    protected Intent eqIntentWith(final Event action, final Bundle givenBundle) {
        return argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Object argument) {
                Intent intent = (Intent) argument;
                return checkEquals(intent.getExtras(), givenBundle)
                        && action.getKey().equals(intent.getAction());
            }
        });
    }

    @NonNull
    protected Bundle givenBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        return bundle;
    }

    // region internal methods

    private boolean checkEquals(Bundle first, Bundle second) {
        Set<String> aks = first.keySet();
        Set<String> bks = second.keySet();

        if (!aks.containsAll(bks)) {
            return false;
        }

        for (String key : aks) {
            if (!Objects.equals(first.get(key), second.get(key))) {
                return false;
            }
        }

        return true;
    }

    // endregion
}
