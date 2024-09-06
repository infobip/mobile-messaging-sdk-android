package com.infobip.webrtc.ui.internal.listener

import com.infobip.webrtc.sdk.api.event.call.CallEarlyMediaEvent
import com.infobip.webrtc.sdk.api.event.call.CallEstablishedEvent
import com.infobip.webrtc.sdk.api.event.call.CallHangupEvent
import com.infobip.webrtc.sdk.api.event.call.CallRecordingStartedEvent
import com.infobip.webrtc.sdk.api.event.call.CallRecordingStoppedEvent
import com.infobip.webrtc.sdk.api.event.call.CallRingingEvent
import com.infobip.webrtc.sdk.api.event.call.CameraVideoAddedEvent
import com.infobip.webrtc.sdk.api.event.call.CameraVideoUpdatedEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceJoinedEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceLeftEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceRecordingStartedEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceRecordingStoppedEvent
import com.infobip.webrtc.sdk.api.event.call.DialogJoinedEvent
import com.infobip.webrtc.sdk.api.event.call.DialogLeftEvent
import com.infobip.webrtc.sdk.api.event.call.DialogRecordingStartedEvent
import com.infobip.webrtc.sdk.api.event.call.DialogRecordingStoppedEvent
import com.infobip.webrtc.sdk.api.event.call.ErrorEvent
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
import com.infobip.webrtc.sdk.api.event.listener.ApplicationCallEventListener
import com.infobip.webrtc.sdk.api.event.listener.WebrtcCallEventListener
import com.infobip.webrtc.sdk.api.model.ErrorCode

internal interface RtcUiCallEventListener {
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

    fun onReconnecting(reconnectingEvent: ReconnectingEvent?)
    fun onReconnected(reconnectedEvent: ReconnectedEvent?)

    fun onCallRecordingStarted(callRecordingStarted: CallRecordingStartedEvent?)
    fun onCallRecordingStopped(callRecordingStoppedEvent: CallRecordingStoppedEvent?)
    fun onDialogRecordingStarted(dialogRecordingStartedEvent: DialogRecordingStartedEvent?)
    fun onDialogRecordingStopped(dialogRecordingStoppedEvent: DialogRecordingStoppedEvent?)
    fun onConferenceRecordingStarted(conferenceRecordingStartedEvent: ConferenceRecordingStartedEvent?)
    fun onConferenceRecordingStopped(conferenceRecordingStoppedEvent: ConferenceRecordingStoppedEvent?)
}

internal fun RtcUiCallEventListener.toWebRtcCallEventListener(): WebrtcCallEventListener {
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

        override fun onCallRecordingStarted(callRecordingStartedEvent: CallRecordingStartedEvent?) {
            this@toWebRtcCallEventListener.onCallRecordingStarted(callRecordingStartedEvent)
        }

        override fun onRinging(callRingingEvent: CallRingingEvent?) {
            this@toWebRtcCallEventListener.onRinging(callRingingEvent)
        }
    }
}

internal fun RtcUiCallEventListener.toAppCallEventListener(): ApplicationCallEventListener {
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

        override fun onReconnected(reconnectedEvent: ReconnectedEvent?) {
            this@toAppCallEventListener.onReconnected(reconnectedEvent)
        }

        override fun onCallRecordingStarted(callRecordingStarted: CallRecordingStartedEvent?) {
            this@toAppCallEventListener.onCallRecordingStarted(callRecordingStarted)
        }

        override fun onCallRecordingStopped(callRecordingStoppedEvent: CallRecordingStoppedEvent?) {
            this@toAppCallEventListener.onCallRecordingStopped(callRecordingStoppedEvent)
        }

        override fun onDialogRecordingStarted(dialogRecordingStartedEvent: DialogRecordingStartedEvent?) {
            this@toAppCallEventListener.onDialogRecordingStarted(dialogRecordingStartedEvent)
        }

        override fun onDialogRecordingStopped(dialogRecordingStoppedEvent: DialogRecordingStoppedEvent?) {
            this@toAppCallEventListener.onDialogRecordingStopped(dialogRecordingStoppedEvent)
        }

        override fun onConferenceRecordingStarted(conferenceRecordingStartedEvent: ConferenceRecordingStartedEvent?) {
            this@toAppCallEventListener.onConferenceRecordingStarted(conferenceRecordingStartedEvent)
        }

        override fun onConferenceRecordingStopped(conferenceRecordingStoppedEvent: ConferenceRecordingStoppedEvent?) {
            this@toAppCallEventListener.onConferenceRecordingStopped(conferenceRecordingStoppedEvent)
        }

        override fun onReconnecting(reconnectingEvent: ReconnectingEvent?) {
            this@toAppCallEventListener.onReconnecting(reconnectingEvent)
        }
    }
}