package org.infobip.mobile.messaging.platform;

import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

/**
 * @author sslavin
 * @since 13/03/2017.
 */

public class AndroidBroadcasterTest extends MobileMessagingTestCase {

    private AndroidBroadcaster broadcastSender;
    private ArgumentCaptor<Intent> intentArgumentCaptor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        broadcastSender = new AndroidBroadcaster(contextMock);
        intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
    }

    public void test_should_send_message_broadcast() {
        // Given
        Message message = createMessage(context, "SomeMessageId", false);

        // When
        broadcastSender.messageReceived(message);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.MESSAGE_RECEIVED.getKey(), intent.getAction());

        Message messageAfter = Message.createFrom(intent.getExtras());
        assertNotSame(message, messageAfter);
        assertEquals("SomeMessageId", messageAfter.getMessageId());
    }

    public void test_should_send_geo_broadcast() throws Exception {
        // Given
        Message message = createMessage(context, "SomeMessageId", false);
        Area area = createArea("SomeAreaId");
        Geo geo = createGeo(1.0, 2.0, "SomeCampaignId", area);

        // When
        broadcastSender.geoEvent(GeoEventType.entry, message, geo);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.GEOFENCE_AREA_ENTERED.getKey(), intent.getAction());

        Message messageAfter = Message.createFrom(intent.getExtras());
        assertNotSame(message, messageAfter);
        assertEquals("SomeMessageId", messageAfter.getMessageId());

        Geo geoAfter = Geo.createFrom(intent.getExtras());
        assertNotSame(geo, geoAfter);
        assertJEquals(geo, geoAfter);
    }

    public void test_should_send_geo_reports() throws Exception {
        // Given
        GeoReport report = createReport(context, "SomeSignalingMessageId", "SomeCampaignId", "SomeSDKMessageId", false);

        // When
        broadcastSender.geoReported(Collections.singletonList(report));

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.GEOFENCE_EVENTS_REPORTED.getKey(), intent.getAction());

        List<GeoReport> reportsAfter = GeoReport.createFrom(intent.getExtras());
        assertEquals(1, reportsAfter.size());
        assertJEquals(report, reportsAfter.get(0));
    }

    public void test_should_send_error() throws Exception {
        // Given
        MobileMessagingError error = new MobileMessagingError("SomeCode", "SomeMessage");

        // When
        broadcastSender.error(error);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.API_COMMUNICATION_ERROR.getKey(), intent.getAction());

        MobileMessagingError errorAfter = (MobileMessagingError) intent.getSerializableExtra(BroadcastParameter.EXTRA_EXCEPTION);
        assertJEquals(error, errorAfter);
    }

    public void test_should_send_push_registration_enabled() {

        // When
        broadcastSender.registrationEnabled("SomeCloudToken", "SomeDeviceInstanceId", false);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.PUSH_REGISTRATION_ENABLED.getKey(), intent.getAction());
        assertEquals("SomeCloudToken", intent.getStringExtra(BroadcastParameter.EXTRA_GCM_TOKEN));
        assertEquals("SomeDeviceInstanceId", intent.getStringExtra(BroadcastParameter.EXTRA_INFOBIP_ID));
        assertEquals(false, intent.getBooleanExtra(BroadcastParameter.EXTRA_PUSH_REGISTRATION_ENABLED, true));
    }

    public void test_should_send_push_registration_acquired() {
        // When
        broadcastSender.registrationAcquired("SomeCloudToken");

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.REGISTRATION_ACQUIRED.getKey(), intent.getAction());
        assertEquals("SomeCloudToken", intent.getStringExtra(BroadcastParameter.EXTRA_GCM_TOKEN));
    }

    public void test_should_send_push_registration_created() {
        // When
        broadcastSender.registrationCreated("SomeCloudToken", "SomeDeviceInstanceId");

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.REGISTRATION_CREATED.getKey(), intent.getAction());
        assertEquals("SomeCloudToken", intent.getStringExtra(BroadcastParameter.EXTRA_GCM_TOKEN));
        assertEquals("SomeDeviceInstanceId", intent.getStringExtra(BroadcastParameter.EXTRA_INFOBIP_ID));
    }

    public void test_should_send_delivery_report_message_ids() {
        // When
        broadcastSender.deliveryReported("id1", "id2");

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.DELIVERY_REPORTS_SENT.getKey(), intent.getAction());
        assertEquals("id1", intent.getStringArrayExtra(BroadcastParameter.EXTRA_MESSAGE_IDS)[0]);
        assertEquals("id2", intent.getStringArrayExtra(BroadcastParameter.EXTRA_MESSAGE_IDS)[1]);
    }

    public void test_should_send_seen_report_message_ids() {
        // When
        broadcastSender.seenStatusReported("id1", "id2");

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.SEEN_REPORTS_SENT.getKey(), intent.getAction());
        assertEquals("id1", intent.getStringArrayExtra(BroadcastParameter.EXTRA_MESSAGE_IDS)[0]);
        assertEquals("id2", intent.getStringArrayExtra(BroadcastParameter.EXTRA_MESSAGE_IDS)[1]);
    }

    public void test_should_send_mo_messages() {
        // Given
        Message message = createMessage(context, "SomeMessageId", false);

        // When
        broadcastSender.messagesSent(Collections.singletonList(message));

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.MESSAGES_SENT.getKey(), intent.getAction());

        List<Message> messagesAfter = Message.createFrom(intent.<Bundle>getParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES));
        assertEquals(1, messagesAfter.size());
        assertEquals("SomeMessageId", messagesAfter.get(0).getMessageId());

    }

    public void test_should_send_user_data() {
        // Given
        UserData userData = new UserData();
        userData.setFirstName("FirstName");
        userData.setLastName("LastName");

        // When
        broadcastSender.userDataReported(userData);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.USER_DATA_REPORTED.getKey(), intent.getAction());

        UserData userDataAfter = UserData.createFrom(intent.getExtras());
        assertNotSame(userData, userDataAfter);
        assertEquals("FirstName", userDataAfter.getFirstName());
        assertEquals("LastName", userDataAfter.getLastName());
    }

    public void test_should_send_system_data() throws Exception {
        // Given
        SystemData systemData = new SystemData("SomeSdkVersion", "SomeOsVersion", "SomeDeviceManufacturer", "SomeDeviceModel", "SomeAppVersion", false, true);

        // When
        broadcastSender.systemDataReported(systemData);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.SYSTEM_DATA_REPORTED.getKey(), intent.getAction());

        SystemData systemDataAfter = SystemData.createFrom(intent.getExtras());
        assertNotSame(systemData, systemDataAfter);
        assertJEquals(systemData, systemDataAfter);
    }

    public void test_should_set_package_for_intent() throws Exception {
        // Given
        Message message = createMessage(context, "SomeMessageId", false);
        Mockito.when(contextMock.getPackageName()).thenReturn("test.package.name");

        // When
        broadcastSender.messageReceived(message);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());
        assertEquals("test.package.name", intentArgumentCaptor.getValue().getPackage());
    }

    public void test_should_send_notification_tapped_event() throws Exception {
        // Given
        Message message = createMessage(context, "SomeMessageId", false);

        // When
        broadcastSender.notificationTapped(message);

        // Then
        Mockito.verify(contextMock, Mockito.times(1)).sendBroadcast(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertEquals(Event.NOTIFICATION_TAPPED.getKey(), intent.getAction());

        Message messageAfter = Message.createFrom(intent.getExtras());
        assertNotSame(message, messageAfter);
        assertEquals("SomeMessageId", messageAfter.getMessageId());
    }
}
