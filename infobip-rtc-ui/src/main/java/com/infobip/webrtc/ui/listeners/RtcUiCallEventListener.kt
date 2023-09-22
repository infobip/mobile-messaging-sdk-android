package com.infobip.webrtc.ui.listeners

import com.infobip.webrtc.sdk.api.event.call.*
import com.infobip.webrtc.sdk.api.event.listener.ApplicationCallEventListener
import com.infobip.webrtc.sdk.api.event.listener.WebrtcCallEventListener
import com.infobip.webrtc.sdk.api.model.ErrorCode

interface RtcUiCallEventListener {
    fun onRinging(callRingingEvent: CallRingingEvent?)
    fun onEarlyMedia(callEarlyMediaEvent: CallEarlyMediaEvent?)
    fun onEstablished(callEstablishedEvent: CallEstablishedEvent?)
    fun onHangup(callHangupEvent: CallHangupEvent?)
    fun onError(errorCode: ErrorCode?)

    fun onCameraVideoAdded(cameraVideoAddedEvent: CameraVideoAddedEvent?)
    fun onCameraVideoUpdated(cameraVideoUpdatedEvent: CameraVideoUpdatedEvent?)
    fun onCameraVideoRemoved()
    fun onScreenShareAdded(screenShareAddedEvent: ScreenShareAddedEvent?)
    fun onScreenShareRemoved(screenShareRemovedEvent: ScreenShareRemovedEvent?)

    fun onConferenceJoined(conferenceJoinedEvent: ConferenceJoinedEvent?)
    fun onConferenceLeft(conferenceLeftEvent: ConferenceLeftEvent?)

    fun onParticipantJoining(participantJoiningEvent: ParticipantJoiningEvent?)
    fun onParticipantJoined(participantJoinedEvent: ParticipantJoinedEvent?)
    fun onParticipantLeft(participantLeftEvent: ParticipantLeftEvent?)

    fun onParticipantCameraVideoAdded(participantCameraVideoAddedEvent: ParticipantCameraVideoAddedEvent?)
    fun onParticipantCameraVideoRemoved(participantCameraVideoRemovedEvent: ParticipantCameraVideoRemovedEvent?)
    fun onParticipantScreenShareAdded(participantScreenShareAddedEvent: ParticipantScreenShareAddedEvent?)
    fun onParticipantScreenShareRemoved(participantScreenShareRemovedEvent: ParticipantScreenShareRemovedEvent?)

    fun onParticipantMuted(participantMutedEvent: ParticipantMutedEvent?)
    fun onParticipantUnmuted(participantUnmutedEvent: ParticipantUnmutedEvent?)
    fun onParticipantDeafen(participantDeafEvent: ParticipantDeafEvent?)
    fun onParticipantUndeafen(participantUndeafEvent: ParticipantUndeafEvent?)
    fun onParticipantStartedTalking(participantStartedTalkingEvent: ParticipantStartedTalkingEvent?)
    fun onParticipantStoppedTalking(participantStoppedTalkingEvent: ParticipantStoppedTalkingEvent?)

    fun onDialogJoined(dialogJoinedEvent: DialogJoinedEvent?)
    fun onDialogLeft(dialogLeftEvent: DialogLeftEvent?)
}

