/*
 * AttachmentSourceSpecificationTest.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.models

import org.assertj.core.api.Assertions
import org.infobip.mobile.messaging.chat.core.InAppChatException
import org.junit.Test

class AttachmentSourceSpecificationTest {

    @Test
    fun `camera source specification throws exception for not allowed extension`() {
        Assertions.assertThatThrownBy { AttachmentSourceSpecification.Camera("svg") }
            .isInstanceOf(InAppChatException.InvalidPhotoAttachmentExtension::class.java)
            .hasMessageContaining("Photo attachment has invalid file extension.")
    }

    @Test
    fun `video recorder source specification throws exception for not allowed extension`() {
        Assertions.assertThatThrownBy { AttachmentSourceSpecification.VideoRecorder("avi") }
            .isInstanceOf(InAppChatException.InvalidVideoAttachmentExtension::class.java)
            .hasMessageContaining("Video attachment has invalid file extension.")
    }

}