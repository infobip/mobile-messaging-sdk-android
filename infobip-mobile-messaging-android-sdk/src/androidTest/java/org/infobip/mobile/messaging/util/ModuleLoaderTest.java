package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

/**
 * @author sslavin
 * @since 06/10/2017.
 */

@RunWith(AndroidJUnit4.class)
public class ModuleLoaderTest {

    private Context mockedContext;

    interface BaseClass {}
    public static class AClass implements BaseClass {}
    public static class BClass implements BaseClass {}
    public static class CClass implements BaseClass {}

    @Before
    public void setUp() throws Exception {
        mockedContext = mock(Context.class);
    }

    @Test
    public void shouldCreateInstancesBasedOnMetadata() throws Exception {

        // Given
        Bundle givenMetadata = new Bundle();
        givenMetadata.putString(AClass.class.getName(), BaseClass.class.getName());
        givenMetadata.putString("something", "something else");
        givenMetadata.putString(BClass.class.getName(), BaseClass.class.getName());
        givenMetadata.putByte("something1", (byte) 0);
        givenMetadata.putString(CClass.class.getName(), BaseClass.class.getName());
        givenMetadata.putFloat("something2", 1f);
        givenMetadata.putLong("something3", 2L);
        givenMetadata.putInt("something4", 3);
        givenMetadata.putStringArray("something5", new String[]{"a", "b"});
        givenMetadataWillBe(givenMetadata);

        ModuleLoader givenModuleLoader = new ModuleLoader(mockedContext);

        // When
        Map<String, BaseClass> actualModuleMap = givenModuleLoader.loadModules(BaseClass.class);

        // Then
        assertEquals(3, actualModuleMap.size());
        assertTrue(actualModuleMap.get(AClass.class.getName()) instanceof AClass);
        assertTrue(actualModuleMap.get(BClass.class.getName()) instanceof BClass);
        assertTrue(actualModuleMap.get(CClass.class.getName()) instanceof CClass);
    }

    private void givenMetadataWillBe(Bundle metadata) throws PackageManager.NameNotFoundException {
        ApplicationInfo ai = new ApplicationInfo();
        ai.metaData = metadata;

        PackageManager mockedPM = mock(PackageManager.class);
        given(mockedContext.getPackageManager()).willReturn(mockedPM);
        given(mockedPM.getApplicationInfo(anyString(), anyInt())).willReturn(ai);
    }

}