fun RtcUiCallEventListener.toWebRtcCallEventListener(): WebrtcCallEventListener {
    return object : WebrtcCallEventListener {
        override fun onEarlyMedia(callEarlyMediaEvent: CallEarlyMediaEvent?) {
            this@toWebRtcCallEventListener.onEarlyMedia(callEarlyMediaEvent)
        }

        override fun onEstablished(callEstablishedEvent: CallEstablishedEvent?) {
            this@toWebRtcCallEventListener.onEstablished(callEstablishedEvent)
        }

        override fun onHangup(callHangupEvent: CallHangupEvent?) {
            this@toWebRtcCallEventListener.onHangup(callHangupEvent)
        }

        override fun onError(callErrorEvent: ErrorEvent?) {
            this@toWebRtcCallEventListener.onError(callErrorEvent?.errorCode)
        }

        override fun onCameraVideoAdded(cameraVideoAddedEvent: CameraVideoAddedEvent?) {
            this@toWebRtcCallEventListener.onCameraVideoAdded(cameraVideoAddedEvent)
        }

        override fun onCameraVideoUpdated(cameraVideoUpdatedEvent: CameraVideoUpdatedEvent?) {
            this@toWebRtcCallEventListener.onCameraVideoUpdated(cameraVideoUpdatedEvent)
        }

        override fun onCameraVideoRemoved() {
            this@toWebRtcCallEventListener.onCameraVideoRemoved()
        }

        override fun onScreenShareAdded(screenShareAddedEvent: ScreenShareAddedEvent?) {
            this@toWebRtcCallEventListener.onScreenShareAdded(screenShareAddedEvent)
        }

        override fun onScreenShareRemoved(screenShareRemovedEvent: ScreenShareRemovedEvent?) {
            this@toWebRtcCallEventListener.onScreenShareRemoved(screenShareRemovedEvent)
        }

        override fun onRemoteCameraVideoAdded(cameraVideoAddedEvent: CameraVideoAddedEvent?) {
            this@toWebRtcCallEventListener.onParticipantCameraVideoAdded(ParticipantCameraVideoAddedEvent(null, cameraVideoAddedEvent?.track))
        }

        override fun onRemoteCameraVideoRemoved() {
            this@toWebRtcCallEventListener.onParticipantCameraVideoRemoved(ParticipantCameraVideoRemovedEvent(null))
        }

        override fun onRemoteScreenShareAdded(screenShareAddedEvent: ScreenShareAddedEvent?) {
            this@toWebRtcCallEventListener.onParticipantScreenShareAdded(ParticipantScreenShareAddedEvent(null, screenShareAddedEvent?.track))
        }

        override fun onRemoteScreenShareRemoved() {
            this@toWebRtcCallEventListener.onParticipantScreenShareRemoved(ParticipantScreenShareRemovedEvent(null))
        }

        override fun onRemoteMuted() {
            this@toWebRtcCallEventListener.onParticipantMuted(ParticipantMutedEvent(null))
        }

        override fun onRemoteUnmuted() {
            this@toWebRtcCallEventListener.onParticipantUnmuted(ParticipantUnmutedEvent(null))
        }

        override fun onRinging(callRingingEvent: CallRingingEvent?) {
            this@toWebRtcCallEventListener.onRinging(callRingingEvent)
        }
    }
}

