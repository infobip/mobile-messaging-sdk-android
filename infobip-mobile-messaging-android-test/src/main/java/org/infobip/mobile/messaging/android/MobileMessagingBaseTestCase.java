package org.infobip.mobile.messaging.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.ValueMatcher;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.util.ArrayList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * @author sslavin
 * @since 10/03/2017.
 */

@RunWith(AndroidJUnit4.class)
public abstract class MobileMessagingBaseTestCase {

    protected Context context;
    protected Context contextMock;
    protected DebugServer debugServer;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Before
    public void setUp() throws Exception {

        context = getInstrumentation().getContext();
        contextMock = mockContext(context);

        debugServer = new DebugServer();
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, "{\n" +
                "  \"code\": \"500\",\n" +
                "  \"message\": \"Internal server error\"\n" +
                "}");
        debugServer.start();
    }

    private static Context mockContext(final Context realContext) {
        Context contextSpy = Mockito.spy(Context.class);

        Mockito.when(contextSpy.getSharedPreferences(Mockito.anyString(), Mockito.anyInt())).thenAnswer(new Answer<SharedPreferences>() {
            @Override
            public SharedPreferences answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                return realContext.getSharedPreferences((String) arguments[0], (Integer) arguments[1]);
            }
        });
        Mockito.when(contextSpy.getPackageManager()).thenReturn(realContext.getPackageManager());
        Mockito.when(contextSpy.getApplicationInfo()).thenReturn(realContext.getApplicationInfo());
        Mockito.when(contextSpy.getApplicationContext()).thenReturn(realContext.getApplicationContext());
        Mockito.when(contextSpy.getMainLooper()).thenReturn(realContext.getMainLooper());
        Mockito.when(contextSpy.getPackageName()).thenReturn(realContext.getPackageName());
        Mockito.when(contextSpy.getResources()).thenReturn(realContext.getResources());

        return contextSpy;
    }

    @After
    public void tearDown() throws Exception {
        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Asserts that two objects are strictly the same using JSONAssert and JSON serialization
     *
     * @param expected expected object
     * @param actual   actual object
     * @throws Exception if serialization or assertion fails
     */
    protected <T> void assertJEquals(T expected, T actual) throws Exception {
        JSONAssert.assertEquals(gson.toJson(expected), gson.toJson(actual), true);
    }

    /**
     * Asserts that two objects are strictly the same using JSONAssert and JSON serialization
     *
     * @param expected expected object
     * @param actual   actual object
     * @param ignoreFields array of fields to ignore when doing comparison
     * @throws Exception if serialization or assertion fails
     */
    protected <T> void assertJEquals(T expected, T actual, String... ignoreFields) throws Exception {
        List<Customization> customizations = new ArrayList<>(ignoreFields.length);
        for (String ignoreField : ignoreFields) {
            customizations.add(new Customization(ignoreField, new ValueMatcher<Object>() {
                @Override
                public boolean equal(Object o1, Object o2) {
                    return true;
                }
            }));
        }
        JSONAssert.assertEquals(gson.toJson(expected), gson.toJson(actual), new CustomComparator(JSONCompareMode.STRICT, customizations.toArray(new Customization[0])));
    }

    /**
     * Asserts that two objects are not strictly the same using JSONAssert and JSON serialization
     *
     * @param expected expected object
     * @param actual   actual object
     * @throws Exception if serialization or assertion fails
     */
    protected <T> void assertJNotEquals(T expected, T actual) throws Exception {
        JSONAssert.assertNotEquals(gson.toJson(expected), gson.toJson(actual), true);
    }
}
