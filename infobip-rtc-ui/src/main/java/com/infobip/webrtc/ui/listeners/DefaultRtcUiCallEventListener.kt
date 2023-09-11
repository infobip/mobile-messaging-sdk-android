package com.infobip.webrtc.ui.listeners

import com.infobip.webrtc.sdk.api.event.call.*
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
}