fun RtcUiCallEventListener.toAppCallEventListener(): ApplicationCallEventListener {
    return object : ApplicationCallEventListener {
        override fun onEarlyMedia(callEarlyMediaEvent: CallEarlyMediaEvent?) {
            this@toAppCallEventListener.onEarlyMedia(callEarlyMediaEvent)
        }

        override fun onEstablished(callEstablishedEvent: CallEstablishedEvent?) {
            this@toAppCallEventListener.onEstablished(callEstablishedEvent)
        }

        override fun onHangup(callHangupEvent: CallHangupEvent?) {
            this@toAppCallEventListener.onHangup(callHangupEvent)
        }

        override fun onError(errorEvent: ErrorEvent?) {
            this@toAppCallEventListener.onError(errorEvent?.errorCode)
        }

        override fun onRinging(callRingingEvent: CallRingingEvent?) {
            this@toAppCallEventListener.onRinging(callRingingEvent)
        }

        override fun onCameraVideoAdded(cameraVideoAddedEvent: CameraVideoAddedEvent?) {
            this@toAppCallEventListener.onCameraVideoAdded(cameraVideoAddedEvent)
        }

        override fun onCameraVideoUpdated(cameraVideoUpdatedEvent: CameraVideoUpdatedEvent?) {
            this@toAppCallEventListener.onCameraVideoUpdated(cameraVideoUpdatedEvent)
        }

        override fun onCameraVideoRemoved() {
            this@toAppCallEventListener.onCameraVideoRemoved()
        }

        override fun onScreenShareAdded(screenShareAddedEvent: ScreenShareAddedEvent?) {
            this@toAppCallEventListener.onScreenShareAdded(screenShareAddedEvent)
        }

        override fun onScreenShareRemoved(screenShareRemovedEvent: ScreenShareRemovedEvent?) {
            this@toAppCallEventListener.onScreenShareRemoved(screenShareRemovedEvent)
        }

        override fun onConferenceJoined(conferenceJoinedEvent: ConferenceJoinedEvent?) {
            this@toAppCallEventListener.onConferenceJoined(conferenceJoinedEvent)
        }

        override fun onConferenceLeft(conferenceLeftEvent: ConferenceLeftEvent?) {
            this@toAppCallEventListener.onConferenceLeft(conferenceLeftEvent)
        }

        override fun onParticipantJoining(participantJoiningEvent: ParticipantJoiningEvent?) {
            this@toAppCallEventListener.onParticipantJoining(participantJoiningEvent)
        }

        override fun onParticipantJoined(participantJoinedEvent: ParticipantJoinedEvent?) {
            this@toAppCallEventListener.onParticipantJoined(participantJoinedEvent)
        }

        override fun onParticipantLeft(participantLeftEvent: ParticipantLeftEvent?) {
            this@toAppCallEventListener.onParticipantLeft(participantLeftEvent)
        }

        override fun onParticipantCameraVideoAdded(participantCameraVideoAddedEvent: ParticipantCameraVideoAddedEvent?) {
            this@toAppCallEventListener.onParticipantCameraVideoAdded(participantCameraVideoAddedEvent)
        }

        override fun onParticipantCameraVideoRemoved(participantCameraVideoRemovedEvent: ParticipantCameraVideoRemovedEvent?) {
            this@toAppCallEventListener.onParticipantCameraVideoRemoved(participantCameraVideoRemovedEvent)
        }

        override fun onParticipantScreenShareAdded(participantScreenShareAddedEvent: ParticipantScreenShareAddedEvent?) {
            this@toAppCallEventListener.onParticipantScreenShareAdded(participantScreenShareAddedEvent)
        }

        override fun onParticipantScreenShareRemoved(participantScreenShareRemovedEvent: ParticipantScreenShareRemovedEvent?) {
            this@toAppCallEventListener.onParticipantScreenShareRemoved(participantScreenShareRemovedEvent)
        }

        override fun onParticipantMuted(participantMutedEvent: ParticipantMutedEvent?) {
            this@toAppCallEventListener.onParticipantMuted(participantMutedEvent)
        }

        override fun onParticipantUnmuted(participantUnmutedEvent: ParticipantUnmutedEvent?) {
            this@toAppCallEventListener.onParticipantUnmuted(participantUnmutedEvent)
        }

        override fun onParticipantDeaf(participantDeafEvent: ParticipantDeafEvent?) {
            this@toAppCallEventListener.onParticipantDeafen(participantDeafEvent)
        }

        override fun onParticipantUndeaf(participantUndeafEvent: ParticipantUndeafEvent?) {
            this@toAppCallEventListener.onParticipantUndeafen(participantUndeafEvent)
        }

        override fun onParticipantStartedTalking(participantStartedTalkingEvent: ParticipantStartedTalkingEvent?) {
            this@toAppCallEventListener.onParticipantStartedTalking(participantStartedTalkingEvent)
        }

        override fun onParticipantStoppedTalking(participantStoppedTalkingEvent: ParticipantStoppedTalkingEvent?) {
            this@toAppCallEventListener.onParticipantStoppedTalking(participantStoppedTalkingEvent)
        }

        override fun onDialogJoined(dialogJoinedEvent: DialogJoinedEvent?) {
            this@toAppCallEventListener.onDialogJoined(dialogJoinedEvent)
        }

        override fun onDialogLeft(dialogLeftEvent: DialogLeftEvent?) {
            this@toAppCallEventListener.onDialogLeft(dialogLeftEvent)
        }
    }
}