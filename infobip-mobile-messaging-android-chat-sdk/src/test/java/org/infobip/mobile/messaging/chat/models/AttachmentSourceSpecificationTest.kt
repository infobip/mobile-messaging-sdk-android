package org.infobip.mobile.messaging.chat.models

import org.assertj.core.api.Assertions
import org.junit.Test

class AttachmentSourceSpecificationTest {

    @Test
    fun `camera source specification throws exception for not allowed extension`() {
        Assertions.assertThatThrownBy { AttachmentSourceSpecification.Camera("svg") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Photo file extension must by jpg or jpeg.")
    }

    @Test
    fun `video recorder source specification throws exception for not allowed extension`() {
        Assertions.assertThatThrownBy { AttachmentSourceSpecification.VideoRecorder("avi") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Video file extension must by mp4 or 3gp.")
    }

}