package com.infobip.webrtc.ui.listeners

import com.infobip.webrtc.sdk.api.event.call.CallEarlyMediaEvent
import com.infobip.webrtc.sdk.api.event.call.CallEstablishedEvent
import com.infobip.webrtc.sdk.api.event.call.CallHangupEvent
import com.infobip.webrtc.sdk.api.event.call.CallRingingEvent
import com.infobip.webrtc.sdk.api.event.call.CameraVideoAddedEvent
import com.infobip.webrtc.sdk.api.event.call.CameraVideoUpdatedEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceJoinedEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceLeftEvent
import com.infobip.webrtc.sdk.api.event.call.DialogJoinedEvent
import com.infobip.webrtc.sdk.api.event.call.DialogLeftEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantCameraVideoAddedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantCameraVideoRemovedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantDeafEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantJoinedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantJoiningEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantLeftEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantMutedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantScreenShareAddedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantScreenShareRemovedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantStartedTalkingEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantStoppedTalkingEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantUndeafEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantUnmutedEvent
import com.infobip.webrtc.sdk.api.event.call.ReconnectedEvent
import com.infobip.webrtc.sdk.api.event.call.ReconnectingEvent
import com.infobip.webrtc.sdk.api.event.call.ScreenShareAddedEvent
import com.infobip.webrtc.sdk.api.event.call.ScreenShareRemovedEvent
import com.infobip.webrtc.sdk.api.model.ErrorCode

open class DefaultRtcUiCallEventListener : RtcUiCallEventListener {
    override fun onRinging(callRingingEvent: CallRingingEvent?) {}

    override fun onEarlyMedia(callEarlyMediaEvent: CallEarlyMediaEvent?) {}

    override fun onEstablished(callEstablishedEvent: CallEstablishedEvent?) {}

    override fun onHangup(callHangupEvent: CallHangupEvent?) {}

    override fun onError(errorCode: ErrorCode?) {}

    override fun onCameraVideoAdded(cameraVideoAddedEvent: CameraVideoAddedEvent?) {}

    override fun onCameraVideoUpdated(cameraVideoUpdatedEvent: CameraVideoUpdatedEvent?) {}

    override fun onCameraVideoRemoved() {}

    override fun onScreenShareAdded(screenShareAddedEvent: ScreenShareAddedEvent?) {}

    override fun onScreenShareRemoved(screenShareRemovedEvent: ScreenShareRemovedEvent?) {}

    override fun onConferenceJoined(conferenceJoinedEvent: ConferenceJoinedEvent?) {}

    override fun onConferenceLeft(conferenceLeftEvent: ConferenceLeftEvent?) {}

    override fun onParticipantJoining(participantJoiningEvent: ParticipantJoiningEvent?) {}

    override fun onParticipantJoined(participantJoinedEvent: ParticipantJoinedEvent?) {}

    override fun onParticipantLeft(participantLeftEvent: ParticipantLeftEvent?) {}

    override fun onParticipantCameraVideoAdded(participantCameraVideoAddedEvent: ParticipantCameraVideoAddedEvent?) {}

    override fun onParticipantCameraVideoRemoved(participantCameraVideoRemovedEvent: ParticipantCameraVideoRemovedEvent?) {}

    override fun onParticipantScreenShareAdded(participantScreenShareAddedEvent: ParticipantScreenShareAddedEvent?) {}

    override fun onParticipantScreenShareRemoved(participantScreenShareRemovedEvent: ParticipantScreenShareRemovedEvent?) {}

    override fun onParticipantMuted(participantMutedEvent: ParticipantMutedEvent?) {}

    override fun onParticipantUnmuted(participantUnmutedEvent: ParticipantUnmutedEvent?) {}

    override fun onParticipantDeafen(participantDeafEvent: ParticipantDeafEvent?) {}

    override fun onParticipantUndeafen(participantUndeafEvent: ParticipantUndeafEvent?) {}

    override fun onParticipantStartedTalking(participantStartedTalkingEvent: ParticipantStartedTalkingEvent?) {}

    override fun onParticipantStoppedTalking(participantStoppedTalkingEvent: ParticipantStoppedTalkingEvent?) {}

    override fun onDialogJoined(dialogJoinedEvent: DialogJoinedEvent?) {}

    override fun onDialogLeft(dialogLeftEvent: DialogLeftEvent?) {}

    override fun onReconnected(reconnectedEvent: ReconnectedEvent?) {}

    override fun onReconnecting(reconnectingEvent: ReconnectingEvent?) {}
}